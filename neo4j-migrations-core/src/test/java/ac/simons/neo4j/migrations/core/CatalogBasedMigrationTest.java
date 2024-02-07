/*
 * Copyright 2020-2024 the original author or authors.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ac.simons.neo4j.migrations.core.CatalogBasedMigration.ApplyOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.CreateOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.DefaultDropOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.DefaultRefactorOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.ItemSpecificOperation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.Operation;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.OperationContext;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.VerificationFailedException;
import ac.simons.neo4j.migrations.core.CatalogBasedMigration.VerifyOperation;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.catalog.Operator;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.refactorings.AddSurrogateKey;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Merge;
import ac.simons.neo4j.migrations.core.refactorings.MigrateBTreeIndexes;
import ac.simons.neo4j.migrations.core.refactorings.Normalize;
import ac.simons.neo4j.migrations.core.refactorings.Rename;
import ac.simons.neo4j.migrations.test_resources.TestResources;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Michael J. Simons
 */
class CatalogBasedMigrationTest {

	@ParameterizedTest
	@ValueSource(strings = { "01.xml", "02.xml", "03.xml" })
	void checksumShouldBeCorrect(String in) {
		URL url = TestResources.class.getResource("/catalogbased/identical-migrations/V01__" + in);
		Objects.requireNonNull(url);
		CatalogBasedMigration schemaBasedMigration = (CatalogBasedMigration) CatalogBasedMigration.from(url);
		assertThat(schemaBasedMigration.getChecksum()).hasValue("2210671299");
		assertThat(schemaBasedMigration.getCatalog().getItems()).hasSize(2);
	}

	@Test
	void shouldParsePreconditions1() {
		URL url = TestResources.class.getResource("/catalogbased/identical-migrations/V01__01.xml");
		Objects.requireNonNull(url);
		CatalogBasedMigration schemaBasedMigration = (CatalogBasedMigration) CatalogBasedMigration.from(url);
		assertThat(schemaBasedMigration.getPreconditions()).hasSize(2);
		assertThat(schemaBasedMigration.getPreconditions()).map(Precondition::getType)
			.containsExactlyInAnyOrder(Precondition.Type.ASSERTION, Precondition.Type.ASSUMPTION);
		assertThat(schemaBasedMigration.getPreconditions())
			.allMatch(p -> p instanceof EditionPrecondition || p instanceof QueryPrecondition);
	}

	@Test
	void shouldParseDescription() {

		URL url = Objects.requireNonNull(TestResources.class.getResource("/catalogbased/identical-migrations/V01__01.xml"));

		Migration migration = CatalogBasedMigration.from(url);
		assertThat(migration.getOptionalDescription()).hasValue("01");
	}

	@Test
	void shouldParsePreconditions2() {
		URL url = TestResources.class.getResource("/preconditions/V0002__Create_node_keys.xml");
		Objects.requireNonNull(url);
		CatalogBasedMigration schemaBasedMigration = (CatalogBasedMigration) CatalogBasedMigration.from(url);
		assertThat(schemaBasedMigration.isResetCatalog()).isFalse();
		assertThat(schemaBasedMigration.getPreconditions()).hasSize(1);
		assertThat(schemaBasedMigration.getPreconditions()).singleElement()
			.extracting(Precondition::getType)
			.isEqualTo(Precondition.Type.ASSUMPTION);
		assertThat(schemaBasedMigration.getPreconditions()).singleElement()
			.extracting(QueryPrecondition.class::cast)
			.extracting(QueryPrecondition::getQuery)
			.isEqualTo("RETURN false");
	}

