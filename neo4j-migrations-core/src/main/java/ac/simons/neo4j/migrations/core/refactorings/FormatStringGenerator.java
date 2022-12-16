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
 * Generates format strings for refactorings, depending on targets and / or custom queries.
 * @author Michael J. Simons
 */
interface FormatStringGenerator {

	/**
	 * @return The fragment used to select the source
	 */
	String getSourceFragment();

	/**
	 * @return The fragment used to select the source by a custom query
	 */
	String getSourceFragmentWithCustomQuery();

	/**
	 * @return The action to deal with elements
	 */
	String getActionFragment();

	/**
	 * @return The action to deal with elements in batches
	 */
	String getActionFragmentWithBatchSize();

	default String generateFormatString(String customQuery, Integer batchSize) {
		String ptSource;
		String ptAction;

		if (customQuery == null) {
			ptSource = getSourceFragment();
		} else {
			ptSource = getSourceFragmentWithCustomQuery();
		}

		if (batchSize == null) {
			ptAction = getActionFragment();
		} else {
			ptAction = getActionFragmentWithBatchSize();
		}

		return ptSource + " " + ptAction;
	}

}
