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
package ac.simons.neo4j.migrations.core.refactorings;

/**
 * Generates format strings for refactorings, depending on targets and / or custom
 * queries.
 *
 * @author Michael J. Simons
 */
interface FormatStringGenerator {

	/**
	 * {@return the fragments making up the format string}
	 */
	Fragments getFragments();

	default String generateFormatString(String customQuery, Integer batchSize) {
		Fragments fragments = getFragments();

		String ptSource;
		String ptAction;

		if (customQuery == null) {
			ptSource = fragments.source();
		}
		else {
			ptSource = fragments.sourceWithCustomQuery();
		}

		if (batchSize == null) {
			ptAction = fragments.action();
		}
		else {
			ptAction = fragments.actionWithBatchSize();
		}

		return ptSource + " " + ptAction;
	}

	/**
	 * Value class for the fragments of a format string.
	 *
	 * @param source the fragment used to select the source
	 * @param sourceWithCustomQuery the fragment used to select the source by a custom
	 * query
	 * @param action the action to deal with elements
	 * @param actionWithBatchSize the action to deal with elements in batches
	 */
	record Fragments(String source, String sourceWithCustomQuery, String action, String actionWithBatchSize) {
	}

}
