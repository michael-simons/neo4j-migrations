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
package ac.simons.neo4j.migrations.core.templates;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import org.neo4j.driver.Query;
import org.neo4j.driver.Session;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.Migrations;

/**
 * This base class can be inherited inside applications that want to grab some data from a URI for example to trigger
 * a {@code LOAD CSV} statement inside Neo4j. Migrations inheriting from it will always be repeatable (when adhering to
 * the repeatable versioning scheme, such as {@code R000__LoadBaseData.java}) but also compute a CRC32 checksum from the
 * source URI, so that the migration is only repeated when the source actually has changed.
 *
 * @author Michael J. Simons
 * @since 2.0.1
 */
@SuppressWarnings("missing-explicit-ctor")
public abstract class RepeatableLoadCSVMigration implements JavaBasedMigration {

	static final Logger LOGGER = Logger.getLogger(RepeatableLoadCSVMigration.class.getName());

	private final HttpClient httpClient = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private volatile Optional<String> checksum;

	public RepeatableLoadCSVMigration() {
	}

	@Override
	public final void apply(MigrationContext context) {

		var originalQuery = getQuery();
		try (Session session = context.getSession()) {
			var summary = session
				.run(new Query(originalQuery.text().formatted(getCSVSource()), originalQuery.parameters()))
				.consume();
			LOGGER.log(Level.FINE, () -> String.format("Loaded CSV from %s resulting in %s", getCSVSource(), summary.counters()));
		}
	}

	/**
	 * @return The URI pointing to the source of the CSV file.
	 */
	public abstract URI getCSVSource();

	/**
	 * The statement returned by this method should have exactly one Java format specifier {@code %s} which will be used
	 * by us to insert the result of {@link #getCSVSource()}. Any other parameters included with the query object will just
	 * be used as is.
	 *
	 * @return The Cypher statement to be used to load the CSV file.
	 */
	public abstract Query getQuery();

	/**
	 * Overwrite this method and apply all customization to the {@link HttpRequest.Builder builder} that you might need,
	 * such as authentication, additional headers and cookies. Everybody loves cookies.
	 *
	 * @param builder Pre-initialized with the target source
	 * @return The usable request
	 */
	public HttpRequest customizeRequest(HttpRequest.Builder builder) {
		return builder.build();
	}

	@Override
	public final boolean isRepeatable() {
		return true;
	}

	@SuppressWarnings("OptionalAssignedToNull")
	@Override
	public final Optional<String> getChecksum() {
		Optional<String> availableChecksum = this.checksum;
		if (availableChecksum == null) {
			synchronized (this) {
				availableChecksum = this.checksum;
				if (availableChecksum == null) {
					this.checksum = Optional.ofNullable(computeChecksum());
					availableChecksum = this.checksum;
				}
			}
		}
		return availableChecksum;
	}

	private String computeChecksum() {
		try {
			var crc32 = new CRC32();
			var request = customizeRequest(HttpRequest
				.newBuilder(getCSVSource())
				.header("User-Agent", Migrations.getUserAgent())
				.GET());
			httpClient.send(request, HttpResponse.BodyHandlers.ofByteArrayConsumer(optionalBytes -> optionalBytes.ifPresent(crc32::update)));
			return Long.toString(crc32.getValue());
		} catch (IOException | InterruptedException e) {
			LOGGER.log(Level.WARNING, e, () -> String.format("Could not retrieve %s, checksum won't be available until next migration attempt.", getCSVSource()));
			return null;
		}
	}
}
