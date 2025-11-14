/*
 * Copyright 2020-2025 the original author or authors.
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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.annotations.proc.RelationshipType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class CatalogGeneratingProcessorTests {

	/**
	 * We have Spring on the Classpath anyway... So not reinvent the wheel for finding the
	 * resources.
	 */
	private static final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

	static Compiler getCompiler(Object... options) {

		String ts = "-A" + CatalogGeneratingProcessor.OPTION_TIMESTAMP + "=2022-09-21T21:21:00+01:00";
		Object[] defaultOptions;

		if (ToolProvider.getSystemJavaCompiler().isSupportedOption("--release") >= 0) {
			defaultOptions = new Object[] { ts, "--release", "8" };
		}
		else {
			defaultOptions = new Object[] { ts, "-source", "8", "-target", "8" };
		}

		Object[] finalOptions = new Object[options.length + defaultOptions.length];
		System.arraycopy(defaultOptions, 0, finalOptions, 0, defaultOptions.length);
		System.arraycopy(options, 0, finalOptions, defaultOptions.length, options.length);

		return Compiler.javac().withOptions(finalOptions);
	}

	static Stream<Arguments> shouldPreventInvalidStandaloneAnnotations() {
		return Stream.of(
				Arguments.of("CompositeOnSingleField",
						"Please annotate the class and not a field for composite constraints"),
				Arguments.of("ConflictingAliasesOGM",
						"Different @AliasFor or @ValueFor mirror values for annotation [org.neo4j.ogm.annotation.Property]"),
				Arguments.of("ContradictingLabels1", "Contradicting labels found: `foo`, `ContradictingLabels1`"),
				Arguments.of("ContradictingLabels2", "Contradicting labels found: `ContradictingLabels2`, `foo`"),
				Arguments.of("ContradictingLabels3", "Contradicting labels found: `ContradictingLabels3`, `foo`"),
				Arguments.of("ContradictingPropertiesOGM", "Contradicting properties: (bar) vs foo"),
				Arguments.of("ContradictingPropertiesSDN", "Contradicting properties: (bar) vs foo"),
				Arguments.of("MixingSDNAndOgm", "Mixing SDN and OGM annotations on the same class is not supported"),
				Arguments.of("NonUniqueLabelsOGM",
						"Explicit identifier `foo` on class contradicts identifier on annotation: `whatever`"),
				Arguments.of("NonUniqueLabelsSDN",
						"Explicit identifier `foo` on class contradicts identifier on annotation: `whatever`"),
				Arguments.of("NonUniqueTypesOGM",
						"Explicit identifier `BAR` on class contradicts identifier on annotation: `FOO`"),
				Arguments.of("PureWrong",
						"Ambiguous annotation @ac.simons.neo4j.migrations.annotations.catalog.Required(label=\"foo\", type=\"bar\")"),
				Arguments.of("UniqueOnRelOGM", "Unique constraints on relationships are not supported"),
				Arguments.of("UniqueOnRelSDN", "Unique constraints on relationships are not supported"),
				Arguments.of("WrongOverwritingDefaultOGM1", "Overwriting explicit type with a label is not supported"),
				Arguments.of("WrongOverwritingDefaultOGM2", "Overwriting explicit label with a type is not supported"),
				Arguments.of("FulltextOnFieldWithProperties",
						"Please annotate the class and not a field for composite constraints"));
	}

	static Stream<Arguments> shouldGenerateCatalog() {
		return Stream.of(
				Arguments.of("ac/simons/neo4j/migrations/annotations/proc/catalog/valid", "2022-11-17T21:21:00+01:00",
						"/expected_catalog_catalog.xml"),
				Arguments.of("ac/simons/neo4j/migrations/annotations/proc/catalog/valid_rels",
						"2022-11-17T21:21:00+01:00", "/expected_catalog_catalog_rels.xml"),
				Arguments.of("ac/simons/neo4j/migrations/annotations/proc/ogm", "2022-09-21T21:21:00+01:00",
						"/expected_catalog_ogm.xml"),
				Arguments.of("ac/simons/neo4j/migrations/annotations/proc/sdn6/movies", "2022-09-21T21:21:00+01:00",
						"/expected_catalog_sdn.xml"));
	}

	static Stream<Arguments> shouldGenerateTypeConstraints() {
		return Stream.of(
				Arguments.of("ac/simons/neo4j/migrations/annotations/proc/ogm", "2022-09-21T21:21:00+01:00",
						"/expected_catalog_ogm_movies_types.xml"),
				Arguments.of("ac/simons/neo4j/migrations/annotations/proc/sdn6/movies", "2022-09-21T21:21:00+01:00",
						"/expected_catalog_sdn_movies_types.xml"));
	}

	JavaFileObject[] getJavaResources(String base) {

		try {
			Resource[] resources = resourceResolver.getResources(base + "/**/*.java");
			JavaFileObject[] result = new JavaFileObject[resources.length];
			for (int i = 0; i < resources.length; i++) {
				result[i] = JavaFileObjects.forResource(resources[i].getURL());
			}
			return result;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Test
	void shouldHandleInvalid() {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler().withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/ogm_invalid"));

		assertThat(compilation).failed();
		assertThat(compilation)
			.hadErrorContaining("Cannot use org.neo4j.ogm.annotation.CompositeIndex without any properties");
		assertThat(compilation).hadErrorContaining(
				"Unique constraints defined at ac.simons.neo4j.migrations.annotations.proc.ogm_invalid.RelPropertyExistenceConstraintEntity are not allowed on relationships");
	}

	@ParameterizedTest
	@MethodSource
	void shouldPreventInvalidStandaloneAnnotations(String classFile, String expectedError) throws IOException {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler().withProcessors(catalogGeneratingProcessor)
			.compile(JavaFileObjects.forResource(resourceResolver.getResources(
					String.format("ac/simons/neo4j/migrations/annotations/proc/catalog/invalid/%s.java", classFile))[0]
				.getURL()));

		assertThat(compilation).failed();
		assertThat(compilation).hadErrorContaining(expectedError);
	}

	@ParameterizedTest
	@MethodSource
	void shouldGenerateCatalog(String packageName, String ts, String expected) throws IOException {

		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler().withProcessors(catalogGeneratingProcessor)
			.withOptions(String.format("-Aorg.neo4j.migrations.catalog_generator.timestamp=%s", ts))
			.compile(getJavaResources(packageName));

		assertThat(compilation).succeeded();
		String expectedCatalog;
		try (var in = this.getClass().getResourceAsStream(expected)) {
			expectedCatalog = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\t", "    ");
		}

		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations",
					CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
	}

	@ParameterizedTest // GH-774
	@ValueSource(strings = { "OrganizationClass", "OrganizationRecord" })
	void shouldDealWithClassesAndRecords(String classFile) throws IOException {

		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler().withProcessors(catalogGeneratingProcessor)
			.withOptions(
					String.format("-Aorg.neo4j.migrations.catalog_generator.timestamp=%s", "2022-12-09T21:21:00+01:00"))
			.compile(JavaFileObjects.forResource(resourceResolver.getResources(
					String.format("ac/simons/neo4j/migrations/annotations/proc/catalog/issues/%s.java", classFile))[0]
				.getURL()));

		String expectedCatalog;
		try (var in = this.getClass().getResourceAsStream(String.format("/expected_catalog_%s.xml", classFile))) {
			expectedCatalog = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\t", "    ");
		}

		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations",
					CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
	}

	@Test // GH-1116
	void classesNestedInTheirNonAbstractParentsMustNotCauseSOException() throws IOException {

		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler().withProcessors(catalogGeneratingProcessor)
			.withOptions(
					String.format("-Aorg.neo4j.migrations.catalog_generator.timestamp=%s", "2022-12-09T21:21:00+01:00"))
			.compile(JavaFileObjects.forResource(resourceResolver
				.getResources(String.format("ac/simons/neo4j/migrations/annotations/proc/misc/%s.java", "Fun"))[0]
				.getURL()));

		var expectedCatalog = """
				<?xml version="1.0" encoding="UTF-8" standalone="no"?>
				<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
				    <!-- This file was generated by Neo4j-Migrations at 2022-12-09T21:21:00+01:00. -->
				    <catalog>
				        <indexes/>
				        <constraints>
				            <constraint name="ac_simons_neo4j_migrations_annotations_proc_misc_fun_funfun_uuid_unique" type="unique">
				                <label>FunFun</label>
				                <properties>
				                    <property>uuid</property>
				                </properties>
				            </constraint>
				        </constraints>
				    </catalog>
				    <apply/>
				</migration>
				""";
		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations",
					CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
	}

	@Test
	void shouldInvokePrimaryKeyNameGeneratorProper() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/labels"));

		assertThat(compilation).succeeded();
		assertThat(generator.labels).hasSize(7)
			.containsEntry("SingleImplicitLabel", List.of("SingleImplicitLabel"))
			.containsEntry("SingleExplicitLabels.AsPrimaryLabel", List.of("pl"))
			.containsEntry("SingleExplicitLabels.AsValue", List.of("1o1"))
			.containsEntry("MultipleExplicitLabels.PrimaryAndValuesCombined", List.of("pl", "l1", "l2", "l3"))
			.containsEntry("MultipleExplicitLabels.MultipleValues", List.of("l1", "l2", "l3"))
			.containsEntry("InheritedLabels.Child", List.of("Child", "Person", "Base", "Bases"));
	}

	@Test
	void shouldIgnoreClassesWithGeneratedOrNoIdValues() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		assertThat(compilation).succeeded();
		assertThat(generator.labels).isEmpty();
	}

	@Test
	void shouldPassReset() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.add_reset=true",
					"-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		assertThat(compilation).succeeded();
		assertThat(generator.labels).isEmpty();

		var expectedCatalog = """
				<?xml version="1.0" encoding="UTF-8" standalone="no"?>
				<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
				    <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->
				    <catalog reset="true">
				        <indexes/>
				        <constraints/>
				    </catalog>
				    <apply/>
				</migration>""";
		assertThat(compilation).generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations/foo.txt")
			.contentsAsString(StandardCharsets.UTF_8)
			.contains(expectedCatalog);
	}

	@ParameterizedTest // GH-1021
	@MethodSource
	void shouldGenerateTypeConstraints(String packageName, String ts, String expected) throws IOException {

		String expectedCatalog;
		try (var in = this.getClass().getResourceAsStream(expected)) {
			expectedCatalog = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\t", "    ");
		}

		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler().withProcessors(catalogGeneratingProcessor)
			.withOptions(String.format("-Aorg.neo4j.migrations.catalog_generator.timestamp=%s", ts),
					String.format("-Aorg.neo4j.migrations.catalog_generator.generate_type_constraints=%s", true))
			.compile(getJavaResources(packageName));

		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations",
					CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);

	}

	@ParameterizedTest
	@ValueSource(strings = { "foo", "ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$HiddenA",
			"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$HiddenB",
			"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$NoDefaultCtor",
			"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$BellyUp",
			"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$WrongType" })
	void shouldUseDefaultCatalogNameGeneratorOnBogusThings(String fqn) {

		Compilation compilation = getCompiler().withProcessors(new CatalogGeneratingProcessor())
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.catalog_name_generator=" + fqn)
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		assertThat(compilation).succeeded();
		assertThat(compilation).hadWarningContaining(String.format("Could not load `%s`, using default for", fqn));
	}

	@Test
	void shouldPassOnOptions() {

		Compilation compilation = getCompiler().withProcessors(new CatalogGeneratingProcessor())
			.withOptions(
					"-Aorg.neo4j.migrations.catalog_generator.constraint_name_generator=ac.simons.neo4j.migrations.annotations.proc.impl.CatalogGeneratingProcessorTests$TestGenerator",
					"-Aorg.neo4j.migrations.catalog_generator.naming_options=foo=bar")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		assertThat(compilation).succeededWithoutWarnings();
	}

	@Test
	void shouldApplyOutputDirAndNameOptions() {

		Compilation compilation = getCompiler().withProcessors(new CatalogGeneratingProcessor())
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.default_catalog_name=foobar.txt",
					"-Aorg.neo4j.migrations.catalog_generator.output_dir=bazbar")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		assertThat(compilation).succeededWithoutWarnings();

		assertThat(compilation).generatedFile(StandardLocation.SOURCE_OUTPUT, "bazbar", "foobar.txt");
	}

	static class CollectingConstraintNameGenerator implements ConstraintNameGenerator {

		Map<String, List<String>> labels = new HashMap<>();

		@Override
		public String generateName(Constraint.Type type, Collection<PropertyType<?>> properties) {

			ElementType<?> owner = properties.stream()
				.findFirst()
				.map(PropertyType::getOwner)
				.orElseThrow(() -> new IllegalArgumentException(
						"Empty collection of properties passed to the name generator"));
			String owningTypeName = owner.getOwningTypeName();
			List<SchemaName> newLabels = (owner instanceof NodeType) ? ((NodeType) owner).getLabels()
					: List.of(((RelationshipType) owner).getName());
			this.labels.put(owningTypeName.substring(owningTypeName.lastIndexOf("labels.") + "labels.".length()),
					newLabels.stream().map(SchemaName::getValue).toList());
			return "n/a";
		}

	}

	static class TestGenerator implements ConstraintNameGenerator {

		@SuppressWarnings("RedundantModifier")
		public TestGenerator() {
			this(Map.of());
			throw new IllegalStateException("Not expected to be here");
		}

		@SuppressWarnings("RedundantModifier")
		public TestGenerator(Map<String, String> options) {
			if ("n/a".equals(options.getOrDefault("foo", "n/a"))) {
				throw new IllegalStateException("Options not set");
			}
		}

		@Override
		public String generateName(Constraint.Type type, Collection<PropertyType<?>> properties) {
			return null;
		}

	}

}
