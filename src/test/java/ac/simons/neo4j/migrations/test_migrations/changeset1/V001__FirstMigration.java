package ac.simons.neo4j.migrations.test_migrations.changeset1;

import ac.simons.neo4j.migrations.JavaBasedMigration;

import org.neo4j.driver.Driver;

public class V001__FirstMigration implements JavaBasedMigration {

	@Override
	public void apply(Driver driver) {
	}
}
