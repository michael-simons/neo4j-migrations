/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ac.simons.neo4j.migrations.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.CatalogBasedMigration.ApplyOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.Counters;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.CreateOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.DefaultDropOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.ItemSpecificOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.Operation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.OperationContext;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.VerificationFailedException;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.VerifyOperation;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.catalog.Operator;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.w3c.dom.Document;

/**
 * @author Michael J. Simons
 */
class CatalogBasedMigrationTest {

	@ParameterizedTest
	@ValueSource(strings = { "01.xml", "02.xml", "03.xml" })
	void checksumShouldBeCorrect(String in) {
		URL url = CatalogBasedMigration.class.getResource("/xml/identical-migrations/V01__" + in);
		Objects.requireNonNull(url);
		CatalogBasedMigration schemaBasedMigration = (CatalogBasedMigration) CatalogBasedMigration.from(url);
		assertThat(schemaBasedMigration.getChecksum()).hasValue("1098321819");
		assertThat(schemaBasedMigration.getCatalog().getItems()).hasSize(2);
	}

	abstract static class MockHolder {

		final Constraint uniqueBookIdV1 = Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("id");

		final Constraint uniqueBookIdV2 = Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("isbn");

		final VersionedCatalog catalog = new DefaultCatalog();

		ArgumentCaptor<String> argumentCaptor;

		QueryRunner runner;

		Result defaultResult;

