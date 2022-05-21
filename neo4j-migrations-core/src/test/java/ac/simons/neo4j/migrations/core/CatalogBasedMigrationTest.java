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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.CatalogBasedMigration.Operation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.OperationContext;
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
	class Applies extends MockHolder {

		@Test
		void shouldDealWithEmptyCatalogs() {
			Operation operation = Operation.use(new DefaultCatalog()).apply();

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
			operation.apply(context, runner);

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(Neo4jVersion.V4_4.getShowConstraints());
			verify(defaultResult).stream();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldDealWithEmptyDatabaseCatalog() {
			Operation operation = Operation.use(catalog).apply();

			Result createResult = mock(Result.class);
			ResultSummary summary = mock(ResultSummary.class);
			SummaryCounters counters = mock(SummaryCounters.class);
			when(counters.constraintsAdded()).thenReturn(22);
			when(counters.indexesAdded()).thenReturn(20);
			when(summary.counters()).thenReturn(counters);
			when(createResult.consume()).thenReturn(summary);

			String createQuery = "CREATE CONSTRAINT book_id_unique FOR (n:Book) REQUIRE n.isbn IS UNIQUE";
			when(runner.run(createQuery)).thenReturn(createResult);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
			operation.apply(context, runner);

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
			Operation operation = Operation.use(new DefaultCatalog()).apply();

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

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
			operation.apply(context, runner);

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
		void simpleOpsShouldWork(Operator operatorUnderTest, Neo4jVersion version, String expected) {

			Operation operation;
			if (operatorUnderTest == Operator.CREATE) {
				operation = Operation.use(catalog)
					.create(Name.of("book_id_unique"), false)
					.with(MigrationVersion.withValue("1"));
			} else if (operatorUnderTest == Operator.DROP) {
				operation = Operation.use(catalog)
					.drop(Name.of("book_id_unique"), false)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operatorUnderTest);
			}

			OperationContext context = new OperationContext(version, Neo4jEdition.ENTERPRISE);
			operation.apply(context, runner);

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expected);
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, defaultResult);
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
		void idempotencyOnSupportedTargetsShouldWork(Operator operatorUnderTest, String expectedQuery) {

			final Operation operation;
			if (operatorUnderTest == Operator.CREATE) {
				operation = Operation.use(catalog)
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else if (operatorUnderTest == Operator.DROP) {
				operation = Operation.use(catalog)
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operatorUnderTest);
			}

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
			operation.apply(context, runner);

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@ParameterizedTest
		@EnumSource(value = Operator.class)
		void shouldCheckForExistence(Operator operatorUnderTest) {

			final Operation op;
			if (operatorUnderTest == Operator.CREATE) {
				op = Operation.use(catalog)
					.create(Name.of("trololo"), false)
					.with(MigrationVersion.withValue("1"));
			} else if (operatorUnderTest == Operator.DROP) {
				op = Operation.use(catalog)
					.drop(Name.of("trololo"), false)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operatorUnderTest);
			}

			OperationContext context = new OperationContext(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE);
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> op.apply(context, runner))
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
				op = Operation.use(catalog)
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else if (operator == Operator.DROP) {
				op = Operation.use(catalog)
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			op.apply(context, runner);

			verify(runner, times(1)).run(argumentCaptor.capture());
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, defaultResult);
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
				op = Operation.use(catalog)
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else if (operator == Operator.DROP) {
				op = Operation.use(catalog)
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			when(runner.run(expectedQuery)).thenThrow(new DatabaseException("Something else is broken", "Oh :("));
			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> op.apply(context, runner))
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
				op = Operation.use(catalog)
					.create(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
				when(result.list(Mockito.<Function<Record, Constraint>>any()))
					.thenAnswer(i -> Stream.of(new MapAccessorAndRecordImpl(Collections.singletonMap("description",
							Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE")))).map(i.getArgument(0))
						.collect(Collectors.toList()));
			} else if (operator == Operator.DROP) {
				op = Operation.use(catalog)
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

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			op.apply(context, runner);

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

			Operation op = Operation.use(catalog)
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

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			op.apply(context, runner);

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptionsWhenNotExistsInTheEnd() {

			Operation op = Operation.use(catalog)
				.create(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any())).thenReturn(Collections.emptyList());

			String expectedDrop = "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_ALREADY_EXISTS, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(runner.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> op.apply(context, runner))
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

			Operation drop = Operation.use(catalog)
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

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptionsWhenStillExists() {

			Operation drop = Operation.use(catalog)
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

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> drop.apply(context, runner))
				.withMessage("Oh :(");

			verify(runner).run(expectedDrop);
			verify(runner).run(expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldTryDroppingOlderVersions() {

			Operation drop = Operation.use(catalog)
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

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);

			verify(runner, times(3)).run(argumentCaptor.capture());
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries)
				.containsExactly(
					firstDrop,
					"CALL db.constraints()",
					"DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"
				);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, result, defaultResult);
		}

		@Test
		void shouldNotEndlessLoopWhenTryingOlderVersions() {

			Operation drop = Operation.use(catalog)
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

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);

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
}
