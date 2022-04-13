package ac.simons.neo4j.migrations.core.catalog;

/**
 * An item in the catalog (of either a single migration or the whole context with the merged catalog).
 *
 * @author Michael J. Simons
 * @soundtrack Anthrax - Spreading The Disease
 * @since TBA
 */
interface CatalogItem {

	/**
	 * @return A unique identifier for a catalog item.
	 */
	String getId();
}
