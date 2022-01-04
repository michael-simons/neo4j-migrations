/**
 * The runtime infrastructure for configuring and creating instances of {@link ac.simons.neo4j.migrations.core.Migrations}.
 * We don't give any guarantees for API stability in this package. We do guarantee a stable behaviour however:
 * <ul>
 *     <li>Configuration properties life under {@literal org.neo4.migrations}.</li>
 *     <li>Configuration properties will stick to their defined phases at least in minor version changes.</li>
 *     <li>The runtime will always create a synthetic build item of type {@link ac.simons.neo4j.migrations.core.Migrations}.</li>
 *     <li>The runtime will always create a synthetic build item of type {@link ac.simons.neo4j.migrations.quarkus.runtime.MigrationsEnabled}.</li>
 * </ul>
 *
 * @author Michael J. Simons
 */
package ac.simons.neo4j.migrations.quarkus.runtime;
