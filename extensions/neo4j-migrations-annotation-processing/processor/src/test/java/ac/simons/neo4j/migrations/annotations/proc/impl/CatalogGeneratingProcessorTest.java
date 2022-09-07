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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.RelationshipType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;

/**
 * @author Michael J. Simons
 */
class CatalogGeneratingProcessorTest {

	/**
	 * We have Spring on the Classpath anyway... So not reinvent the wheel for finding the resources.
	 */
	private final static PathMatchingResourcePatternResolver resourceResolver
		= new PathMatchingResourcePatternResolver();

	static Compiler getCompiler(Object... options) {

		String ts = "-A" + CatalogGeneratingProcessor.OPTION_TIMESTAMP + "=2022-09-21T21:21:00+01:00";
		Object[] defaultOptions;

		if (ToolProvider.getSystemJavaCompiler().isSupportedOption("--release") >= 0) {
			defaultOptions = new Object[] { ts, "--release", "8" };
		} else {
			defaultOptions = new Object[] { ts, "-source", "8", "-target", "8" };
		}

		Object[] finalOptions = new Object[options.length + defaultOptions.length];
		System.arraycopy(defaultOptions, 0, finalOptions, 0, defaultOptions.length);
		System.arraycopy(options, 0, finalOptions, defaultOptions.length, options.length);

		return Compiler.javac().withOptions(finalOptions);
	}

	JavaFileObject[] getJavaResources(String base) {

		try {
			Resource[] resources = resourceResolver.getResources(base + "/**/*.java");
			JavaFileObject[] result = new JavaFileObject[resources.length];
			for (int i = 0; i < resources.length; i++) {
				result[i] = JavaFileObjects.forResource(resources[i].getURL());
			}
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Test
	void shouldHandleInvalid() {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/ogm_invalid"));

		CompilationSubject.assertThat(compilation).failed();
		CompilationSubject.assertThat(compilation)
			.hadErrorContaining("Cannot use org.neo4j.ogm.annotation.CompositeIndex without any properties");
		CompilationSubject.assertThat(compilation)
			.hadErrorContaining("Unique constraints defined at ac.simons.neo4j.migrations.annotations.proc.ogm_invalid.RelPropertyExistenceConstraintEntity are not allowed on relationships");
	}

	@Test
	void shouldGenerateCatalogOGM() {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/ogm"));

		CompilationSubject.assertThat(compilation).succeeded();
		CompilationSubject.assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations", CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(""
				+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
				+ "    <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->\n"
				+ "    <catalog>\n"
				+ "        <indexes>\n"
				+ "            <index name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_singleindexentity_login_property\" type=\"property\">\n"
				+ "                <label>Entity</label>\n"
				+ "                <properties>\n"
				+ "                    <property>login</property>\n"
				+ "                </properties>\n"
				+ "            </index>\n"
				+ "            <index name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_relpropertyindextentity_description_property\" type=\"property\">\n"
				+ "                <type>RELPROPERTYINDEXTENTITY</type>\n"
				+ "                <properties>\n"
				+ "                    <property>description</property>\n"
				+ "                </properties>\n"
				+ "            </index>\n"
				+ "            <index name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_compositeindexentity_name_age_property\" type=\"property\">\n"
				+ "                <label>EntityWithCompositeIndex</label>\n"
				+ "                <properties>\n"
				+ "                    <property>name</property>\n"
				+ "                    <property>age</property>\n"
				+ "                </properties>\n"
				+ "            </index>\n"
				+ "            <index name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_multiplecompositeindexentity_firstName_age_property\" type=\"property\">\n"
				+ "                <label>EntityWithMultipleCompositeIndexes</label>\n"
				+ "                <properties>\n"
				+ "                    <property>firstName</property>\n"
				+ "                    <property>age</property>\n"
				+ "                </properties>\n"
				+ "            </index>\n"
				+ "            <index name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_multiplecompositeindexentity_firstName_email_property\" type=\"property\">\n"
				+ "                <label>EntityWithMultipleCompositeIndexes</label>\n"
				+ "                <properties>\n"
				+ "                    <property>firstName</property>\n"
				+ "                    <property>email</property>\n"
				+ "                </properties>\n"
				+ "            </index>\n"
				+ "        </indexes>\n"
				+ "        <constraints>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_entitywithassignedid_id_unique\" type=\"unique\">\n"
				+ "                <label>EntityWithAssignedId</label>\n"
				+ "                <properties>\n"
				+ "                    <property>id</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_entitywithexternallygeneratedid_id_unique\" type=\"unique\">\n"
				+ "                <label>EntityWithExternallyGeneratedId</label>\n"
				+ "                <properties>\n"
				+ "                    <property>id</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_nodepropertyexistenceconstraintentity_login_exists\" type=\"exists\">\n"
				+ "                <label>Entity</label>\n"
				+ "                <properties>\n"
				+ "                    <property>login</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_uniqueconstraintentity_login_unique\" type=\"unique\">\n"
				+ "                <label>Entity</label>\n"
				+ "                <properties>\n"
				+ "                    <property>login</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_relpropertyexistenceconstraintentity_description_exists\" type=\"exists\">\n"
				+ "                <type>REL</type>\n"
				+ "                <properties>\n"
				+ "                    <property>description</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_ogm_nodekeyconstraintentity_name_age_key\" type=\"key\">\n"
				+ "                <label>Entity</label>\n"
				+ "                <properties>\n"
				+ "                    <property>name</property>\n"
				+ "                    <property>age</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "        </constraints>\n"
				+ "    </catalog>\n"
				+ "    <apply/>\n"
				+ "</migration>\n"
			);
	}

	@Test
	void shouldGenerateCatalogSDN6() {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/movies"));

		CompilationSubject.assertThat(compilation).succeeded();
		CompilationSubject.assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations", CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(""
				+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
				+ "    <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->\n"
				+ "    <catalog>\n"
				+ "        <indexes/>\n"
				+ "        <constraints>\n"
				+ "            <constraint name=\"ac_simons_neo4j_migrations_annotations_proc_sdn6_movies_movie_title_unique\" type=\"unique\">\n"
				+ "                <label>Movie</label>\n"
				+ "                <properties>\n"
				+ "                    <property>title</property>\n"
				+ "                </properties>\n"
				+ "            </constraint>\n"
				+ "        </constraints>\n"
				+ "    </catalog>\n"
				+ "    <apply/>\n"
				+ "</migration>\n"
			);
	}

