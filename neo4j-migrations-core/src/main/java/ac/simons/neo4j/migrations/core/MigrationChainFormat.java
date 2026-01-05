/*
 * Copyright 2020-2026 the original author or authors.
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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for formatting a migration chain and its elements.
 *
 * @author Michael J. Simons
 * @since 0.0.11
 */
final class MigrationChainFormat {

	static final String LS = System.lineSeparator();

	private MigrationChainFormat() {
	}

	static void formatElements(MigrationChain chain, StringBuilder target) {

		Map<String, List<String>> table = buildMigrationTable(chain);

		// Compute column widths
		int[] columnWidths = new int[table.size()];
		int columnm = 0;
		for (Map.Entry<String, List<String>> entry : table.entrySet()) {
			String k = entry.getKey();
			List<String> v = entry.getValue();
			columnWidths[columnm++] = Math.max(k.length(),
					v.stream().map(String::length).max(Integer::compareTo).orElse(0));
		}
		String[] columnFormats = Arrays.stream(columnWidths).mapToObj(w -> "| %-" + w + "s ").toArray(String[]::new);

		// Build separator
		String separator = buildSeparator(columnWidths);

		// Build rows
		Map<Integer, StringBuilder> rows = new TreeMap<>();
		int numRows = chain.getElements().size();
		columnm = 0;
		for (List<String> v : table.values()) {
			for (int i = 0; i < numRows; ++i) {
				StringBuilder row = rows.computeIfAbsent(i, k -> new StringBuilder());
				row.append(String.format(columnFormats[columnm], v.get(i)));
			}
			++columnm;
		}

		// Stitch everything together
		target.append(separator);
		columnm = 0;
		for (String column : table.keySet()) {
			target.append(String.format(columnFormats[columnm++], column));
		}
		target.append("|").append(LS).append(separator);
		rows.values().forEach(row -> target.append(row).append("|").append(LS));
		target.append(separator);
	}

	private static Map<String, List<String>> buildMigrationTable(MigrationChain chain) {

		Map<String, List<String>> table = new LinkedHashMap<>();
		for (MigrationChain.Element element : chain.getElements()) {

			List<String> column;

			column = table.computeIfAbsent("Version", k -> new ArrayList<>());
			column.add(element.getVersion());

			column = table.computeIfAbsent("Description", k -> new ArrayList<>());
			column.add(element.getOptionalDescription().orElse(""));

			column = table.computeIfAbsent("Type", k -> new ArrayList<>());
			column.add(element.getType().name());

			column = table.computeIfAbsent("Installed on", k -> new ArrayList<>());
			column.add(element.getInstalledOn().map(ZonedDateTime::toString).orElse(""));

			column = table.computeIfAbsent("by", k -> new ArrayList<>());
			column.add(element.getInstalledBy().orElse(""));

			column = table.computeIfAbsent("Execution time", k -> new ArrayList<>());
			column.add(element.getExecutionTime().map(Duration::toString).orElse(""));

			column = table.computeIfAbsent("State", k -> new ArrayList<>());
			column.add(element.getState().name());

			column = table.computeIfAbsent("Source", k -> new ArrayList<>());
			column.add(element.getSource());
		}
		return table;
	}

	private static String buildSeparator(int[] columnWidhts) {

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < columnWidhts.length; ++i) {
			sb.append("+");
			for (int j = 0; j < columnWidhts[i] + 2; ++j) {
				sb.append("-");
			}
		}
		return sb.append("+").append(LS).toString();
	}

}
