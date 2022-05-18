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

import ac.simons.neo4j.migrations.core.CatalogBasedMigration.DropOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.Operation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.OperationContext;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.net.URL;
import java.util.Collections;
import java.util.List;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.DatabaseException;

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
		assertThat(schemaBasedMigration.getChecksum()).hasValue("620177586");
		assertThat(schemaBasedMigration.getCatalog().getItems()).hasSize(2);
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Drops {

		ArgumentCaptor<String> configArgumentCaptor;

		Constraint uniqueBookIdV1 = Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("id");

		Constraint uniqueBookIdV2 = Constraint
			.forNode("Book")
			.named("book_id_unique")
			.unique("isbn");

		VersionedCatalog catalog;

		QueryRunner runner;

		Result defaultResult;

		@BeforeEach
		void initMocks() {

			catalog = new DefaultCatalog();
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("1"),
				() -> Collections.singletonList(uniqueBookIdV1));
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("2"),
				() -> Collections.singletonList(uniqueBookIdV2));

			defaultResult = mock(Result.class);
			runner = mock(QueryRunner.class);
			when(runner.run(Mockito.anyString())).thenReturn(defaultResult);
			configArgumentCaptor = ArgumentCaptor.forClass(String.class);
		}

		@SuppressWarnings("unused")
		Stream<Arguments> simpleDropShouldWork() {

			return Stream.of(
				Arguments.of(Neo4jVersion.V3_5, "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"),
				Arguments.of(Neo4jVersion.V4_4, "DROP CONSTRAINT book_id_unique")
			);
		}

		@ParameterizedTest
		@MethodSource
		void simpleDropShouldWork(Neo4jVersion version, String expected) {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("book_id_unique"), false)
				.with(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(version, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);

			verify(runner, times(1)).run(configArgumentCaptor.capture());
			String query = configArgumentCaptor.getValue();
			assertThat(query).isEqualTo(expected);
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void shouldCheckForExistence() {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("trololo"), false)
				.with(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.LATEST, Neo4jEdition.ENTERPRISE);
			assertThatExceptionOfType(MigrationsException.class)
				.isThrownBy(() -> drop.apply(context, runner))
				.withMessage("An item named 'trololo' has not been defined as of version 1.");
		}

		@Test
		void idempotencyOnSupportedTargetsShouldWork() {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);

			verify(runner, times(1)).run(configArgumentCaptor.capture());
			String query = configArgumentCaptor.getValue();
			assertThat(query).isEqualTo("DROP CONSTRAINT book_id_unique IF EXISTS");
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldWork() {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			drop.apply(context, runner);

			verify(runner, times(1)).run(configArgumentCaptor.capture());
			String query = configArgumentCaptor.getValue();
			assertThat(query).isEqualTo("DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE");
			verify(defaultResult).consume();
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldIgnoreNoSuchConstraintErrors() {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any())).thenReturn(Collections.emptyList());

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
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptions() {

			DropOperation drop = Operation.use(catalog)
				.drop(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			String expectedDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(runner.run(expectedDrop))
				.thenThrow(new DatabaseException("Something else is broken", "Oh :("));

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> drop.apply(context, runner))
				.withMessage("Oh :(");

			verify(runner).run(expectedDrop);
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(runner, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldByHappyWithOtherConstraints() {

			DropOperation drop = Operation.use(catalog)
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

			DropOperation drop = Operation.use(catalog)
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

			DropOperation drop = Operation.use(catalog)
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

			verify(runner, times(3)).run(configArgumentCaptor.capture());
			List<String> queries = configArgumentCaptor.getAllValues();
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

			DropOperation drop = Operation.use(catalog)
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

			verify(runner, times(4)).run(configArgumentCaptor.capture());
			List<String> queries = configArgumentCaptor.getAllValues();
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