		MockHolder() {
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("1"),
				() -> Collections.singletonList(uniqueBookIdV1));
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("2"),
				() -> Collections.singletonList(uniqueBookIdV2));
		}

		@BeforeEach
		void initMocks() {

			defaultResult = mock(Result.class);
			runner = mock(QueryRunner.class);
			when(runner.run(Mockito.anyString())).thenReturn(defaultResult);
			argumentCaptor = ArgumentCaptor.forClass(String.class);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Operations {

		@Test
		void emptyDocumentShouldBeReadable() {
			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/no-operations.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			assertThat(CatalogBasedMigration.parseOperations(document, MigrationVersion.withValue("1"))).isEmpty();
		}

		@Test
		void applyShouldWork() {

			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/apply.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			List<Operation> operations = CatalogBasedMigration.parseOperations(document,
				MigrationVersion.withValue("1"));
			assertThat(operations).hasSize(1)
				.satisfiesExactly(op -> assertThat(op).isInstanceOf(ApplyOperation.class));
		}

		@Test
		void defaultVerifyShouldWork() {

			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/verify-default.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			List<Operation> operations = CatalogBasedMigration.parseOperations(document,
				MigrationVersion.withValue("1"));
			assertThat(operations).hasSize(1)
				.satisfiesExactly(op -> {
					assertThat(op).isInstanceOf(VerifyOperation.class);
					VerifyOperation verifyOperation = (VerifyOperation) op;
					assertThat(verifyOperation.useCurrent()).isFalse();
					assertThat(verifyOperation.allowEquivalent()).isTrue();
					assertThat(verifyOperation.getDefinedAt()).isEqualTo(MigrationVersion.withValue("1"));
				});
		}

		@Test
		void verifyModifiedShouldWork() {

			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/verify-modified.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			List<Operation> operations = CatalogBasedMigration.parseOperations(document,
				MigrationVersion.withValue("1"));
			assertThat(operations).hasSize(1)
				.satisfiesExactly(op -> {
					assertThat(op).isInstanceOf(VerifyOperation.class);
					VerifyOperation verifyOperation = (VerifyOperation) op;
					assertThat(verifyOperation.useCurrent()).isTrue();
					assertThat(verifyOperation.allowEquivalent()).isFalse();
					assertThat(verifyOperation.getDefinedAt()).isEqualTo(MigrationVersion.withValue("1"));
				});
		}

		@SuppressWarnings("unused")
		Stream<Arguments> createsAndDrops() {
			return Stream.of(
				Arguments.of("creates.xml", CreateOperation.class),
				Arguments.of("drops.xml", CatalogBasedMigration.DropOperation.class)
			);
		}

		@ParameterizedTest
		@MethodSource
		void createsAndDrops(String file, Class<?> expectedType) {

			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/" + file);
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			List<Operation> operations = CatalogBasedMigration.parseOperations(document,
				MigrationVersion.withValue("1"));
			assertThat(operations).hasSize(4)
				.satisfies(op -> {
					assertThat(op).isInstanceOf(expectedType);
					ItemSpecificOperation operation = (ItemSpecificOperation) op;
					assertThat(operation.getReference()).hasValue(Name.of("unique_isbn"));
					assertThat(operation.getLocalItem()).isEmpty();
				}, Index.atIndex(0))
				.satisfies(op -> {
					assertThat(op).isInstanceOf(expectedType);
					ItemSpecificOperation operation = (ItemSpecificOperation) op;
					assertThat(operation.getReference()).hasValue(Name.of("definedSomewhereElse"));
					assertThat(operation.getLocalItem()).isEmpty();
				}, Index.atIndex(1))
				.satisfies(op -> {
					assertThat(op).isInstanceOf(expectedType);
					ItemSpecificOperation operation = (ItemSpecificOperation) op;
					assertThat(operation.getReference()).hasValue(Name.of("somewhereElseButNoEmptyElement"));
					assertThat(operation.getLocalItem()).isEmpty();
				}, Index.atIndex(2))
				.satisfies(op -> {
					assertThat(op).isInstanceOf(expectedType);
					ItemSpecificOperation operation = (ItemSpecificOperation) op;
					assertThat(operation.getReference()).isEmpty();
					assertThat(operation.getLocalItem()).hasValueSatisfying(item -> {
						assertThat(item).isInstanceOf(Constraint.class);
						assertThat(item.getName()).isEqualTo(Name.of("someConstraint"));
					});
				}, Index.atIndex(3));
		}

		@ParameterizedTest
		@ValueSource(strings = { "create", "drop" })
		void itemShouldRequireRefOrItem(String file) {

			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/" + file + "-both-ref-and-item.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			MigrationVersion version = MigrationVersion.withValue("1");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> CatalogBasedMigration.parseOperations(document, version))
				.withMessage(
					"Cannot create an operation referring to an item with both ref and item attributes. Please pick one.");
		}

		@ParameterizedTest
		@ValueSource(strings = { "create", "drop" })
		void itemShouldRequireRefNameOrLocal(String file) {

			URL url = CatalogBasedMigration.class.getResource("/xml/parsing/" + file + "-both-item-and-local.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			MigrationVersion version = MigrationVersion.withValue("1");
			assertThatIllegalArgumentException()
				.isThrownBy(() -> CatalogBasedMigration.parseOperations(document, version))
				.withMessage(
					"Cannot create an operation referring to an element and defining an item locally at the same time.");
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Verifies extends MockHolder {

		@ParameterizedTest
		@ValueSource(booleans = { true, false })
		void shouldDealWithEmptyCatalogs(boolean useCurrent) {

			Operation operation = Operation.verify(useCurrent).allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE,
				new DefaultCatalog(), runner);

			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldUsePriorVersion() {

			Operation operation = Operation
				.verify(false) // There is nothing prior to version 1, so this will be empty.
				.allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				runner);

			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldThrowWhenNotIdentical() {

			Operation operation = Operation
				.verify(true)
				.allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				runner);

			assertThatExceptionOfType(VerificationFailedException.class)
				.isThrownBy(() -> operation.execute(context))
				.withMessage("Catalogs are neither identical nor equivalent.");

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldFailWhenEquivalencyIsNotAllowed() {

			Map<String, Value> constraints = new HashMap<>();
			constraints.put("name", Values.value("foo"));
			constraints.put("description", Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE"));
			when(defaultResult.stream()).thenReturn(Stream.of(new MapAccessorAndRecordImpl(constraints)));

			Operation operation = Operation
				.verify(true)
				.allowEquivalent(false)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				runner);

			assertThatExceptionOfType(VerificationFailedException.class)
				.isThrownBy(() -> operation.execute(context))
				.withMessage(
					"Database schema and the catalog are equivalent but the verification requires them to be identical.");

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldNotFailWhenEquivalencyIsAllowed() {

			Map<String, Value> constraints = new HashMap<>();
			constraints.put("name", Values.value("foo"));
			constraints.put("description", Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE"));
			when(defaultResult.stream()).thenReturn(Stream.of(new MapAccessorAndRecordImpl(constraints)));

			Operation operation = Operation
				.verify(true)
				.allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				runner);

			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Applies extends MockHolder {

		@Test
		void shouldDealWithEmptyCatalogs() {
			Operation operation = Operation.apply(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE,
				new DefaultCatalog(), runner);
			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldDealWithEmptyDatabaseCatalog() {
			Operation operation = Operation.apply(MigrationVersion.withValue("2"));

			Result createResult = mock(Result.class);
			ResultSummary summary = mock(ResultSummary.class);
			SummaryCounters counters = mock(SummaryCounters.class);
			when(counters.constraintsAdded()).thenReturn(22);
			when(counters.indexesAdded()).thenReturn(20);
			when(summary.counters()).thenReturn(counters);
			when(createResult.consume()).thenReturn(summary);

			String createQuery = "CREATE CONSTRAINT book_id_unique FOR (n:Book) REQUIRE n.isbn IS UNIQUE";
			when(runner.run(createQuery)).thenReturn(createResult);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			operation.execute(context);

			verify(runner, times(2)).run(argumentCaptor.capture());
			assertThat(argumentCaptor.getAllValues())
				.containsExactly(Neo4jVersion.V4_4.getShowConstraints(), createQuery);
			verify(defaultResult).stream();
			verify(createResult).consume();
			verify(summary).counters();
			verify(counters).constraintsAdded();
			verify(counters).indexesAdded();
			verifyNoMoreInteractions(runner, defaultResult, summary, counters);
		}

		@Test
		void shouldDealWithEmptyCatalog() {
			Operation operation = Operation.apply(MigrationVersion.withValue("1"));

			Result dropResult = mock(Result.class);
			ResultSummary summary = mock(ResultSummary.class);
			SummaryCounters counters = mock(SummaryCounters.class);
			when(counters.constraintsRemoved()).thenReturn(22);
			when(counters.indexesRemoved()).thenReturn(20);
			when(summary.counters()).thenReturn(counters);
			when(dropResult.consume()).thenReturn(summary);

			Map<String, Value> constraints = new HashMap<>();
			constraints.put("name", Values.value(uniqueBookIdV1.getName().getValue()));
			constraints.put("description", Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE"));
			when(defaultResult.stream()).thenReturn(Stream.of(new MapAccessorAndRecordImpl(constraints)));

			String dropQuery = "DROP CONSTRAINT book_id_unique";
			when(runner.run(dropQuery)).thenReturn(dropResult);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE,
				new DefaultCatalog(), runner);
			operation.execute(context);

			verify(runner, times(2)).run(argumentCaptor.capture());
			assertThat(argumentCaptor.getAllValues())
				.containsExactly(Neo4jVersion.V4_4.getShowConstraints(), dropQuery);
			verify(defaultResult).stream();
			verify(dropResult).consume();
			verify(summary).counters();
			verify(counters).constraintsRemoved();
			verify(counters).indexesRemoved();
			verifyNoMoreInteractions(runner, defaultResult, summary, counters);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class CreateAndDrop extends MockHolder {

		@SuppressWarnings("unused")
		Stream<Arguments> simpleOpsShouldWork() {

			return Stream.of(
				Arguments.of(Operator.CREATE, Neo4jVersion.V3_5,
					"CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"),
				Arguments.of(Operator.CREATE, Neo4jVersion.V4_4,
					"CREATE CONSTRAINT book_id_unique FOR (n:Book) REQUIRE n.id IS UNIQUE"),
				Arguments.of(Operator.DROP, Neo4jVersion.V3_5,
					"DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"),
				Arguments.of(Operator.DROP, Neo4jVersion.V4_4, "DROP CONSTRAINT book_id_unique")
			);
		}

		@ParameterizedTest
		@MethodSource
		void simpleOpsShouldWork(Operator operator, Neo4jVersion version, String expectedQuery) {

			Operation operation;
			if (operator == Operator.CREATE) {
				operation = Operation
					.create(Name.of("book_id_unique"), false)
					.with(MigrationVersion.withValue("1"));
			} else if (operator == Operator.DROP) {
				operation = Operation
					.drop(Name.of("book_id_unique"), false)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			Result opResult = mock(Result.class);
			ResultSummary resultSummary = mock(ResultSummary.class);
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.constraintsAdded()).thenReturn(1);
			when(summaryCounters.constraintsRemoved()).thenReturn(2);
			when(resultSummary.counters()).thenReturn(summaryCounters);
			when(opResult.consume()).thenReturn(resultSummary);
			when(runner.run(expectedQuery)).thenReturn(opResult);

			OperationContext context = new OperationContext(version, Neo4jEdition.ENTERPRISE, catalog, runner);
			Counters counters = operation.execute(context);
			if (operator == Operator.CREATE) {
				assertThat(counters.constraintsAdded()).isEqualTo(1);
			} else {
				assertThat(counters.constraintsRemoved()).isEqualTo(2);
			}

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(opResult).consume();
			verifyNoMoreInteractions(runner, defaultResult, opResult);
		}

		@SuppressWarnings("unused")
		Stream<Arguments> idempotencyOnSupportedTargetsShouldWork() {

			return Stream.of(
				Arguments.of(Operator.CREATE,
					"CREATE CONSTRAINT book_id_unique IF NOT EXISTS FOR (n:Book) REQUIRE n.id IS UNIQUE"),
				Arguments.of(Operator.DROP, "DROP CONSTRAINT book_id_unique IF EXISTS")
			);
		}

		@ParameterizedTest
		@MethodSource
		void idempotencyOnSupportedTargetsShouldWork(Operator operator, String expectedQuery) {

			final Operation operation;
			if (operator == Operator.CREATE) {
				operation = Operation
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else if (operator == Operator.DROP) {
				operation = Operation
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			Result opResult = mock(Result.class);
			ResultSummary resultSummary = mock(ResultSummary.class);
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.constraintsAdded()).thenReturn(1);
			when(summaryCounters.constraintsRemoved()).thenReturn(2);
			when(resultSummary.counters()).thenReturn(summaryCounters);
			when(opResult.consume()).thenReturn(resultSummary);
			when(runner.run(expectedQuery)).thenReturn(opResult);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			Counters counters = operation.execute(context);
			if (operator == Operator.CREATE) {
				assertThat(counters.constraintsAdded()).isEqualTo(1);
			} else {
				assertThat(counters.constraintsRemoved()).isEqualTo(2);
			}

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(opResult).consume();
			verifyNoMoreInteractions(runner, defaultResult, opResult);
		}

		@ParameterizedTest
		@EnumSource(value = Operator.class)
		void shouldCheckForExistence(Operator operatorUnderTest) {

			final Operation op;
			if (operatorUnderTest == Operator.CREATE) {
				op = Operation
					.create(Name.of("trololo"), false)
					.with(MigrationVersion.withValue("1"));
			} else if (operatorUnderTest == Operator.DROP) {
				op = Operation
					.drop(Name.of("trololo"), false)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operatorUnderTest);
			}

			OperationContext context = new OperationContext(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> op.execute(context))
				.withMessage("An item named 'trololo' has not been defined as of version 1.");
		}

		@SuppressWarnings("unused")
		Stream<Arguments> idempotencyOnUnsupportedTargetsShouldWorkWhenNoExceptionIsThrown() {

			return Stream.of(
				Arguments.of(Operator.CREATE, "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"),
				Arguments.of(Operator.DROP, "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE")
			);
		}

		@ParameterizedTest
		@MethodSource
		void idempotencyOnUnsupportedTargetsShouldWorkWhenNoExceptionIsThrown(Operator operator, String expectedQuery) {

			final Operation op;
			if (operator == Operator.CREATE) {
				op = Operation
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else if (operator == Operator.DROP) {
				op = Operation
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			Result opResult = mock(Result.class);
			ResultSummary resultSummary = mock(ResultSummary.class);
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.constraintsAdded()).thenReturn(1);
			when(summaryCounters.constraintsRemoved()).thenReturn(2);
			when(resultSummary.counters()).thenReturn(summaryCounters);
			when(opResult.consume()).thenReturn(resultSummary);
			when(runner.run(expectedQuery)).thenReturn(opResult);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			Counters counters = op.execute(context);
			if (operator == Operator.CREATE) {
				assertThat(counters.constraintsAdded()).isEqualTo(1);
			} else {
				assertThat(counters.constraintsRemoved()).isEqualTo(2);
			}

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(opResult).consume();
			verifyNoMoreInteractions(runner, defaultResult, opResult);
		}

		@SuppressWarnings("unused")
		Stream<Arguments> idempotencyOnUnsupportedTargetsShouldRethrowExceptions() {
			return Stream.of(
				Arguments.of(Operator.CREATE, "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"),
				Arguments.of(Operator.DROP, "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE")
			);
		}

		@ParameterizedTest
		@MethodSource
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptions(Operator operator, String expectedQuery) {

			final Operation op;
			if (operator == Operator.CREATE) {
				op = Operation
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else if (operator == Operator.DROP) {
				op = Operation
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			when(runner.run(expectedQuery)).thenThrow(new DatabaseException("Something else is broken", "Oh :("));
			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> op.execute(context))
				.withMessage("Oh :(");

			verify(runner).run(expectedQuery);
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@SuppressWarnings("unused")
		Stream<Arguments> idempotencyOnUnsupportedTargetsShouldIgnoreSomeErrorCodes() {
			return Stream.of(
				Arguments.of(Operator.CREATE, "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE",
					Neo4jCodes.EQUIVALENT_SCHEMA_RULE_ALREADY_EXISTS),
				Arguments.of(Operator.CREATE, "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE",
					Neo4jCodes.CONSTRAINT_ALREADY_EXISTS),
				Arguments.of(Operator.DROP, "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE",
					Neo4jCodes.CONSTRAINT_DROP_FAILED)
			);
		}

		@ParameterizedTest
		@MethodSource
		void idempotencyOnUnsupportedTargetsShouldIgnoreSomeErrorCodes(Operator operator, String expectedQuery,
			String codeToThrow) {

			final Operation op;
			Result result = mock(Result.class);
			if (operator == Operator.CREATE) {
				op = Operation
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
				when(result.list(Mockito.<Function<Record, Constraint>>any()))
					.thenAnswer(i -> Stream.of(new MapAccessorAndRecordImpl(Collections.singletonMap("description",
							Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE")))).map(i.getArgument(0))
						.collect(Collectors.toList()));
			} else if (operator == Operator.DROP) {
				op = Operation
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
				when(result.list(Mockito.<Function<Record, Constraint>>any())).thenReturn(Collections.emptyList());
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			when(runner.run(expectedQuery))
				.thenThrow(new DatabaseException(codeToThrow, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall))
				.thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			op.execute(context);

			verify(runner).run(expectedQuery);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}
	}

	@Nested
	class Creates extends MockHolder {

		@Test
		void idempotencyOnUnsupportedTargetsShouldByHappyWithOtherConstraints() {

			Operation op = Operation
				.create(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);

			when(result.list(Mockito.<Function<Record, Constraint>>any()))
				.thenAnswer(i -> Stream.of(
						new MapAccessorAndRecordImpl(Collections.singletonMap("description",
							Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.isbn) IS UNIQUE"))),
						new MapAccessorAndRecordImpl(Collections.singletonMap("description",
							Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE")))
					).map(i.getArgument(0))
					.collect(Collectors.toList()));

			String expectedDrop = "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_ALREADY_EXISTS, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall))
				.thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			op.execute(context);

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptionsWhenNotExistsInTheEnd() {

			Operation op = Operation
				.create(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any())).thenReturn(Collections.emptyList());

			String expectedDrop = "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_ALREADY_EXISTS, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> op.execute(context))
				.withMessage("Oh :(");

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}
	}

	@Nested
	class Drops extends MockHolder {

		@Test
		void idempotencyOnUnsupportedTargetsShouldByHappyWithOtherConstraints() {

			Operation drop = Operation
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);

			when(result.list(Mockito.<Function<Record, Constraint>>any()))
				.thenAnswer(i -> Stream.of(new MapAccessorAndRecordImpl(Collections.singletonMap("description",
						Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.isbn) IS UNIQUE")))).map(i.getArgument(0))
					.collect(Collectors.toList()));

			String expectedDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall))
				.thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			drop.execute(context);

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptionsWhenStillExists() {

			Operation drop = Operation
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any()))
				.thenAnswer(i -> Stream.of(new MapAccessorAndRecordImpl(Collections.singletonMap("description",
						Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE")))).map(i.getArgument(0))
					.collect(Collectors.toList()));

			String expectedDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> drop.execute(context))
				.withMessage("Oh :(");

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldTryDroppingOlderVersions() {

			Operation drop = Operation
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("2"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any()))
				.thenAnswer(i -> Stream.of(new MapAccessorAndRecordImpl(Collections.singletonMap("description",
						Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE")))).map(i.getArgument(0))
					.collect(Collectors.toList()));

			String firstDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE";
			when(runner.run(firstDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall)).thenReturn(result);

			Result dropResult = mock(Result.class);
			ResultSummary resultSummary = mock(ResultSummary.class);
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.constraintsRemoved()).thenReturn(1);
			when(resultSummary.counters()).thenReturn(summaryCounters);
			when(dropResult.consume()).thenReturn(resultSummary);
			when(runner.run("DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE")).thenReturn(dropResult);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			Counters counters = drop.execute(context);
			assertThat(counters.constraintsRemoved()).isEqualTo(1);

			verify(runner, times(3)).run(argumentCaptor.capture());
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries)
				.containsExactly(
					firstDrop,
					"CALL db.constraints()",
					"DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"
				);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verify(dropResult).consume();
			verifyNoMoreInteractions(runner, result, defaultResult, dropResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldNotTryDroppingOlderVersionsWithLocalItem() {

			Operation drop = Operation
				.drop(Constraint
					.forNode("Book")
					.named("book_id_unique")
					.unique("isbn"), true);

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any()))
				.thenAnswer(i -> Stream.of(new MapAccessorAndRecordImpl(Collections.singletonMap("description",
						Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE")))).map(i.getArgument(0))
					.collect(Collectors.toList()));

			String firstDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE";
			when(runner.run(firstDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = Neo4jVersion.V3_5.getShowConstraints();
			when(runner.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			drop.execute(context);

			verify(runner, times(2)).run(argumentCaptor.capture());
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(firstDrop, expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void shouldNotEndlessLoopWhenTryingOlderVersions() {

			Operation drop = Operation
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("2"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any()))
				.thenAnswer(i -> Stream.of(
							new MapAccessorAndRecordImpl(Collections.singletonMap("description",
								Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE"))),
							new MapAccessorAndRecordImpl(Collections.singletonMap("description",
								Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.foobar) IS UNIQUE")))
						)
						.map(i.getArgument(0)).collect(Collectors.toList())
				)
				.thenAnswer(i -> Stream.of(
						new MapAccessorAndRecordImpl(Collections.singletonMap("description",
							Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.foobar) IS UNIQUE")))
					).map(i.getArgument(0)).collect(Collectors.toList())
				);

			String firstDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE";
			String secondDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(firstDrop)).thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			when(runner.run(secondDrop)).thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));

			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				runner);
			drop.execute(context);

			verify(runner, times(4)).run(argumentCaptor.capture());
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries)
				.containsExactly(
					firstDrop, "CALL db.constraints()",
					secondDrop, "CALL db.constraints()"
				);
			verify(result, times(2)).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoMoreInteractions(runner, result, defaultResult);
		}
	}

	@Nested
	class IllegalArguments {

		@Test
		void nullVersionShouldRequireItem() {
			assertThatIllegalArgumentException().isThrownBy(() ->
					new DefaultDropOperation(null, null, null, true))
				.withMessage("Without a version, a concrete, local item is required.");
		}

		@Test
		void shouldBeAnExplicitConfiguration() {
			assertThatIllegalArgumentException().isThrownBy(() ->
					new DefaultDropOperation(MigrationVersion.withValue("1"), Name.of("whatever"), Constraint
						.forNode("Book")
						.named("book_id_unique")
						.unique("isbn"), true))
				.withMessage("Either reference or item is required, not both.");
		}
	}

	@Nested
	class SummaryCountersDecoupling {

		@Test
		void shouldConvertSummaryCounters() {
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.indexesAdded()).thenReturn(1);
			when(summaryCounters.indexesRemoved()).thenReturn(2);
			when(summaryCounters.constraintsAdded()).thenReturn(3);
			when(summaryCounters.constraintsRemoved()).thenReturn(4);

			Counters counters = Counters.of(summaryCounters);
			assertThat(counters.indexesAdded()).isEqualTo(1);
			assertThat(counters.indexesRemoved()).isEqualTo(2);
			assertThat(counters.constraintsAdded()).isEqualTo(3);
			assertThat(counters.constraintsRemoved()).isEqualTo(4);
		}

		@Test
		void shouldAdd() {

			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.indexesAdded()).thenReturn(1);
			when(summaryCounters.indexesRemoved()).thenReturn(2);
			when(summaryCounters.constraintsAdded()).thenReturn(3);
			when(summaryCounters.constraintsRemoved()).thenReturn(4);

			Counters counters = Counters.of(summaryCounters).add(Counters.of(summaryCounters));
			assertThat(counters.indexesAdded()).isEqualTo(2);
			assertThat(counters.indexesRemoved()).isEqualTo(4);
			assertThat(counters.constraintsAdded()).isEqualTo(6);
			assertThat(counters.constraintsRemoved()).isEqualTo(8);
		}

		@Test
		void shouldNoopEmpty() {

			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.indexesAdded()).thenReturn(1);
			when(summaryCounters.indexesRemoved()).thenReturn(2);
			when(summaryCounters.constraintsAdded()).thenReturn(3);
			when(summaryCounters.constraintsRemoved()).thenReturn(4);

			Counters original = Counters.of(summaryCounters);
			Counters counters = original.add(Counters.empty());
			assertThat(counters).isSameAs(original);
		}

		@Test
		void shouldAddThingsToEmpty() {

			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.indexesAdded()).thenReturn(1);
			when(summaryCounters.indexesRemoved()).thenReturn(2);
			when(summaryCounters.constraintsAdded()).thenReturn(3);
			when(summaryCounters.constraintsRemoved()).thenReturn(4);

			Counters counters = Counters.empty().add(Counters.of(summaryCounters));
			assertThat(counters.indexesAdded()).isEqualTo(1);
			assertThat(counters.indexesRemoved()).isEqualTo(2);
			assertThat(counters.constraintsAdded()).isEqualTo(3);
			assertThat(counters.constraintsRemoved()).isEqualTo(4);
		}

		@Test
		void shouldUseCorrectArgs() {

			Counters counters = Counters.of(1, 2, 3, 4);
			assertThat(counters.indexesAdded()).isEqualTo(1);
			assertThat(counters.indexesRemoved()).isEqualTo(2);
			assertThat(counters.constraintsAdded()).isEqualTo(3);
			assertThat(counters.constraintsRemoved()).isEqualTo(4);
		}
	}
}
