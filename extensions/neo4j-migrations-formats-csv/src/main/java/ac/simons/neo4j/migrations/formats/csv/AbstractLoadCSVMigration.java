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
package ac.simons.neo4j.migrations.formats.csv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import ac.simons.neo4j.migrations.core.JavaBasedMigration;
import ac.simons.neo4j.migrations.core.MigrationContext;
import ac.simons.neo4j.migrations.core.Migrations;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;

/**
 * This base class can be inherited inside applications that want to grab some data from a
 * URI for example to trigger a {@code LOAD CSV} statement inside Neo4j. Migrations
 * inheriting from it will always be repeatable (when adhering to the repeatable
 * versioning scheme, such as {@code R000__LoadBaseData.java}) but also compute a CRC32
 * checksum from the source URI, so that the migration is only repeated when the source
 * actually has changed.
 *
 * @author Michael J. Simons
 * @since 2.0.1
 */
public abstract class AbstractLoadCSVMigration implements JavaBasedMigration {

	static final Logger LOGGER = Logger.getLogger(AbstractLoadCSVMigration.class.getName());

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

	private final URI csvSource;

	private final boolean repeatable;

	@SuppressWarnings({ "OptionalUsedAsFieldOrParameterType", "squid:S3077" })
	private volatile Optional<String> checksum;

	/**
	 * You need to call this constructor, but your implementation must have a default,
	 * no-arg constructor to be loadable like any other Java based migration.
	 * @param csvSource the source to load data from
	 * @param repeatable whether this is repeatable or not
	 */
	protected AbstractLoadCSVMigration(URI csvSource, boolean repeatable) {
		this.csvSource = csvSource;
		this.repeatable = repeatable;
	}

	@Override
	public final void apply(MigrationContext context) {

		var originalQuery = getQuery();
		try (Session session = context.getSession()) {
			var summary = session
				.run(new Query(originalQuery.text().formatted(this.csvSource), originalQuery.parameters()))
				.consume();
			LOGGER.log(Level.FINE,
					() -> String.format("Loaded CSV from %s resulting in %s", this.csvSource, summary.counters()));
		}
	}

	/**
	 * The statement returned by this method should have exactly one Java format specifier
	 * {@code %s} which will be used by us to insert the result of {@link #csvSource}. Any
	 * other parameters included with the query object will just be used as is.
	 * @return the Cypher statement to be used to load the CSV file.
	 */
	public abstract Query getQuery();

	/**
	 * Overwrite this method and apply all customization to the {@link HttpRequest.Builder
	 * builder} that you might need, such as authentication, additional headers and
	 * cookies. Everybody loves cookies.
	 * @param builder pre-initialized with the target source
	 * @return the usable request
	 */
	public HttpRequest customizeRequest(HttpRequest.Builder builder) {
		return builder.build();
	}

	@Override
	public final boolean isRepeatable() {
		return this.repeatable;
	}

	// Having the value in a lazily initialized optional is the point here.
	@SuppressWarnings({ "OptionalAssignedToNull", "squid:S2789" })
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
			var request = customizeRequest(
					HttpRequest.newBuilder(this.csvSource).header("User-Agent", Migrations.getUserAgent()).GET());
			this.httpClient.send(request, HttpResponse.BodyHandlers
				.ofByteArrayConsumer(optionalBytes -> optionalBytes.ifPresent(crc32::update)));
			return Long.toString(crc32.getValue());
		}
		catch (IOException | InterruptedException ex) {
			LOGGER.log(Level.WARNING, ex,
					() -> String.format(
							"Could not retrieve %s, checksum won't be available until next migration attempt.",
							this.csvSource));
			if (ex instanceof InterruptedException) {
				// Restore interrupted state...
				Thread.currentThread().interrupt();
			}
			return null;
		}
	}

}