	@Test
	void shouldParseReset() {
		URL url = TestResources.class.getResource("/catalogbased/parsing/V01__with_reset.xml");
		Objects.requireNonNull(url);
		CatalogBasedMigration schemaBasedMigration = (CatalogBasedMigration) CatalogBasedMigration.from(url);
		assertThat(schemaBasedMigration.isResetCatalog()).isTrue();
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

		final ac.simons.neo4j.migrations.core.catalog.Index bookPropertyIndexV1 =
			ac.simons.neo4j.migrations.core.catalog.Index
				.forNode("Book")
				.named("index_name")
				.onProperties("property1", "property2");

		final ac.simons.neo4j.migrations.core.catalog.Index bookPropertyIndexV2 =
			ac.simons.neo4j.migrations.core.catalog.Index
				.forNode("Book")
				.named("another_index_name")
				.onProperties("property12", "property22");

		final String indexQueryV1 = "CREATE INDEX index_name FOR (n:Book) ON (n.property1, n.property2)";
		final String indexQueryV2 = "CREATE INDEX another_index_name FOR (n:Book) ON (n.property12, n.property22)";

		final VersionedCatalog catalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());

		ArgumentCaptor<String> argumentCaptor;

		Session session;

		Result defaultResult;

		MockHolder() {
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("1"),
				() -> Arrays.asList(uniqueBookIdV1, bookPropertyIndexV1), false);
			((WriteableCatalog) catalog).addAll(MigrationVersion.withValue("2"),
				() -> Arrays.asList(uniqueBookIdV2, bookPropertyIndexV2), false);
		}

		@BeforeEach
		void initMocks() {

			defaultResult = mock(Result.class);
			session = mock(Session.class);
			when(session.run(Mockito.anyString())).thenReturn(defaultResult);
			argumentCaptor = ArgumentCaptor.forClass(String.class);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class RefactoringParsing {

		@Test
		void allRefactoringsShouldWork() {

			Map<String, ac.simons.neo4j.migrations.core.catalog.Index.Type> typeMappings = Map.of(
				"c", ac.simons.neo4j.migrations.core.catalog.Index.Type.POINT,
				"d", ac.simons.neo4j.migrations.core.catalog.Index.Type.TEXT
			);

			URL url = TestResources.class.getResource("/catalogbased/parsing/full-example.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			assertThat(CatalogBasedMigration.parseOperations(document, MigrationVersion.baseline()))
				.filteredOn(op -> op instanceof DefaultRefactorOperation)
				.map(DefaultRefactorOperation.class::cast)
				.map(op -> op.refactoring)
				.containsExactly(
					Merge.nodes("MATCH (n:Foo) RETURN n"),
					Merge.nodes("MATCH (p:Person) RETURN p ORDER BY p.name ASC", Arrays.asList(
						Merge.PropertyMergePolicy.of("name", Merge.PropertyMergePolicy.Strategy.KEEP_LAST),
						Merge.PropertyMergePolicy.of(".*", Merge.PropertyMergePolicy.Strategy.KEEP_FIRST)
					)),
					Rename.type("ACTED_IN", "HAT_GESPIELT_IN"),
					Rename.label("MOVIE", "FILM"),
					Rename.nodeProperty("released", "veröffentlicht im Jahr"),
					Rename.relationshipProperty("roles", "rollen"),
					Rename.type("ACTED_IN", "HAT_GESPIELT_IN")
						.withCustomQuery(
							"MATCH (n:Movie) <-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n")
						.inBatchesOf(23),
					AddSurrogateKey.toNodes("Movie", "Person"),
					AddSurrogateKey.toNodes("Movie", "Person")
						.withGeneratorFunction("elementId(%s)")
						.inBatchesOf(23)
						.withProperty("theId"),
					AddSurrogateKey.toNodesMatching("MATCH (n:Film) return n"),
					AddSurrogateKey.toRelationships("LIKED"),
					AddSurrogateKey.toRelationships("LIKED")
						.withGeneratorFunction("id(%s)")
						.inBatchesOf(42)
						.withProperty("myId"),
					AddSurrogateKey.toRelationshipsMatching("MATCH (n:Movie) &lt;-[r:ACTED_IN] -() WHERE n.title =~ '.*Matrix.*' RETURN r AS n"),
					Normalize.asBoolean("title",
							Arrays.asList("The Matrix"),
							Arrays.asList("Das deutsche Kettensägenmassaker", null, null))
						.withCustomQuery("MATCH (n:Movie) return n")
						.inBatchesOf(42),
					MigrateBTreeIndexes.createFutureIndexes(),
					MigrateBTreeIndexes
						.createFutureIndexes("_future")
						.withExcludes(Arrays.asList("a", "b"))
						.withTypeMapping(typeMappings),
					MigrateBTreeIndexes.replaceBTreeIndexes(),
					MigrateBTreeIndexes.replaceBTreeIndexes()
						.withExcludes(Arrays.asList("a", "b"))
						.withTypeMapping(typeMappings),
					MigrateBTreeIndexes.replaceBTreeIndexes()
						.withIncludes(Arrays.asList("x", "y"))
				);
		}

		Document errorSource;

		@BeforeAll
		void loadErrorSource() throws Exception {
			errorSource = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().parse(
					TestResources.class.getResource("/catalogbased/parsing/broken-refactorings.xml")
						.openStream());
		}

		Node getElementById(String id) {
			NodeList refactor = errorSource.getElementsByTagName("refactor");
			for (int i = 0; i < refactor.getLength(); ++i) {
				Node element = refactor.item(i);
				if (element.getAttributes().getNamedItem("id").getNodeValue().equals(id)) {
					return element;
				}
			}
			throw new NoSuchElementException(id);
		}

		@ParameterizedTest
		@CsvSource(nullValues = "n/a", textBlock = """
			surrogate-node-no-label, Cannot parse <refactor type="addSurrogateKeyTo.nodes" /> into a supported refactoring: No labels specified
			surrogate-broken-batch, Cannot parse <refactor type="addSurrogateKeyTo.nodes" /> into a supported refactoring: Invalid value `wurstalat` for parameter `batchSize`
			surrogate-rel-no-type, Cannot parse <refactor type="addSurrogateKeyTo.relationships" /> into a supported refactoring: No `type` parameter
			surrogate-invalid, Cannot parse <refactor type="addSurrogateKeyTo.invalid" /> into a supported refactoring: `invalid` is not a valid rename operation
			surrogate-no-args, Cannot parse <refactor type="addSurrogateKeyTo.nodes" /> into a supported refactoring: The addSurrogateKey refactoring requires several parameters
			""")
		void brokenSurrogates(String id, String message) {

			Node refactoring = getElementById(id);
			assertThatIllegalArgumentException().isThrownBy(() -> CatalogBasedRefactorings.fromNode(refactoring))
				.withMessage(message);
		}

		@ParameterizedTest
		@CsvSource(nullValues = "n/a", textBlock = """
			create-future-index-empty-suffix, n/a
			create-future-index-empty-ignore1, n/a
			create-future-index-empty-ignore2, n/a
			create-future-index-empty-tm1, n/a
			create-future-index-empty-tm2, n/a
			create-future-index-empty-duplicate-name, is
			create-future-index-empty-no-name, Index name is required when customizing type mappings
			create-future-index-empty-no-type, Type is required when customizing type mappings
			""")
		void brokenMigrateBtreeNodes(String id, String message) {

			Node refactoring = getElementById(id);
			if (message == null) {
				MigrateBTreeIndexes migrateBTreeIndexes = (MigrateBTreeIndexes) CatalogBasedRefactorings.fromNode(refactoring);
				assertThat(migrateBTreeIndexes).isEqualTo(MigrateBTreeIndexes.createFutureIndexes());
			} else if ("is".equals(message)) {
				assertThatIllegalArgumentException().isThrownBy(() -> CatalogBasedRefactorings.fromNode(refactoring))
					.withMessage("Duplicate child node `name`");
			} else {
				assertThatIllegalArgumentException().isThrownBy(() -> CatalogBasedRefactorings.fromNode(refactoring))
					.withMessage(message);
			}
		}

		@ParameterizedTest
		@ValueSource(ints = { 1, 2, 3, 4, 5 })
		void brokenMergeNodes(int id) {

			Node refactoring = getElementById("merge.nodes.no-source-query" + id);
			assertThatIllegalArgumentException().isThrownBy(() -> CatalogBasedRefactorings.fromNode(refactoring))
				.withMessage(
					"Cannot parse <refactor type=\"merge.nodes\" /> into a supported refactoring: No source query");
		}

		@ParameterizedTest
		@CsvSource(textBlock = """
			invalid-type,: `invalid` is not a valid rename operation
			no-from,: No `from` parameter
			no-to,: No `to` parameter
			no-anything-1,: The rename refactoring requires `from` and `to` parameters
			no-anything-2,: No `from` parameter
			invalid-batch-size,: Invalid value `foobar` for parameter `batchSize`
			""")
		void brokenRenames(String id, String message) {
			Node refactoring = getElementById("rename-" + id);
			String type = "invalid-type".equals(id) ? "invalid" : "label";
			assertThatIllegalArgumentException().isThrownBy(() -> CatalogBasedRefactorings.fromNode(refactoring))
				.withMessage(
					"Cannot parse <refactor type=\"rename.%s\" /> into a supported refactoring%s", type, message);
		}

		@ParameterizedTest
		@CsvSource(delimiterString = "@@", textBlock = """
			nothing@@The normalizeAsBoolean refactoring requires `property`, `trueValues` and `falseValues` parameters
			no-op@@No `property` parameter
			no-truth@@No `trueValues` parameter
			just-the-truth@@No `falseValues` parameter
			""")
		void brokenNormalizes(String id, String messsage) {
			Node refactoring = getElementById("normalize-" + id);
			assertThatIllegalArgumentException().isThrownBy(() -> CatalogBasedRefactorings.fromNode(refactoring))
				.withMessageEndingWith(messsage);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Operations {

		@Test
		void emptyDocumentShouldBeReadable() {
			URL url = TestResources.class.getResource("/catalogbased/parsing/no-operations.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			assertThat(CatalogBasedMigration.parseOperations(document, MigrationVersion.withValue("1"))).isEmpty();
		}

		@Test
		void applyShouldWork() {

			URL url = TestResources.class.getResource("/catalogbased/parsing/apply.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);
			List<Operation> operations = CatalogBasedMigration.parseOperations(document,
				MigrationVersion.withValue("1"));
			assertThat(operations).hasSize(1)
				.satisfiesExactly(op -> assertThat(op).isInstanceOf(ApplyOperation.class));
		}

		@Test
		void defaultVerifyShouldWork() {

			URL url = TestResources.class.getResource("/catalogbased/parsing/verify-default.xml");
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
					assertThat(verifyOperation.includeOptions()).isFalse();
					assertThat(verifyOperation.definedAt()).isEqualTo(MigrationVersion.withValue("1"));
				});
		}

		@Test
		void verifyModifiedShouldWork() {

			URL url = TestResources.class.getResource("/catalogbased/parsing/verify-modified.xml");
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
					assertThat(verifyOperation.includeOptions()).isTrue();
					assertThat(verifyOperation.definedAt()).isEqualTo(MigrationVersion.withValue("1"));
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

			URL url = TestResources.class.getResource("/catalogbased/parsing/" + file);
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

			URL url = TestResources.class.getResource("/catalogbased/parsing/" + file + "-both-ref-and-item.xml");
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

			URL url = TestResources.class.getResource("/catalogbased/parsing/" + file + "-both-item-and-local.xml");
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
				new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator()), MigrationsConfig.defaultConfig(), () -> session);

			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes());
			verify(defaultResult, times(2)).stream();
			verifyNoMoreInteractions(session, defaultResult);
		}

		@Test
		void shouldUsePriorVersion() {

			Operation operation = Operation
				.verify(false) // There is nothing prior to version 1, so this will be empty.
				.allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog, MigrationsConfig.defaultConfig(),
				() -> session);

			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes());
			verify(defaultResult, times(2)).stream();
			verifyNoMoreInteractions(session, defaultResult);
		}

		@Test
		void shouldThrowWhenNotIdentical() {

			Operation operation = Operation
				.verify(true)
				.allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(),
				() -> session);

			assertThatExceptionOfType(VerificationFailedException.class)
				.isThrownBy(() -> operation.execute(context))
				.withMessage("Catalogs are neither identical nor equivalent.");

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes());
			verify(defaultResult, times(2)).stream();
			verifyNoMoreInteractions(session, defaultResult);
		}

		@Test
		void shouldFailWhenEquivalencyIsNotAllowed() {

			Map<String, Value> constraints = new HashMap<>();
			constraints.put("name", Values.value("foo"));
			constraints.put("description", Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE"));

			Map<String, Value> indexes = new HashMap<>();
			indexes.put("name", Values.value("index_name"));
			indexes.put("type", Values.value("BTREE"));
			indexes.put("entityType", Values.value("NODE"));
			indexes.put("labelsOrTypes", Values.value(Collections.singletonList("Book")));
			indexes.put("properties", Values.value(Arrays.asList("property1", "property2")));

			when(defaultResult.stream())
				.thenReturn(Stream.of(new MapAccessorAndRecordImpl(constraints)))
				.thenReturn(Stream.of(new MapAccessorAndRecordImpl(indexes)));

			Operation operation = Operation
				.verify(true)
				.allowEquivalent(false)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog, MigrationsConfig.defaultConfig(),
				() -> session);

			assertThatExceptionOfType(VerificationFailedException.class)
				.isThrownBy(() -> operation.execute(context))
				.withMessage(
					"Database schema and the catalog are equivalent but the verification requires them to be identical.");

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes());
			verify(defaultResult, times(2)).stream();
			verifyNoMoreInteractions(session, defaultResult);
		}

		@Test
		void shouldNotFailWhenEquivalencyIsAllowed() {

			Map<String, Value> constraints = new HashMap<>();
			constraints.put("name", Values.value("foo"));
			constraints.put("description", Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.id) IS UNIQUE"));

			Map<String, Value> indexes = new HashMap<>();
			indexes.put("name", Values.value("index_name"));
			indexes.put("type", Values.value("BTREE"));
			indexes.put("entityType", Values.value("NODE"));
			indexes.put("labelsOrTypes", Values.value(Collections.singletonList("Book")));
			indexes.put("properties", Values.value(Arrays.asList("property1", "property2")));

			when(defaultResult.stream())
				.thenReturn(Stream.of(new MapAccessorAndRecordImpl(constraints)))
				.thenReturn(Stream.of(new MapAccessorAndRecordImpl(indexes)));

			Operation operation = Operation
				.verify(true)
				.allowEquivalent(true)
				.at(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog, MigrationsConfig.defaultConfig(),
				() -> session);

			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes());
			verify(defaultResult, times(2)).stream();
			verifyNoMoreInteractions(session, defaultResult);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class Applies extends MockHolder {

		@Test
		void shouldDealWithEmptyCatalogs() {
			Operation operation = Operation.apply(MigrationVersion.withValue("1"));

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE,
				new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator()), MigrationsConfig.defaultConfig(), () -> session);
			assertThatNoException().isThrownBy(() -> operation.execute(context));

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes());
			verify(defaultResult, times(2)).stream();
			verifyNoMoreInteractions(session, defaultResult);
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
			when(session.run(Mockito.anyString())).thenReturn(defaultResult);
			when(session.run(createQuery)).thenReturn(createResult);
			when(defaultResult.consume()).thenReturn(summary);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			operation.execute(context);

			verify(session, times(5)).run(argumentCaptor.capture());
			verify(session).close();
			assertThat(argumentCaptor.getAllValues())
				.containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes(),
						indexQueryV1, indexQueryV2, createQuery);
			verify(defaultResult, times(2)).stream();
			verify(defaultResult, times(2)).consume();
			verify(createResult).consume();
			verify(summary, times(3)).counters();
			verify(counters, times(3)).constraintsAdded();
			verify(counters, times(3)).indexesAdded();
			verifyNoMoreInteractions(session, defaultResult, summary, counters);
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

			Map<String, Value> indexes = new HashMap<>();
			indexes.put("name", Values.value("index_name"));
			indexes.put("type", Values.value("BTREE"));
			indexes.put("entityType", Values.value("NODE"));
			indexes.put("labelsOrTypes", Values.value(Collections.singletonList("Book")));
			indexes.put("properties", Values.value(Arrays.asList("property1", "property2")));

			when(defaultResult.stream())
				.thenReturn(Stream.of(new MapAccessorAndRecordImpl(constraints)))
				.thenReturn(Stream.of(new MapAccessorAndRecordImpl(indexes)));

			String dropQuery = "DROP CONSTRAINT book_id_unique";
			String dropIndex = "DROP INDEX index_name";
			when(session.run(dropQuery)).thenReturn(dropResult);
			when(session.run(dropIndex)).thenReturn(dropResult);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE,
				new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator()), MigrationsConfig.defaultConfig(), () -> session);
			operation.execute(context);

			verify(session, times(4)).run(argumentCaptor.capture());
			verify(session).close();
			assertThat(argumentCaptor.getAllValues())
				.containsExactly(Neo4jVersion.V4_4.getShowConstraints(), Neo4jVersion.V4_4.getShowIndexes(),
						dropQuery, dropIndex);
			verify(defaultResult, times(2)).stream();
			verify(dropResult, times(2)).consume();
			verify(summary, times(2)).counters();
			verify(counters, times(2)).constraintsRemoved();
			verify(counters, times(2)).indexesRemoved();
			verifyNoMoreInteractions(session, defaultResult, summary, counters);
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
			when(session.run(expectedQuery)).thenReturn(opResult);

			OperationContext context = new OperationContext(version, Neo4jEdition.ENTERPRISE, catalog, MigrationsConfig.defaultConfig(), () -> session);
			Counters counters = operation.execute(context);
			if (operator == Operator.CREATE) {
				assertThat(counters.constraintsAdded()).isEqualTo(1);
			} else {
				assertThat(counters.constraintsRemoved()).isEqualTo(2);
			}

			verify(session, times(1)).run(argumentCaptor.capture());
			verify(session).close();
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(opResult).consume();
			verifyNoMoreInteractions(session, defaultResult, opResult);
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
			when(session.run(expectedQuery)).thenReturn(opResult);

			OperationContext context = new OperationContext(Neo4jVersion.V4_4, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			Counters counters = operation.execute(context);
			if (operator == Operator.CREATE) {
				assertThat(counters.constraintsAdded()).isEqualTo(1);
			} else {
				assertThat(counters.constraintsRemoved()).isEqualTo(2);
			}

			verify(session, times(1)).run(argumentCaptor.capture());
			verify(session).close();
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(opResult).consume();
			verifyNoMoreInteractions(session, defaultResult, opResult);
		}

		@Test // GH-1182
		void optionsShouldBeApply() {
			URL url = TestResources.class.getResource("/catalogbased/actual-migrations-with-complete-verification/V20__Apply_complete_catalog.xml");
			Objects.requireNonNull(url);
			Document document = CatalogBasedMigration.parseDocument(url);

			List<Operation> operations = CatalogBasedMigration.parseOperations(document,
				MigrationVersion.baseline());

			Result opResult = mock(Result.class);
			ResultSummary resultSummary = mock(ResultSummary.class);
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.constraintsAdded()).thenReturn(1);
			when(summaryCounters.constraintsRemoved()).thenReturn(2);
			when(resultSummary.counters()).thenReturn(summaryCounters);
			when(opResult.consume()).thenReturn(resultSummary);
			when(session.run(anyString())).thenReturn(opResult);

			var localCatalog = new DefaultCatalog(MigrationsConfig.defaultConfig().getVersionComparator());
			localCatalog.addAll(MigrationVersion.baseline(), Catalog.of(document), false);
			OperationContext context = new OperationContext(Neo4jVersion.V5, Neo4jEdition.ENTERPRISE, localCatalog,
				MigrationsConfig.builder().withConstraintRenderingOptions(List.of(new RenderConfig.CypherRenderingOptions() {
					@Override
					public boolean includingOptions() {
						return true;
					}
				})).build(), () -> session);
			operations.get(0).execute(context);

			verify(session, times(5)).run(argumentCaptor.capture());
			verify(session).close();
			assertThat(argumentCaptor.getAllValues())
				.contains("CREATE CONSTRAINT unique_isbn FOR (n:Book) REQUIRE n.isbn IS UNIQUE OPTIONS {`indexConfig`: {`spatial.cartesian.min`: [-1000000.0, -1000000.0], `spatial.wgs-84.min`: [-180.0, -90.0], `spatial.wgs-84.max`: [180.0, 90.0], `spatial.cartesian.max`: [1000000.0, 1000000.0], `spatial.wgs-84-3d.max`: [180.0, 90.0, 1000000.0], `spatial.cartesian-3d.min`: [-1000000.0, -1000000.0, -1000000.0], `spatial.cartesian-3d.max`: [1000000.0, 1000000.0, 1000000.0], `spatial.wgs-84-3d.min`: [-180.0, -90.0, -1000000.0]}, `indexProvider`: \"native-btree-1.0\"}");
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
				MigrationsConfig.defaultConfig(), () -> session);
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
			when(session.run(expectedQuery)).thenReturn(opResult);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			Counters counters = op.execute(context);
			if (operator == Operator.CREATE) {
				assertThat(counters.constraintsAdded()).isEqualTo(1);
			} else {
				assertThat(counters.constraintsRemoved()).isEqualTo(2);
			}

			verify(session, times(1)).run(argumentCaptor.capture());
			verify(session).close();
			String query = argumentCaptor.getValue();
			assertThat(query).isEqualTo(expectedQuery);
			verify(opResult).consume();
			verifyNoMoreInteractions(session, defaultResult, opResult);
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

			when(session.run(expectedQuery)).thenThrow(new DatabaseException("Something else is broken", "Oh :("));
			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> op.execute(context))
				.withMessage("Oh :(");

			verify(session).run(expectedQuery);
			verify(session).close();
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(session, defaultResult);
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
						.toList());
			} else if (operator == Operator.DROP) {
				op = Operation
					.drop(Name.of("book_id_unique"), true)
					.with(MigrationVersion.withValue("1"));
				when(result.list(Mockito.<Function<Record, Constraint>>any())).thenReturn(Collections.emptyList());
			} else {
				throw new IllegalArgumentException("Unsupported operator under test " + operator);
			}

			when(session.run(expectedQuery))
				.thenThrow(new DatabaseException(codeToThrow, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(session.run(expectedCall))
				.thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			op.execute(context);

			verify(session).run(expectedQuery);
			verify(session).run(expectedCall);
			verify(session).close();
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(session, result, defaultResult);
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
					.toList());

			String expectedDrop = "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(session.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_ALREADY_EXISTS, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(session.run(expectedCall))
				.thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			op.execute(context);

			verify(session).run(expectedDrop);
			verify(session).run(expectedCall);
			verify(session).close();
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(session, result, defaultResult);
		}

		@Test
		void idempotencyOnUnsupportedTargetsShouldRethrowExceptionsWhenNotExistsInTheEnd() {

			Operation op = Operation
				.create(Name.of("book_id_unique"), true)
				.with(MigrationVersion.withValue("1"));

			Result result = mock(Result.class);
			when(result.list(Mockito.<Function<Record, Constraint>>any())).thenReturn(Collections.emptyList());

			String expectedDrop = "CREATE CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(session.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_ALREADY_EXISTS, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(session.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> op.execute(context))
				.withMessage("Oh :(");

			verify(session).run(expectedDrop);
			verify(session).run(expectedCall);
			verify(session).close();
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(session, result, defaultResult);
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
					.toList());

			String expectedDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(session.run(expectedDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(session.run(expectedCall))
				.thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			drop.execute(context);

			verify(session).run(expectedDrop);
			verify(session).run(expectedCall);
			verify(session).close();
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoInteractions(defaultResult);
			verifyNoMoreInteractions(session, result, defaultResult);
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
					.toList());

			String expectedContraintDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(session.run(expectedContraintDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedConstraintCall = "CALL db.constraints()";
			when(session.run(expectedConstraintCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			assertThatExceptionOfType(DatabaseException.class)
				.isThrownBy(() -> drop.execute(context))
				.withMessage("Oh :(");

			verify(session).run(expectedContraintDrop);
			verify(session).run(expectedConstraintCall);
			verify(session).close();
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoMoreInteractions(session, result, defaultResult);
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
					.toList());

			String firstDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE";
			when(session.run(firstDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = "CALL db.constraints()";
			when(session.run(expectedCall)).thenReturn(result);

			Result dropResult = mock(Result.class);
			ResultSummary resultSummary = mock(ResultSummary.class);
			SummaryCounters summaryCounters = mock(SummaryCounters.class);
			when(summaryCounters.constraintsRemoved()).thenReturn(1);
			when(resultSummary.counters()).thenReturn(summaryCounters);
			when(dropResult.consume()).thenReturn(resultSummary);
			when(session.run("DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE")).thenReturn(dropResult);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			Counters counters = drop.execute(context);
			assertThat(counters.constraintsRemoved()).isEqualTo(1);

			verify(session, times(3)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries)
				.containsExactly(
					firstDrop,
					"CALL db.constraints()",
					"DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE"
				);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verify(dropResult).consume();
			verifyNoMoreInteractions(session, result, defaultResult, dropResult);
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
					.toList());

			String firstDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE";
			when(session.run(firstDrop))
				.thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			String expectedCall = Neo4jVersion.V3_5.getShowConstraints();
			when(session.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			drop.execute(context);

			verify(session, times(2)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries).containsExactly(firstDrop, expectedCall);
			verify(result).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoMoreInteractions(session, result, defaultResult);
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
						.map(i.getArgument(0)).toList()
				)
				.thenAnswer(i -> Stream.of(
						new MapAccessorAndRecordImpl(Collections.singletonMap("description",
							Values.value("CONSTRAINT ON ( book:Book ) ASSERT (book.foobar) IS UNIQUE")))
					).map(i.getArgument(0)).toList()
				);

			String firstDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.isbn IS UNIQUE";
			String secondDrop = "DROP CONSTRAINT ON (n:Book) ASSERT n.id IS UNIQUE";
			when(session.run(firstDrop)).thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));
			when(session.run(secondDrop)).thenThrow(new DatabaseException(Neo4jCodes.CONSTRAINT_DROP_FAILED, "Oh :("));

			String expectedCall = "CALL db.constraints()";
			when(session.run(expectedCall)).thenReturn(result);

			OperationContext context = new OperationContext(Neo4jVersion.V3_5, Neo4jEdition.ENTERPRISE, catalog,
				MigrationsConfig.defaultConfig(), () -> session);
			drop.execute(context);

			verify(session, times(4)).run(argumentCaptor.capture());
			verify(session).close();
			List<String> queries = argumentCaptor.getAllValues();
			assertThat(queries)
				.containsExactly(
					firstDrop, "CALL db.constraints()",
					secondDrop, "CALL db.constraints()"
				);
			verify(result, times(2)).list(Mockito.<Function<Record, Constraint>>any());
			verifyNoMoreInteractions(session, result, defaultResult);
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

			Counters counters = CatalogBasedMigration.schemaCounters(summaryCounters);
			assertThat(counters.indexesAdded()).isEqualTo(1);
			assertThat(counters.indexesRemoved()).isEqualTo(2);
			assertThat(counters.constraintsAdded()).isEqualTo(3);
			assertThat(counters.constraintsRemoved()).isEqualTo(4);
		}

		@Test
		void shouldConvertSummaryCountersIndividually() {
			Counters counters = CatalogBasedMigration.schemaCounters(1, 2, 3, 4);
			assertThat(counters.indexesAdded()).isEqualTo(1);
			assertThat(counters.indexesRemoved()).isEqualTo(2);
			assertThat(counters.constraintsAdded()).isEqualTo(3);
			assertThat(counters.constraintsRemoved()).isEqualTo(4);
		}
	}
}
