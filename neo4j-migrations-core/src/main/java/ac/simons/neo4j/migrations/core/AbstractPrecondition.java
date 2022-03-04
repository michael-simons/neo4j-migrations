package ac.simons.neo4j.migrations.core;

abstract class AbstractPrecondition implements Precondition {

	private final Type type;

	AbstractPrecondition(Type type) {
		this.type = type;
	}

	@Override
	public final Type getType() {
		return type;
	}
}
