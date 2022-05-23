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
package ac.simons.neo4j.migrations.core;

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogDiff;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.catalog.Operator;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;
import ac.simons.neo4j.migrations.core.internal.NodeSetDataImpl;
import ac.simons.neo4j.migrations.core.internal.NoopDOMCryptoContext;
import ac.simons.neo4j.migrations.core.internal.ThrowingErrorHandler;
import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import javax.xml.XMLConstants;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.summary.SummaryCounters;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A migration based on a catalog. The migration itself can contain a (partial) catalog with items that will be added
 * to the {@link MigrationContext Migration contexts} global catalog. Items with the same ids in newer migrations will
 * be added to the catalog. They will be picked up by operations depending on which migration the operation is applied.
 *
 * @author Michael J. Simons
 * @soundtrack Tom Holkenborg - Terminator: Dark Fate
 * @since TBA
 */
final class CatalogBasedMigration implements Migration {

	private static final Logger LOGGER = Logger.getLogger(CatalogBasedMigration.class.getName());

	/**
	 * A reference to the schema for validating our input.
	 */
	private static final Schema MIGRATION_SCHEMA;
	/**
	 * Neither document builder factories nor document builders are thread safe, so here we are…
	 */
	private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY;

	static {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			MIGRATION_SCHEMA = schemaFactory.newSchema(new StreamSource(
				CatalogBasedMigration.class.getResourceAsStream("/ac/simons/neo4j/migrations/core/migration.xsd")));
		} catch (SAXException e) {
			throw new MigrationsException("Could not load XML schema definition for schema based migrations.", e);
		}

