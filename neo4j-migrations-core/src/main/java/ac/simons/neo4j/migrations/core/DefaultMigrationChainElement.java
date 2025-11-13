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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;

/**
 * Only implementation of a {@link MigrationChain.Element}.
 *
 * @author Michael J. Simons
 * @since 2.0.0
 */
final class DefaultMigrationChainElement implements MigrationChain.Element {

	private final MigrationState state;

	private final MigrationType type;

	private final String checksum;

	private final String version;

	private final String description;

	private final String source;

	private final InstallationInfo installationInfo;

	private DefaultMigrationChainElement(MigrationState state, MigrationType type, String checksum, String version,
			String description, String source, InstallationInfo installationInfo) {
		this.state = state;
		this.type = type;
		this.checksum = checksum;
		this.version = version;
		this.description = description;
		this.source = source;
		this.installationInfo = installationInfo;
	}

	static MigrationChain.Element appliedElement(Path.Segment appliedMigration, List<Relationship> repetitions) {

		Node targetMigration = appliedMigration.end();
		Map<String, Object> properties = targetMigration.asMap();

		Relationship migrationProperties = repetitions.stream()
			.filter(relationship -> relationship.endNodeElementId().equals(targetMigration.elementId()))
			.max(Comparator.comparing((Relationship r) -> r.get("at").asZonedDateTime()))
			.orElse(appliedMigration.relationship());

		ZonedDateTime installedOn = migrationProperties.get("at").asZonedDateTime();
		String installedBy = String.format("%s/%s", migrationProperties.get("by").asString(),
				migrationProperties.get("connectedAs").asString());
		IsoDuration storedExecutionTime = migrationProperties.get("in").asIsoDuration();
		Duration executionTime = Duration.ofSeconds(storedExecutionTime.seconds())
			.plusNanos(storedExecutionTime.nanoseconds());

		return new DefaultMigrationChainElement(MigrationState.APPLIED,
				MigrationType.valueOf((String) properties.get("type")),
				migrationProperties.get("checksum").asString((String) properties.get("checksum")),
				(String) properties.get("version"), (String) properties.get("description"),
				(String) properties.get("source"), new InstallationInfo(installedOn, installedBy, executionTime));
	}

	static MigrationChain.Element pendingElement(Migration pendingMigration) {
		return new DefaultMigrationChainElement(MigrationState.PENDING, Migrations.getMigrationType(pendingMigration),
				pendingMigration.getChecksum().orElse(null), pendingMigration.getVersion().getValue(),
				pendingMigration.getOptionalDescription().orElse(null), pendingMigration.getSource(), null);
	}

	@Override
	public MigrationState getState() {
		return this.state;
	}

	@Override
	public MigrationType getType() {
		return this.type;
	}

	@Override
	public Optional<String> getChecksum() {
		return Optional.ofNullable(this.checksum);
	}

	@Override
	public String getVersion() {
		return this.version;
	}

	@Override
	public Optional<String> getOptionalDescription() {
		return Optional.ofNullable(this.description);
	}

	@Override
	public String getSource() {
		return this.source;
	}

	@Override
	public Optional<ZonedDateTime> getInstalledOn() {
		return Optional.ofNullable(this.installationInfo).map(InstallationInfo::installedOn);
	}

	@Override
	public Optional<String> getInstalledBy() {
		return Optional.ofNullable(this.installationInfo).map(InstallationInfo::installedBy);
	}

	@Override
	public Optional<Duration> getExecutionTime() {
		return Optional.ofNullable(this.installationInfo).map(InstallationInfo::executionTime);
	}

	@Override
	public String toString() {
		return "DefaultMigrationChainElement{" + "type=" + this.type + ", checksum='" + this.checksum + '\''
				+ ", version='" + this.version + '\'' + '}';
	}

	record InstallationInfo(ZonedDateTime installedOn, String installedBy, Duration executionTime) {
	}

}
