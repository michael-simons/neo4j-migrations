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

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.annotations.proc.RelationshipType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;
import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.testing.compile.Compilation;
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

		assertThat(compilation).failed();
		assertThat(compilation)
			.hadErrorContaining("Cannot use org.neo4j.ogm.annotation.CompositeIndex without any properties");
		assertThat(compilation)
			.hadErrorContaining("Unique constraints defined at ac.simons.neo4j.migrations.annotations.proc.ogm_invalid.RelPropertyExistenceConstraintEntity are not allowed on relationships");
	}

	@Test
	void shouldGenerateCatalogOGM() {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/ogm"));

		assertThat(compilation).succeeded();
		var expectedCatalog = """
			<?xml version="1.0" encoding="UTF-8" standalone="no"?>
			<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
			    <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->
			    <catalog>
			        <indexes>
			            <index name="ac_simons_neo4j_migrations_annotations_proc_ogm_singleindexentity_login_property" type="property">
			                <label>Entity</label>
			                <properties>
			                    <property>login</property>
			                </properties>
			            </index>
			            <index name="ac_simons_neo4j_migrations_annotations_proc_ogm_relpropertyindextentity_description_property" type="property">
			                <type>REL_PROPERTY_INDEXT_ENTITY</type>
			                <properties>
			                    <property>description</property>
			                </properties>
			            </index>
			            <index name="ac_simons_neo4j_migrations_annotations_proc_ogm_compositeindexentity_name_age_property" type="property">
			                <label>EntityWithCompositeIndex</label>
			                <properties>
			                    <property>name</property>
			                    <property>age</property>
			                </properties>
			            </index>
			            <index name="ac_simons_neo4j_migrations_annotations_proc_ogm_multiplecompositeindexentity_firstName_age_property" type="property">
			                <label>EntityWithMultipleCompositeIndexes</label>
			                <properties>
			                    <property>firstName</property>
			                    <property>age</property>
			                </properties>
			            </index>
			            <index name="ac_simons_neo4j_migrations_annotations_proc_ogm_multiplecompositeindexentity_firstName_email_property" type="property">
			                <label>EntityWithMultipleCompositeIndexes</label>
			                <properties>
			                    <property>firstName</property>
			                    <property>email</property>
			                </properties>
			            </index>
			        </indexes>
			        <constraints>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_ogm_entitywithassignedid_id_unique" type="unique">
			                <label>EntityWithAssignedId</label>
			                <properties>
			                    <property>id</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_ogm_entitywithexternallygeneratedid_id_unique" type="unique">
			                <label>EntityWithExternallyGeneratedId</label>
			                <properties>
			                    <property>id</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_ogm_nodepropertyexistenceconstraintentity_login_exists" type="exists">
			                <label>Entity</label>
			                <properties>
			                    <property>login</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_ogm_uniqueconstraintentity_login_unique" type="unique">
			                <label>Entity</label>
			                <properties>
			                    <property>login</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_ogm_relpropertyexistenceconstraintentity_description_exists" type="exists">
			                <type>REL</type>
			                <properties>
			                    <property>description</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_ogm_nodekeyconstraintentity_name_age_key" type="key">
			                <label>Entity</label>
			                <properties>
			                    <property>name</property>
			                    <property>age</property>
			                </properties>
			            </constraint>
			        </constraints>
			    </catalog>
			    <apply/>
			</migration>
			""";
		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations", CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
	}

	@Test
	void shouldGenerateCatalogSDN6() {
		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/movies"));

		assertThat(compilation).succeeded();
		var expectedCatalog = """
			<?xml version="1.0" encoding="UTF-8" standalone="no"?>
			<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
			    <!-- This file was generated by Neo4j-Migrations at 2022-09-21T21:21:00+01:00. -->
			    <catalog>
			        <indexes/>
			        <constraints>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_sdn6_movies_movie_title_unique" type="unique">
			                <label>Movie</label>
			                <properties>
			                    <property>title</property>
			                </properties>
			            </constraint>
			        </constraints>
			    </catalog>
			    <apply/>
			</migration>
			""";
		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations", CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
	}

	@Nested
	class InvalidStandAloneAnnotations {

		public static final String BASE = "ac/simons/neo4j/migrations/annotations/proc/catalog/invalid";

		@Test
		void shouldNotAllowCompositeOnSingleField() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/CompositeOnSingleField.java")[0].getURL()));

			assertThat(compilation).failed();
		}

		@Test
		void shouldPreventConflictingAliasesOGM() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/ConflictingAliasesOGM.java")[0].getURL()));

			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Different @AliasFor or @ValueFor mirror values for annotation [org.neo4j.ogm.annotation.Property]");
		}

		@ParameterizedTest
		@ValueSource(strings = {"ContradictingLabels1.java", "ContradictingLabels2.java"})
		void shouldPreventContradictingLabels(String src) throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/" + src)[0].getURL()));

			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Contradicting labels found: ");
		}

		@Test
		void shouldPreventContradictingPropertiesOGM() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/ContradictingPropertiesOGM.java")[0].getURL()));

			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Contradicting properties: (bar) vs foo");
		}

		@Test
		void shouldPreventContradictingPropertiesSDN() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/ContradictingPropertiesSDN.java")[0].getURL()));

			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Contradicting properties: (bar) vs foo");
		}

		@Test
		void shouldPreventMixingSDNAndOgm() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/MixingSDNAndOgm.java")[0].getURL()));
			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Mixing SDN and OGM annotations on the same class is not supported");
		}

		@Test
		void shouldPreventNonUniqueLabelsOGM() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/NonUniqueLabelsOGM.java")[0].getURL()));
			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Explicit identifier `foo` on class contradicts identifier on annotation: `whatever`");
		}

		@Test
		void shouldPreventNonUniqueLabelsSDN() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/NonUniqueLabelsSDN.java")[0].getURL()));
			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Explicit identifier `foo` on class contradicts identifier on annotation: `whatever`");
		}

		@Test
		void shouldPreventNonUniqueTypesOGM() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/NonUniqueTypesOGM.java")[0].getURL()));
			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Explicit identifier `BAR` on class contradicts identifier on annotation: `FOO`");
		}

		@Test
		void shouldPreventPureWrong() throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/PureWrong.java")[0].getURL()));
			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Ambiguous annotation @ac.simons.neo4j.migrations.annotations.catalog.Required(label=\"foo\", type=\"bar\")");
		}

		@ParameterizedTest
		@ValueSource(strings = {"UniqueOnRelOGM.java", "UniqueOnRelSDN.java"})
		void shouldPreventUniqueOnRel(String src) throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/" + src)[0].getURL()));

			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContaining("Unique constraints on relationships are not supported");
		}

		@ParameterizedTest
		@ValueSource(strings = {"WrongOverwritingDefaultOGM1.java", "WrongOverwritingDefaultOGM2.java"})
		void shouldPreventWrongOverwrite(String src) throws IOException {

			CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
			Compilation compilation = getCompiler()
				.withProcessors(catalogGeneratingProcessor)
				.compile(JavaFileObjects.forResource(resourceResolver.getResources(BASE + "/" + src)[0].getURL()));

			assertThat(compilation).failed();
			assertThat(compilation)
				.hadErrorContainingMatch("Overwriting explicit (type|label) with a (label|type) is not supported");
		}
	}

	@Test
	void shouldGenerateCatalogCatalogForRels() {

		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-11-17T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/catalog/valid_rels"));

		assertThat(compilation).succeeded();
		var expectedCatalog = """
			<?xml version="1.0" encoding="UTF-8" standalone="no"?>
			<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
			    <!-- This file was generated by Neo4j-Migrations at 2022-11-17T21:21:00+01:00. -->
			    <catalog>
			        <indexes/>
			        <constraints>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_rels_ogmrelprops_x_exists" type="exists">
			                <type>O_G_M_REL_PROPS</type>
			                <properties>
			                    <property>x</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_rels_ogmrelpropsexplicit1_x_exists" type="exists">
			                <type>FOO</type>
			                <properties>
			                    <property>x</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_rels_ogmrelpropsexplicit2_x_exists" type="exists">
			                <type>BAR</type>
			                <properties>
			                    <property>x</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_rels_overwritingdefaultogm_uuid_exists" type="exists">
			                <type>WHATEVER</type>
			                <properties>
			                    <property>uuid</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_rels_sdnrelprops_x_exists" type="exists">
			                <type>S_D_N_REL_PROPS</type>
			                <properties>
			                    <property>x</property>
			                </properties>
			            </constraint>
			        </constraints>
			    </catalog>
			    <apply/>
			</migration>
			""";

		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations", CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
	}

		@Test
	void shouldGenerateCatalogCatalog() {

		CatalogGeneratingProcessor catalogGeneratingProcessor = new CatalogGeneratingProcessor();
		Compilation compilation = getCompiler()
			.withProcessors(catalogGeneratingProcessor)
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-11-17T21:21:00+01:00")
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/catalog/valid"));

		assertThat(compilation).succeeded();
		var expectedCatalog = """
			<?xml version="1.0" encoding="UTF-8" standalone="no"?>
			<migration xmlns="https://michael-simons.github.io/neo4j-migrations">
			    <!-- This file was generated by Neo4j-Migrations at 2022-11-17T21:21:00+01:00. -->
			    <catalog>
			        <indexes/>
			        <constraints>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanmultiple1_unique_unique" type="unique">
			                <label>CoffeeBeanMultiple1</label>
			                <properties>
			                    <property>unique</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanmultiple1_a_b_c_unique" type="unique">
			                <label>CoffeeBeanMultiple1</label>
			                <properties>
			                    <property>a</property>
			                    <property>b</property>
			                    <property>c</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanmultiple2_unique_unique" type="unique">
			                <label>CoffeeBeanMultiple2</label>
			                <properties>
			                    <property>unique</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanmultiple2_a_b_c_unique" type="unique">
			                <label>CoffeeBeanMultiple2</label>
			                <properties>
			                    <property>a</property>
			                    <property>b</property>
			                    <property>c</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanogm_a_b_c_unique" type="unique">
			                <label>CBOGM</label>
			                <properties>
			                    <property>a</property>
			                    <property>b</property>
			                    <property>c</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanogm_uuid_unique" type="unique">
			                <label>CBOGM</label>
			                <properties>
			                    <property>uuid</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanogm_name_exists" type="exists">
			                <label>CBOGM</label>
			                <properties>
			                    <property>name</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanogm_theName_exists" type="exists">
			                <label>CBOGM</label>
			                <properties>
			                    <property>theName</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanogm_theOtherName_exists" type="exists">
			                <label>CBOGM</label>
			                <properties>
			                    <property>theOtherName</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpure_a_b_c_unique" type="unique">
			                <label>CoffeeBeanPure</label>
			                <properties>
			                    <property>a</property>
			                    <property>b</property>
			                    <property>c</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpure_uuid_unique" type="unique">
			                <label>CoffeeBeanPure</label>
			                <properties>
			                    <property>uuid</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpure_name_exists" type="exists">
			                <label>CoffeeBeanPure</label>
			                <properties>
			                    <property>name</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpure_theName_exists" type="exists">
			                <label>CoffeeBeanPure</label>
			                <properties>
			                    <property>theName</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpure_nameB_exists" type="exists">
			                <label>CoffeeBeanPure</label>
			                <properties>
			                    <property>nameB</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpureexplicitlabel_a_b_c_unique" type="unique">
			                <label>foo</label>
			                <properties>
			                    <property>a</property>
			                    <property>b</property>
			                    <property>c</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpureexplicitlabel_uuid_unique" type="unique">
			                <label>foo</label>
			                <properties>
			                    <property>uuid</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpureexplicitlabel_name_exists" type="exists">
			                <label>foo</label>
			                <properties>
			                    <property>name</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeanpureexplicitlabel_theName_exists" type="exists">
			                <label>foo</label>
			                <properties>
			                    <property>theName</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeansdn6_a_b_c_unique" type="unique">
			                <label>CBSDN6</label>
			                <properties>
			                    <property>a</property>
			                    <property>b</property>
			                    <property>c</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeansdn6_uuid_unique" type="unique">
			                <label>CBSDN6</label>
			                <properties>
			                    <property>uuid</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeansdn6_name_exists" type="exists">
			                <label>CBSDN6</label>
			                <properties>
			                    <property>name</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeansdn6_theName_exists" type="exists">
			                <label>CBSDN6</label>
			                <properties>
			                    <property>theName</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_coffeebeansdn6_theOtherName_exists" type="exists">
			                <label>CBSDN6</label>
			                <properties>
			                    <property>theOtherName</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_overwritingdefaultogm_uuid_unique" type="unique">
			                <label>whatever</label>
			                <properties>
			                    <property>uuid</property>
			                </properties>
			            </constraint>
			            <constraint name="ac_simons_neo4j_migrations_annotations_proc_catalog_valid_overwritingdefaultsdn6_uuid_unique" type="unique">
			                <label>whatever</label>
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
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations", CatalogGeneratingProcessor.DEFAULT_MIGRATION_NAME)
			.contentsAsString(StandardCharsets.UTF_8)
			.isEqualTo(expectedCatalog);
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
				List.of(((RelationshipType) owner).getName());
			this.labels.put(owningTypeName.substring(owningTypeName.lastIndexOf("labels.") + "labels.".length()),
				newLabels.stream().map(SchemaName::getValue).toList());
			return "n/a";
		}
	}

	@Test
	void shouldInvokePrimaryKeyNameGeneratorProper() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(() -> "foo.txt", generator, null))
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/labels"));

		assertThat(compilation).succeeded();
		assertThat(generator.labels)
			.hasSize(7)
			.containsEntry("SingleImplicitLabel",
				List.of("SingleImplicitLabel"))
			.containsEntry("SingleExplicitLabels.AsPrimaryLabel",
				List.of("pl"))
			.containsEntry("SingleExplicitLabels.AsValue",
				List.of("1o1"))
			.containsEntry("MultipleExplicitLabels.PrimaryAndValuesCombined",
				List.of("pl", "l1", "l2", "l3"))
			.containsEntry("MultipleExplicitLabels.MultipleValues",
				List.of("l1", "l2", "l3"))
			.containsEntry("InheritedLabels.Child",
				List.of("Child", "Person", "Base", "Bases"));
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
			.withOptions("-Aorg.neo4j.migrations.catalog_generator.add_reset=true", "-Aorg.neo4j.migrations.catalog_generator.timestamp=2022-09-21T21:21:00+01:00")
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
		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "neo4j-migrations/foo.txt")
			.contentsAsString(StandardCharsets.UTF_8)
			.contains(expectedCatalog);
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

		assertThat(compilation).succeeded();
		assertThat(compilation).hadWarningContaining(String.format("Could not load `%s`, using default for", fqn));
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

		assertThat(compilation).succeededWithoutWarnings();
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

		assertThat(compilation).succeededWithoutWarnings();

		assertThat(compilation)
			.generatedFile(StandardLocation.SOURCE_OUTPUT, "bazbar", "foobar.txt");
	}

	static class TestGenerator implements ConstraintNameGenerator {

		private final Map<String, String> options;

		@SuppressWarnings("RedundantModifier")
		public TestGenerator() {
			this(Map.of());
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
