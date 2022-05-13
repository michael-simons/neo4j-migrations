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
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.catalog.Operator;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import javax.xml.XMLConstants;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMCryptoContext;
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
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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

	private static final Schema MIGRATION_SCHEMA;
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

	static class ThrowingErrorHandler implements ErrorHandler {

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			throw exception;
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			// We do check for names later on, so we ignore this id
			// https://wiki.xmldation.com/Support/Validator/cvc-id-1
			if (!exception.getMessage().startsWith("cvc-id.1")) {
				throw exception;
			}
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			throw exception;
		}

	}

	static class NoopDOMCryptoContext extends DOMCryptoContext {

	}

	static class NodeSetDataImpl implements NodeSetData {

		static NodeSetData of(List<Node> elements) {
			return new NodeSetDataImpl(elements);
		}

		private final List<Node> elements;

		private NodeSetDataImpl(List<Node> elements) {
			this.elements = elements;
		}

		@Override
		public Iterator<Node> iterator() {
			return this.elements.iterator();
		}

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
	}

	static class OperationContext {

		final Neo4jVersion version;

		final Neo4jEdition edition;

		OperationContext(Neo4jVersion version, Neo4jEdition edition) {
			this.version = version;
			this.edition = edition;
		}
	}

	interface Operation {

		static <T extends Operation> BuilderWithCatalog<T> use(VersionedCatalog catalog) {
			return new DefaultOperationBuilder<T>(catalog);
		}

		void apply(OperationContext context, QueryRunner queryRunner);
	}

	interface DropOperation extends Operation {
	}

	interface BuilderWithCatalog<T extends Operation> {

		BuilderWithTargetItem<DropOperation> drop(Name name, boolean ifExits);
	}

	interface BuilderWithTargetItem<T extends Operation> {

		T with(MigrationVersion version);
	}

	private static class DefaultOperationBuilder<T extends Operation>
		implements BuilderWithCatalog<T>, BuilderWithTargetItem<T> {

		private final VersionedCatalog catalog;

		private Operator operator;

		private Name reference;

		private boolean idempotent;

		DefaultOperationBuilder(VersionedCatalog catalog) {
			this.catalog = catalog;
		}

		@SuppressWarnings({ "unchecked", "HiddenField" })
		@Override
		public BuilderWithTargetItem<DropOperation> drop(Name reference, boolean idempotent) {

			this.operator = Operator.DROP;
			this.reference = reference;
			this.idempotent = idempotent;
			return (BuilderWithTargetItem<DropOperation>) this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T with(MigrationVersion version) {

			if (this.operator == Operator.DROP) {
				return (T) new DefaultDropOperation(version, reference, idempotent, catalog);
			}
			throw new UnsupportedOperationException();
		}
	}

	static class DefaultDropOperation implements DropOperation {

		private final MigrationVersion definedAt;

		private final Name reference;

		private final boolean idempotent;

		private final VersionedCatalog catalog;

		DefaultDropOperation(MigrationVersion definedAt, Name reference, boolean idempotent, VersionedCatalog catalog) {
			this.definedAt = definedAt;
			this.reference = reference;
			this.idempotent = idempotent;
			this.catalog = catalog;
		}

		@Override
		public void apply(OperationContext context, QueryRunner queryRunner) {

			CatalogItem<?> item = catalog.getItem(reference, definedAt).get();
			Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
			queryRunner.run(
				renderer.render(item, RenderConfig.drop().forVersionAndEdition(context.version, context.edition))
			);
		}
	}
}
