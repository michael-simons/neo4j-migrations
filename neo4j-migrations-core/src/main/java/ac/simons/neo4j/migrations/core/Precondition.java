package ac.simons.neo4j.migrations.core;

interface Precondition {

	enum Type {
		ASSUMPTION,
		ASSERTION
	}

	static Precondition parse(String in) {

		return null;
	}

	boolean isSatisfied(MigrationContext migrationContext);

	Type getType();
}
