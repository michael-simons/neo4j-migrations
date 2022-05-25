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

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PackageStructureTest {

	private JavaClasses coreClasses;

	@BeforeAll
	void importCorePackage() {
		coreClasses = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("ac.simons.neo4j.migrations.core..");
	}

	@Test
	public void catalogShouldBeIndependentFromCore() {

		ArchRule rule = noClasses()
			.that().resideInAPackage("..catalog..")
			.should().dependOnClassesThat()
			.resideInAPackage("..core");
		rule.check(coreClasses);
	}

	@Test
	public void internalShouldNotHaveDependencies() {

		ArchRule rule = noClasses()
			.that().resideInAPackage("..internal..")
			.should().dependOnClassesThat(
				resideOutsideOfPackages("..internal", "java..", "javax..", "org.xml..", "org.w3c..")
					.and(areNotPrimitives())
			);
		rule.check(coreClasses);
	}

	DescribedPredicate<JavaClass> areNotPrimitives() {
		return DescribedPredicate.not(new DescribedPredicate<>("Should be a primitive") {
			@Override
			public boolean apply(JavaClass input) {
				return input.isPrimitive() || (input.isArray() && input.getBaseComponentType().isPrimitive());
			}
		});
	}
}
