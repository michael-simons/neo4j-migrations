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
import ac.simons.neo4j.migrations.annotations.proc.Label;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
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
	void shouldGenerateCatalog() {
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor())
			.compile(getJavaResources("ac/simons/neo4j/migrations/annotations/proc/sdn6/movies"));

		CompilationSubject.assertThat(compilation).succeeded();

		Assertions.fail("not enough testing here...");
	}

	static class CollectingConstraintNameGenerator implements ConstraintNameGenerator {

		Map<String, List<String>> labels = new HashMap<>();

		@Override
		public String generateName(Constraint.Type type, PropertyType<NodeType> propertyType) {

			NodeType owner = propertyType.getOwner();
			String owningTypeName = owner.getOwningTypeName();
			this.labels.put(owningTypeName.substring(owningTypeName.lastIndexOf("labels.") + "labels.".length()),
				owner.getLabels().stream().map(Label::getValue).collect(Collectors.toList()));
			return "n/a";
		}
	}

	@Test
	void shouldInvokePrimaryKeyNameGeneratorProper() {

		CollectingConstraintNameGenerator generator = new CollectingConstraintNameGenerator();
		Compilation compilation = getCompiler()
			.withProcessors(new CatalogGeneratingProcessor(generator))
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

}