	static class CollectingConstraintNameGenerator implements ConstraintNameGenerator {

		Map<String, List<String>> labels = new HashMap<>();

		@Override
		public String generateName(Constraint.Type type, Collection<PropertyType<?>> properties) {

			ElementType<?> owner = properties.stream().findFirst().map(PropertyType::getOwner).orElseThrow(
				() -> new IllegalArgumentException("Empty collection of properties passed to the name generator"));
			String owningTypeName = owner.getOwningTypeName();
			List<SchemaName> newLabels = owner instanceof NodeType ?
				((NodeType) owner).getLabels() :
				Collections.singletonList(((RelationshipType) owner).getName());
			this.labels.put(owningTypeName.substring(owningTypeName.lastIndexOf("labels.") + "labels.".length()),
				newLabels.stream().map(SchemaName::getValue).collect(Collectors.toList()));
			return "n/a";
		}
	}

	@Test
	void shouldInvokePrimaryKeyNameGeneratorProper() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/labels"));

		CompilationSubject.assertThat(compilation).succeeded();
		assertThat(generator.labels)
			.hasSize(7)
			.containsEntry("SingleImplicitLabel",
				Collections.singletonList("SingleImplicitLabel"))
			.containsEntry("SingleExplicitLabels.AsPrimaryLabel",
				Collections.singletonList("pl"))
			.containsEntry("SingleExplicitLabels.AsValue",
				Collections.singletonList("1o1"))
			.containsEntry("MultipleExplicitLabels.PrimaryAndValuesCombined",
				Arrays.asList("pl", "l1", "l2", "l3"))
			.containsEntry("MultipleExplicitLabels.MultipleValues",
				Arrays.asList("l1", "l2", "l3"))
			.containsEntry("InheritedLabels.Child",
				Arrays.asList("Child", "Person", "Base", "Bases"));
	}

	@Test
	void shouldIgnoreClassesWithGeneratedOrNoIdValues() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		CompilationSubject.assertThat(compilation).succeeded();
		assertThat(generator.labels).isEmpty();
	}

	@Test
	void shouldPassReset() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.add_reset=true", "-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		CompilationSubject.assertThat(compilation).succeeded();
		assertThat(generator.labels).isEmpty();

		CompilationSubject.assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations/foo.txt")
			.contentsAsString(StandardCharsets.UTF_8)
			.contains(""
				+ "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
				+ "<migration xmlns=\"https://michael-simons.github.io/neo4j-migrations\">\n"
				+ "    <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->\n"
				+ "    <catalog reset=\"true\">\n"
				+ "        <indexes/>\n"
				+ "        <constraints/>\n"
				+ "    </catalog>\n"
				+ "    <apply/>\n"
				+ "</migration>"
			);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"foo",
		"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$HiddenA",
		"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$HiddenB",
		"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$NoDefaultCtor",
		"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$BellyUp",
		"ac.simons.neo4j.migrations.annotations.proc.impl.Testgenerators$WrongType",
	})
	void shouldUseDefaultCatalogNameGeneratorOnBogusThings(String fqn) {

		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor())
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.catalog_name_generator=" + fqn)
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		CompilationSubject
			.assertThat(compilation).succeeded();
		CompilationSubject
			.assertThat(compilation).hadWarningContaining(String.format("Could not load `%s`, using default for", fqn));
	}

	@Test
	void shouldPassOnOptions() {

		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor())
			.withOptions(
				"-Aorg.neo4j.migrations.catalog_generator.constraint_name_generator=ac.simons.neo4j.migrations.annotations.proc.impl.CatalogGeneratingProcessorTest$TestGenerator",
				"-Aorg.neo4j.migrations.catalog_generator.naming_options=foo=bar"
			)
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		CompilationSubject
			.assertThat(compilation).succeededWithoutWarnings();
	}

	@Test
	void shouldApplyOutputDirAndNameOptions() {

		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor())
			.withOptions(
				"-Aorg.neo4j.migrations.catalog_generator.default_catalog_name=foobar.txt",
				"-Aorg.neo4j.migrations.catalog_generator.output_dir=bazbar"
			)
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/ignored"));

		CompilationSubject
			.assertThat(compilation).succeededWithoutWarnings();

		CompilationSubject.assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "bazbar", "foobar.txt");
	}

	static class TestGenerator implements ConstraintNameGenerator {

		private Map<String, String> options;

		@SuppressWarnings("RedundantModifier")
		public TestGenerator() {
			this(Collections.emptyMap());
			throw new IllegalStateException("Not expected to be here");
		}

		@SuppressWarnings("RedundantModifier")
		public TestGenerator(Map<String, String> options) {
			this.options = options;
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