		DOCUMENT_BUILDER_FACTORY = ThreadLocal.withInitial(() -> {
			DocumentBuilderFactory value = DocumentBuilderFactory.newInstance();
			value.setSchema(MIGRATION_SCHEMA);
			value.setExpandEntityReferences(false);
			value.setNamespaceAware(true);
			return value;
		});
	}

	private static String computeChecksum(Document document) {

		final NodeList allElements = document.getElementsByTagName("*");

		Node newCatalog = document.createElement(XMLSchemaConstants.CATALOG);
		Node oldCatalog = null;
		Node constraints = null;
		Node indexes = null;

		final List<Node> elements = new ArrayList<>();
		for (int i = 0; i < allElements.getLength(); i++) {
			Node currentItem = allElements.item(i);

			if (currentItem.getLocalName().equals(XMLSchemaConstants.CATALOG)) {
				oldCatalog = currentItem;
				continue;
			}
			if (currentItem.getLocalName().equals(XMLSchemaConstants.INDEXES)) {
				indexes = currentItem;
			} else if (currentItem.getLocalName().equals(XMLSchemaConstants.CONSTRAINTS)) {
				constraints = currentItem;
			}
			elements.add(currentItem);
			NodeList childNodes = currentItem.getChildNodes();
			for (int j = 0; j < childNodes.getLength(); ++j) {
				Node childItem = childNodes.item(j);
				if (!(childItem instanceof CharacterData)) {
					continue;
				}

				CharacterData textNode = (CharacterData) childItem;
				if (!textNode.getTextContent().trim().isEmpty()) {

					String content = Arrays
						.stream(textNode.getTextContent().split("\r?\n"))
						.map(String::trim).collect(Collectors.joining("\n"));
					textNode.setData(content);
					elements.add(textNode);
				}
			}
		}

		if (oldCatalog != null) {
			oldCatalog.getParentNode().replaceChild(newCatalog, oldCatalog);
			if (constraints != null) {
				newCatalog.appendChild(constraints);
			}
			if (indexes != null) {
				newCatalog.appendChild(indexes);
			}
		}
		elements.add(newCatalog);
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			XMLCryptoContext cryptoContext = new NoopDOMCryptoContext();
			TransformService transformService = TransformService.getInstance(CanonicalizationMethod.INCLUSIVE, "DOM");
			transformService.init(new DOMStructure(document.createElement("holder")), cryptoContext);
			transformService.transform(NodeSetDataImpl.of(elements), cryptoContext, os);

			os.flush();

			final CRC32 crc32 = new CRC32();
			byte[] bytes = os.toByteArray();
			crc32.update(bytes, 0, bytes.length);
			return Long.toString(crc32.getValue());
		} catch (TransformException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | IOException e) {
			throw new MigrationsException("Could not canonicalize an xml document", e);
		}
	}

	static Migration from(URL url) {

		String path = url.getPath();
		try {
			path = URLDecoder.decode(path, Defaults.CYPHER_SCRIPT_ENCODING.name());
		} catch (UnsupportedEncodingException e) {
			throw new MigrationsException("Somethings broken: UTF-8 encoding not supported.");
		}
		int lastIndexOf = path.lastIndexOf("/");
		String fileName = lastIndexOf < 0 ? path : path.substring(lastIndexOf + 1);
		MigrationVersion version = MigrationVersion.parse(fileName);

		try (InputStream source = url.openStream()) {
			DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
			documentBuilder.setErrorHandler(new ThrowingErrorHandler());
			Document document = documentBuilder.parse(source);

			document.normalizeDocument();

			return new CatalogBasedMigration(fileName, version, computeChecksum(document), Catalog.of(document));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new MigrationsException("Could not parse the given document.", e);
		}
	}

	private final String source;

	private final MigrationVersion version;

	private final String checksum;

	private final Catalog catalog;

	private CatalogBasedMigration(String source, MigrationVersion version, String checksum, Catalog catalog) {
		this.source = source;
		this.version = version;
		this.checksum = checksum;
		this.catalog = catalog;
	}

	@Override
	public Optional<String> getChecksum() {
		return Optional.of(checksum);
	}

	@Override
	public MigrationVersion getVersion() {
		return version;
	}

	@Override
	public String getDescription() {
		return version.getDescription();
	}

	@Override
	public String getSource() {
		return source;
	}

	Catalog getCatalog() {
		return catalog;
	}

	@Override
	public void apply(MigrationContext context) {

		throw new UnsupportedOperationException("Not there yet");
	}

	static class OperationContext {

		final Neo4jVersion version;

		final Neo4jEdition edition;

		OperationContext(Neo4jVersion version, Neo4jEdition edition) {
			this.version = version;
			this.edition = edition;
		}
	}

	/**
	 * Something that can be executed from withing a catalog based migration.
	 */
	interface Operation {

		static <T extends Operation> BuilderWithCatalog<T> use(VersionedCatalog catalog) {
			return new DefaultOperationBuilder<>(catalog);
		}

		void apply(OperationContext context, QueryRunner queryRunner);
	}

	/**
	 * An operation that creates an item from the catalog inside the database.
	 */
	interface CreateOperation extends Operation {
	}

	/**
	 * An operation that drops an item from the catalog from the database.
	 */
	interface DropOperation extends Operation {
	}

	/**
	 * This operation loads all supported item types from the database, drops them and then creates all items of the local catalog.
	 */
	interface ApplyOperation extends Operation {
	}

	/**
	 * This operation takes the current catalog and checks whether all items in the version second to last are
	 * defined in the same or equivalent fashion in the database or if both this catalog or the database are empty. The
	 * assertion is done before the most recent version so that - if necessary - all create and drop operations can be
	 * safely applied. Thus, you can even assert an empty catalog. This behaviour can be switched to using the current
	 * version by using the appropriate argument to builder method.
	 */
	interface VerifyOperation extends Operation {
	}

	/**
	 * Entry point for getting hold of operations.
	 *
	 * @param <T> The type of operation to build
	 */
	interface BuilderWithCatalog<T extends Operation> {

		/**
		 * Creates a new drop operation
		 *
		 * @param name    The name of the item to drop
		 * @param ifExits should it be an idempotent operation or not?
		 * @return Ongoing definition
		 */
		BuilderWithTargetItem<T> drop(Name name, boolean ifExits);

		/**
		 * Creates a new create operation
		 *
		 * @param name        The name of the item to create
		 * @param ifNotExists should it be an idempotent operation or not?
		 * @return Ongoing definition
		 */
		BuilderWithTargetItem<T> create(Name name, boolean ifNotExists);

		/**
		 * Creates a new {@link ApplyOperation}. This operation is potentially destructive. It will load all supported
		 * item types from the database, drop them and eventually create the content of the catalog.
		 *
		 * @return The operation ready to apply.
		 */
		ApplyOperation apply();

		/**
		 * Create a new {@link VerifyOperation}.
		 *
		 * @param useCurrent Use {@literal true} to verify / assert the current version, use {@literal false} to verify the previous.
		 * @return The operation ready to apply.
		 */
		VerifyBuilder verify(boolean useCurrent);
	}

	/**
	 * Specifies the version in which the item that is dealt with has been reference
	 *
	 * @param <T> The type of operation to build
	 */
	interface BuilderWithTargetItem<T extends Operation> {

		T with(MigrationVersion version);
	}

	/**
	 * Specifies the version at which verification should take place.
	 */
	interface TerminalVerifyBuilder {
		VerifyOperation at(MigrationVersion version);

	}

	/**
	 * Allows to configure whether equivalent but not identical catalogs are allowed
	 */
	interface VerifyBuilder extends TerminalVerifyBuilder {

		TerminalVerifyBuilder allowEquivalent(boolean allowEquivalent);
	}

	private static class DefaultOperationBuilder<T extends Operation>
		implements BuilderWithCatalog<T>, BuilderWithTargetItem<T>, VerifyBuilder {

		private final VersionedCatalog catalog;

		private Operator operator;

		private Name reference;

		private boolean idempotent;

		private boolean useCurrent;

		private boolean allowEquivalent = true;

		DefaultOperationBuilder(VersionedCatalog catalog) {
			this.catalog = catalog;
		}

		@SuppressWarnings({ "HiddenField" })
		@Override
		public BuilderWithTargetItem<T> drop(Name reference, boolean ifExits) {

			this.operator = Operator.DROP;
			this.reference = reference;
			this.idempotent = ifExits;
			return this;
		}

		@SuppressWarnings({ "HiddenField" })
		@Override
		public BuilderWithTargetItem<T> create(Name reference, boolean ifNotExists) {

			this.operator = Operator.CREATE;
			this.reference = reference;
			this.idempotent = ifNotExists;
			return this;
		}

		@Override
		public ApplyOperation apply() {
			return new DefaultApplyOperation(catalog);
		}

		@SuppressWarnings({ "HiddenField" })
		@Override
		public VerifyBuilder verify(boolean useCurrent) {

			this.useCurrent = useCurrent;
			return this;
		}

		@SuppressWarnings({ "HiddenField" })
		@Override
		public TerminalVerifyBuilder allowEquivalent(boolean allowEquivalent) {

			this.allowEquivalent = allowEquivalent;
			return this;
		}

		@Override
		public VerifyOperation at(MigrationVersion version) {
			return new DefaultVerifyOperation(catalog, useCurrent, allowEquivalent, version);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T with(MigrationVersion version) {

			switch (this.operator) {
				case DROP:
					return (T) new DefaultDropOperation(version, reference, idempotent, catalog);
				case CREATE:
					return (T) new DefaultCreateOperation(version, reference, idempotent, catalog);
			}
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Some state for all operations working on a specific item defined by a named reference.
	 */
	private static abstract class AbstractItemBasedOperation implements Operation {

		protected final MigrationVersion definedAt;

		protected final Name reference;

		protected final boolean idempotent;

		protected final VersionedCatalog catalog;

		AbstractItemBasedOperation(MigrationVersion definedAt, Name reference, boolean idempotent,
			VersionedCatalog catalog) {
			this.definedAt = definedAt;
			this.reference = reference;
			this.idempotent = idempotent;
			this.catalog = catalog;
		}

		CatalogItem<?> getRequiredItem() {
			return catalog.getItem(reference, definedAt).orElseThrow(() -> new MigrationsException(
				String.format("An item named '%s' has not been defined as of version %s.", reference.getValue(),
					definedAt.getValue())));
		}
	}

	/**
	 * Executes creates.
	 */
	static final class DefaultCreateOperation extends AbstractItemBasedOperation implements CreateOperation {

		DefaultCreateOperation(MigrationVersion definedAt, Name reference, boolean idempotent,
			VersionedCatalog catalog) {
			super(definedAt, reference, idempotent, catalog);
		}

		@Override
		public void apply(OperationContext context, QueryRunner queryRunner) {

			CatalogItem<?> item = getRequiredItem();
			Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
			RenderConfig config = RenderConfig.create()
				.idempotent(idempotent)
				.forVersionAndEdition(context.version, context.edition);

			if (idempotent && !context.version.hasIdempotentOperations()) {
				config = config.ignoreName();
				createIfNotExists(context, item, queryRunner, renderer, config);
			} else {
				queryRunner.run(renderer.render(item, config)).consume();
			}
		}

		private void createIfNotExists(OperationContext context, CatalogItem<?> item, QueryRunner queryRunner,
			Renderer<CatalogItem<?>> renderer, RenderConfig config) {

			try {
				queryRunner.run(renderer.render(item, config)).consume();
			} catch (Neo4jException e) {
				// Directly throw anything that can't match
				if (!Neo4jCodes.CODES_FOR_EXISTING_CONSTRAINT.contains(e.code())) {
					throw e;
				}
				// Make sure the thing actually is there.
				List<Constraint> constraints = queryRunner.run(context.version.getShowConstraints())
					.list(Constraint::parse);

				// If there are no constraints there at all, something fishy is going on for sure
				// otherwise, there must now an equivalent version of it
				if (constraints.isEmpty() || constraints.stream()
					.noneMatch(existingConstraint -> existingConstraint.isEquivalentTo(item))) {
					throw e;
				}
			}
		}
	}

	/**
	 * Executes drops.
	 */
	static final class DefaultDropOperation extends AbstractItemBasedOperation implements DropOperation {

		DefaultDropOperation(MigrationVersion definedAt, Name reference, boolean idempotent, VersionedCatalog catalog) {
			super(definedAt, reference, idempotent, catalog);
		}

		@Override
		public void apply(OperationContext context, QueryRunner queryRunner) {

			CatalogItem<?> item = getRequiredItem();
			Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
			RenderConfig config = RenderConfig.drop()
				.idempotent(idempotent)
				.forVersionAndEdition(context.version, context.edition);

			if (idempotent && !context.version.hasIdempotentOperations()) {
				config = config.ignoreName();
				drop(context, item, queryRunner, renderer, config, true);
			} else {
				queryRunner.run(renderer.render(item, config)).consume();
			}
		}

		private void drop(OperationContext context, CatalogItem<?> item, QueryRunner queryRunner,
			Renderer<CatalogItem<?>> renderer,
			RenderConfig config, boolean fallbackToPrior) {

			try {
				queryRunner.run(renderer.render(item, config)).consume();
			} catch (Neo4jException e) {
				// Directly throw anything that can't match
				if (!Neo4jCodes.CONSTRAINT_DROP_FAILED.equals(e.code())) {
					throw e;
				}
				// Make sure the thing actually not there.
				List<Constraint> constraints = queryRunner.run(context.version.getShowConstraints())
					.list(Constraint::parse);

				// Let's skip all the hard work directly
				if (constraints.isEmpty()) {
					return;
				}

				// Directly throw if it is still there
				if (constraints.stream().anyMatch(existingConstraint -> existingConstraint.isEquivalentTo(item))) {
					throw e;
				}

				if (!fallbackToPrior) {
					return;
				}

				// If it has been defined in an older version users might have redefined it in this version,
				// such that couldn't have been dropped
				catalog.getItemPriorTo(reference, definedAt)
					.filter(
						v -> constraints.stream().anyMatch(existingConstraint -> existingConstraint.isEquivalentTo(v)))
					.ifPresent(olderItem -> drop(context, olderItem, queryRunner, renderer, config, false));
			}
		}
	}

	/**
	 * Default implementation of verification.
	 */
	static final class DefaultVerifyOperation implements VerifyOperation {

		private final VersionedCatalog currentCatalog;
		private final boolean useCurrent;
		private final boolean allowEquivalent;
		private final MigrationVersion currentVersion;

		DefaultVerifyOperation(VersionedCatalog currentCatalog, boolean useCurrent, boolean allowEquivalent,
			MigrationVersion currentVersion) {
			this.currentCatalog = currentCatalog;
			this.useCurrent = useCurrent;
			this.currentVersion = currentVersion;
			this.allowEquivalent = allowEquivalent;
		}

		@Override
		public void apply(OperationContext context, QueryRunner queryRunner) {

			// Get all the constraints
			Catalog databaseCatalog = DatabaseCatalog.of(context.version, queryRunner);

			CatalogDiff diff = CatalogDiff.between(databaseCatalog,
				useCurrent ?
					currentCatalog.getCatalogAt(currentVersion) :
					currentCatalog.getCatalogPriorTo(currentVersion));

			if (diff.identical()) {
				LOGGER.log(Level.FINE, "Database schema and catalog are identical.");
			} else if (diff.equivalent() && allowEquivalent) {
				if (LOGGER.isLoggable(Level.WARNING)) {

					StringBuilder message = new StringBuilder();
					Collection<CatalogItem<?>> itemsOnlyInRight = diff.getItemsOnlyInRight();
					message.append(
						"Items in the database are not identical to items in the schema catalog. The following items have different names but an equivalent definition:");
					diff.getItemsOnlyInLeft().forEach(item -> itemsOnlyInRight.stream()
						.filter(item::isEquivalentTo)
						.findFirst()
						.ifPresent(equivalentItem ->
							message
								.append(System.lineSeparator())
								.append("* Database item `")
								.append(item.getName().getValue())
								.append("` matches catalog item `")
								.append(equivalentItem.getName().getValue())
								.append("`")
						));

					LOGGER.log(Level.WARNING, message.toString());
				}
			} else {
				throw new MigrationsException(diff.equivalent() ?
					"Database schema and the catalog are equivalent but the verification requires them to be identical." :
					"Catalogs are neither identical nor equivalent.");
			}
		}
	}

	/**
	 * Drops everything from the database catalog, adds everything from the migrations catalog.
	 */
	static final class DefaultApplyOperation implements ApplyOperation {

		private final Catalog currentCatalog;

		DefaultApplyOperation(Catalog currentCatalog) {
			this.currentCatalog = currentCatalog;
		}

		@Override
		public void apply(OperationContext context, QueryRunner queryRunner) {

			// Get all the constraints
			Catalog databaseCatalog = DatabaseCatalog.of(context.version, queryRunner);

			// Make them go away
			RenderConfig dropConfig = RenderConfig.drop()
				.forVersionAndEdition(context.version, context.edition);
			AtomicInteger droppedCnt = new AtomicInteger(0);
			databaseCatalog.getItems().forEach(catalogItem -> {
				Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, catalogItem);
				SummaryCounters counters = queryRunner.run(renderer.render(catalogItem, dropConfig)).consume()
					.counters();
				droppedCnt.addAndGet(counters.constraintsRemoved());
				droppedCnt.addAndGet(counters.indexesRemoved());
			});

			// Add the new ones
			RenderConfig createConfig = RenderConfig.create()
				.forVersionAndEdition(context.version, context.edition);
			AtomicInteger addedCnt = new AtomicInteger(0);
			currentCatalog.getItems().forEach(item -> {
				Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
				SummaryCounters counters = queryRunner.run(renderer.render(item, createConfig)).consume().counters();
				addedCnt.addAndGet(counters.constraintsAdded());
				addedCnt.addAndGet(counters.indexesAdded());
			});

			LOGGER.log(Level.INFO,
				() -> String.format("Dropped %d items, added %d new items.", droppedCnt.get(), addedCnt.get()));
		}
	}
}
