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
package ac.simons.neo4j.migrations.core.refactorings;

/**
 * Executable definition of a refactoring.
 *
 * @author Michael J. Simons
 * @soundtrack Nightwish - Decades: Live In Buenos Aires
 * @since 1.10.0
 */
public interface Refactoring {

	/**
	 * Applies this refactoring in the given context
	 *
	 * @param refactoringContext The context in which the refactoring is to be applied
	 * @return Statistics about modified schema content
	 */
	Counters apply(RefactoringContext refactoringContext);
}
