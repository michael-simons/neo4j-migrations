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
package ac.simons.neo4j.migrations.core;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackages;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * @author Michael J. Simons
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PackageStructureTests {

	private final DescribedPredicate<JavaClass> areInCoreExceptVersionAndEditionAPI = resideInAPackage("..core")
		.and(not(assignableTo(Neo4jVersion.class).or(assignableTo(Neo4jEdition.class))));

	private JavaClasses coreClasses;

	@BeforeAll
	void importCorePackage() {
		this.coreClasses = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("ac.simons.neo4j.migrations.core..");
	}

	@Test
	void catalogShouldBeIndependentFromCore() {

		ArchRule rule = noClasses().that()
			.resideInAPackage("..catalog..")
			.should()
			.dependOnClassesThat(this.areInCoreExceptVersionAndEditionAPI);
		rule.check(this.coreClasses);
	}

	@Test
	void refactoringShouldBeIndependentFromCore() {

		ArchRule rule = noClasses().that()
			.resideInAPackage("..refactorings..")
			.should()
			.dependOnClassesThat(this.areInCoreExceptVersionAndEditionAPI);
		rule.check(this.coreClasses);
	}

	@Test
	void internalShouldNotHaveDependencies() {

		ArchRule rule = noClasses().that()
			.resideInAPackage("..internal..")
			.should()
			.dependOnClassesThat(resideOutsideOfPackages("..internal", "java..", "javax..", "org.xml..", "org.w3c..",
					"org.neo4j.cypherdsl.support.schema_name.." // That will be shaded
																// anyway into the same
																// internal package
			).and(areNotPrimitives()));
		rule.check(this.coreClasses);
	}

	DescribedPredicate<JavaClass> areNotPrimitives() {
		return not(new DescribedPredicate<JavaClass>("Should be a primitive") {
			@Override
			public boolean test(JavaClass input) {
				return input.isPrimitive() || (input.isArray() && input.getBaseComponentType().isPrimitive());
			}
		});
	}

}
