/*
 * Copyright 2020-2026 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
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

import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogDiff;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.Name;
import ac.simons.neo4j.migrations.core.catalog.Operator;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.internal.NodeSetDataImpl;
import ac.simons.neo4j.migrations.core.internal.NoopDOMCryptoContext;
import ac.simons.neo4j.migrations.core.internal.ThrowingErrorHandler;
import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;
import ac.simons.neo4j.migrations.core.refactorings.Counters;
import ac.simons.neo4j.migrations.core.refactorings.Refactoring;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.summary.SummaryCounters;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A migration based on a catalog. The migration itself can contain a (partial) catalog
 * with items that will be added to the {@link MigrationContext Migration contexts} global
 * catalog. Items with the same ids in newer migrations will be added to the catalog. They
 * will be picked up by operations depending on which migration the operation is applied.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
final class CatalogBasedMigration implements MigrationWithPreconditions {

	private static final Logger LOGGER = Logger.getLogger(CatalogBasedMigration.class.getName());

	/**
	 * A reference to the schema for validating our input.
	 */
	private static final Schema MIGRATION_SCHEMA;

	/**
	 * Neither document builder factories nor document builders are thread safe, so here
	 * we are… This is supposed to stay there, otherwise I can spare myself the effort of
	 * a thread local.
	 */
	@SuppressWarnings("squid:S5164")
	private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY;

	static {
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			MIGRATION_SCHEMA = schemaFactory.newSchema(new StreamSource(
					CatalogBasedMigration.class.getResourceAsStream("/ac/simons/neo4j/migrations/core/migration.xsd")));
		}
		catch (SAXException ex) {
			throw new MigrationsException("Could not load XML schema definition for schema based migrations.", ex);
		}

		DOCUMENT_BUILDER_FACTORY = ThreadLocal.withInitial(() -> {
			DocumentBuilderFactory value = DocumentBuilderFactory.newInstance();
			value.setSchema(MIGRATION_SCHEMA);
			value.setExpandEntityReferences(false);
			value.setNamespaceAware(true);
			return value;
		});
	}

	private final String source;

	private final MigrationVersion version;

	private final String checksum;

	private final Catalog catalog;

	private final List<Operation> operations;

	private final List<Precondition> preconditions;

	private final boolean resetCatalog;

	/**
	 * Allows for configuration of alternative checksums.
	 * @see CypherBasedMigration#getAlternativeChecksums() and field
	 */
	private List<String> alternativeChecksums = Collections.emptyList();

	private CatalogBasedMigration(String source, MigrationVersion version, String checksum, Catalog catalog,
			List<Operation> operations, List<Precondition> preconditions, boolean resetCatalog) {
		this.source = source;
		this.version = version;
		this.checksum = checksum;
		this.catalog = catalog;
		this.operations = operations;
		this.preconditions = preconditions;
		this.resetCatalog = resetCatalog;
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
			}
			else if (currentItem.getLocalName().equals(XMLSchemaConstants.CONSTRAINTS)) {
				constraints = currentItem;
			}
			elements.add(currentItem);
			NodeList childNodes = currentItem.getChildNodes();
			for (int j = 0; j < childNodes.getLength(); ++j) {
				Node childItem = childNodes.item(j);
				if (!(childItem instanceof CharacterData textNode) || textNode.getTextContent().trim().isEmpty()) {
					continue;
				}

				String content = Arrays.stream(textNode.getTextContent().split("\r?\n"))
					.map(String::trim)
					.collect(Collectors.joining("\n"));
				textNode.setData(content);
				elements.add(textNode);
			}
		}

		if (oldCatalog != null) {
			updateCatalog(oldCatalog, newCatalog);
		}
		if (constraints != null) {
			newCatalog.appendChild(constraints);
		}
		if (indexes != null) {
			newCatalog.appendChild(indexes);
		}
		elements.add(newCatalog);
		return canonicalizeAndChecksumElements(document, elements);
	}

	private static void updateCatalog(Node oldCatalog, Node newCatalog) {
		oldCatalog.getParentNode().replaceChild(newCatalog, oldCatalog);
		NamedNodeMap attributes = oldCatalog.getAttributes();
		for (int i = 0; i < attributes.getLength(); ++i) {
			Node attribute = attributes.item(i);
			attributes.removeNamedItem(attribute.getNodeName());
			newCatalog.getAttributes().setNamedItem(attribute);
		}
	}

	private static String canonicalizeAndChecksumElements(Document document, List<Node> elements) {
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
		}
		catch (TransformException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | IOException ex) {
			throw new MigrationsException("Could not canonicalize an xml document", ex);
		}
	}

	static Migration from(ResourceContext context) {

		var url = context.getUrl();
		String path = URLDecoder.decode(url.getPath(), Defaults.CYPHER_SCRIPT_ENCODING);
		int lastIndexOf = path.lastIndexOf("/");
		String fileName = (lastIndexOf < 0) ? path : path.substring(lastIndexOf + 1);
		MigrationVersion version = MigrationVersion.parse(fileName);

		Document document = parseDocument(context);
		return new CatalogBasedMigration(fileName, version, computeChecksum(document), Catalog.of(document),
				parseOperations(document, version), getPreconditions(document), isResetCatalog(document));
	}

	static Document parseDocument(ResourceContext context) {

		try (InputStream source = context.openStream()) {
			DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.get().newDocumentBuilder();
			documentBuilder.setErrorHandler(new ThrowingErrorHandler());
			Document document = documentBuilder.parse(source);

			document.normalizeDocument();

			return document;
		}
		catch (SAXParseException ex) {
			throw new MigrationsException("Could not parse migration: " + ex.getMessage());
		}
		catch (SAXException | IOException | ParserConfigurationException ex) {
			throw new MigrationsException("Could not parse the given document", ex);
		}
	}

	static boolean isResetCatalog(Document document) {

		NodeList catalog = document.getElementsByTagName(XMLSchemaConstants.CATALOG);
		return catalog.getLength() == 1
				&& Boolean.parseBoolean(((Element) catalog.item(0)).getAttribute(XMLSchemaConstants.RESET));
	}

	static List<Precondition> getPreconditions(Node parentNode) {
		List<Precondition> result = new ArrayList<>();
		NodeList childNodes = parentNode.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); ++i) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				Precondition.parse(String.format("// %s %s", node.getNodeName(), node.getTextContent().trim()))
					.ifPresent(result::add);
			}
			else if (node.getNodeType() == Node.ELEMENT_NODE) {
				result.addAll(getPreconditions(node));
			}
		}
		return result;
	}

	static List<Operation> parseOperations(Document document, MigrationVersion version) {

		List<Operation> result = new ArrayList<>();

		// We read the elements as they come, as there is no way to say "give me all
		// elements of a given type"
		NodeList migration = document.getElementsByTagName("migration");
		if (migration.getLength() != 1) {
			throw new MigrationsException("Invalid document: No <migration /> element.");
		}
		NodeList childNodes = migration.item(0).getChildNodes();
		for (int i = 0; i < childNodes.getLength(); ++i) {
			Node node = childNodes.item(i);
			String nodeName = node.getNodeName();
			if (!((node instanceof Element) && XMLSchemaConstants.SUPPORTED_OPERATIONS.contains(nodeName))) {
				LOGGER.fine(() -> String.format("Skipping node: %s", nodeName));
				continue;
			}
			if (XMLSchemaConstants.REFACTOR.equals(nodeName)) {
				result.add(Operation.refactorWith(CatalogBasedRefactorings.fromNode(node)));
			}
			else {
				OperationType type = OperationType.valueOf(nodeName.toUpperCase(Locale.ROOT));
				result.add(type.build((Element) node, version));
			}
		}

		Comparator<CatalogItem<?>> catalogItemComparator = CatalogBasedMigration::compareCatalogItems;
		return result.stream().sorted((operation1, operation2) -> {
			if (operation1 instanceof ItemSpecificOperation isop1 && operation2 instanceof ItemSpecificOperation isop2
					&& isop1.getLocalItem().isPresent() && isop2.getLocalItem().isPresent()) {
				CatalogItem<?> item1 = isop1.getLocalItem().get();
				CatalogItem<?> item2 = isop2.getLocalItem().get();
				return catalogItemComparator.compare(item1, item2);
			}
			return 0;
		}).toList();
	}

	/**
	 * Sorts operations by type (constraints before indexes).
	 * @param o1 first item to compare
	 * @param o2 second item to compare
	 * @return result of comparison
	 */
	private static int compareCatalogItems(CatalogItem<?> o1, CatalogItem<?> o2) {
		if (o1 instanceof Constraint && o2 instanceof Index) {
			return -1;
		}
		else if (o2 instanceof Constraint && o1 instanceof Index) {
			return 1;
		}
		return 0;
	}

	static Counters schemaCounters(SummaryCounters summaryCounters) {

		Map<String, Integer> schema = Map.of("indexesAdded", summaryCounters.indexesAdded(), "indexesRemoved",
				summaryCounters.indexesRemoved(), "constraintsAdded", summaryCounters.constraintsAdded(),
				"constraintsRemoved", summaryCounters.constraintsRemoved());
		return Counters.of(schema);
	}

	static Counters schemaCounters(int indexesAdded, int indexesRemoved, int constraintsAdded, int constraintsRemoved) {

		Map<String, Integer> schema = Map.of("indexesAdded", indexesAdded, "indexesRemoved", indexesRemoved,
				"constraintsAdded", constraintsAdded, "constraintsRemoved", constraintsRemoved);
		return Counters.of(schema);
	}

	@Override
	public Optional<String> getChecksum() {
		return Optional.of(this.checksum);
	}

	@Override
	public List<String> getAlternativeChecksums() {
		return Collections.unmodifiableList(this.alternativeChecksums);
	}

	@Override
	public void setAlternativeChecksums(List<String> alternativeChecksums) {

		Objects.requireNonNull(alternativeChecksums);
		this.alternativeChecksums = new ArrayList<>(alternativeChecksums);
	}

	@Override
	public MigrationVersion getVersion() {
		return this.version;
	}

	@Override
	public Optional<String> getOptionalDescription() {
		return this.version.getOptionalDescription();
	}

	@Override
	public String getSource() {
		return this.source;
	}

	Catalog getCatalog() {
		return this.catalog;
	}

	boolean isResetCatalog() {
		return this.resetCatalog;
	}

	@Override
	public void apply(MigrationContext context) {

		Neo4jVersion neo4jVersion = Neo4jVersion.of(context.getConnectionDetails().getServerVersion());
		Neo4jEdition neo4jEdition = Neo4jEdition.of(context.getConnectionDetails().getServerEdition());

		Catalog globalCatalog = context.getCatalog();
		if (!(globalCatalog instanceof VersionedCatalog)) {
			throw new MigrationsException("Cannot use catalog based migrations without a versioned catalog.");
		}

		try {
			OperationContext operationContext = new OperationContext(neo4jVersion, neo4jEdition,
					(VersionedCatalog) globalCatalog, context.getConfig(), context::getSession);

			Counters counters = this.operations.stream()
				.map(op -> op.execute(operationContext))
				.reduce(Counters.empty(), Counters::add);

			LOGGER.fine(() -> String.format(
					"Removed %d constraints and %d indexes, added %d constraints and %d indexes in total.",
					counters.constraintsRemoved(), counters.indexesRemoved(), counters.constraintsAdded(),
					counters.indexesAdded()));
			LOGGER.fine(() -> String.format(
					"Removed %d labels and %d types, added %d labels and %d types and modified %d properties in total.",
					counters.labelsRemoved(), counters.typesRemoved(), counters.labelsAdded(), counters.typesAdded(),
					counters.propertiesSet()));

			try (Session session = operationContext.sessionSupplier().get()) {
				HBD.vladimirAndEstragonMayWait(session, counters);
			}

		}
		catch (VerificationFailedException ex) {
			throw new MigrationsException("Could not apply migration " + Migrations.toString(this)
					+ " verification failed: " + ex.getMessage());
		}
	}

	@Override
	public boolean isRepeatable() {
		return this.version.isRepeatable();
	}

	@Override
	public List<Precondition> getPreconditions() {
		return Collections.unmodifiableList(this.preconditions);
	}

	private enum OperationType {

		VERIFY, CREATE, DROP, APPLY;

		Operation build(Element operationElement, MigrationVersion targetVersion) {
			switch (this) {
				case VERIFY:
					return Operation.verify(Boolean.parseBoolean(operationElement.getAttribute("useCurrent")))
						.includeOptions(Boolean.parseBoolean(operationElement.getAttribute("includeOptions")))
						.allowEquivalent(Boolean.parseBoolean(operationElement.getAttribute("allowEquivalent")))
						.at(targetVersion);
				case CREATE, DROP:
					Optional<Name> optionalName = getOptionalReference(operationElement);
					boolean ifNotExists = Boolean.parseBoolean(operationElement.getAttribute("ifNotExists"));
					boolean ifExists = Boolean.parseBoolean(operationElement.getAttribute("ifExists"));
					return optionalName.<Operation>map(name -> {
						OperationBuilder<?> builder = (this == CREATE)
								? Operation.create(optionalName.get(), ifNotExists)
								: Operation.drop(optionalName.get(), ifExists);
						return builder.with(targetVersion);
					})
						.orElseGet(
								() -> (this == CREATE) ? Operation.create(getLocalItem(operationElement), ifNotExists)
										: Operation.drop(getLocalItem(operationElement), ifExists));
				case APPLY:
					return Operation.apply(targetVersion);
				default:
					throw new IllegalArgumentException("Unsupported operation type: " + this);
			}
		}

		private Optional<Name> getOptionalReference(Element operationElement) {

			if (operationElement.hasAttribute("ref") && operationElement.hasAttribute("item")) {
				throw new IllegalArgumentException(
						"Cannot create an operation referring to an item with both ref and item attributes. Please pick one.");
			}

			// operationElement.hasAttributes() won't work, as it will always return true
			// as the element has a couple of defaulted attributes
			if ((operationElement.hasAttribute("ref") || operationElement.hasAttribute("item"))
					&& hasLocalItem(operationElement)) {
				throw new IllegalArgumentException(
						"Cannot create an operation referring to an element and defining an item locally at the same time.");
			}

			if (operationElement.hasAttribute("ref")) {
				return Optional.of(Name.of(operationElement.getAttribute("ref")));
			}
			else if (operationElement.hasAttribute("item")) {
				return Optional.of(Name.of(operationElement.getAttribute("item")));
			}
			else {
				return Optional.empty();
			}
		}

		private CatalogItem<?> getLocalItem(Element operationElement) {

			if (operationElement.getElementsByTagName("constraint").getLength() == 1) {
				return Constraint.parse((Element) operationElement.getElementsByTagName("constraint").item(0));
			}

			if (operationElement.getElementsByTagName("index").getLength() == 1) {
				return Index.parse((Element) operationElement.getElementsByTagName("index").item(0));
			}

			throw new UnsupportedOperationException("Could not get a local catalog item.");
		}

		private boolean hasLocalItem(Element operationElement) {
			if (!operationElement.hasChildNodes()) {
				return false;
			}
			NodeList childNodes = operationElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); ++i) {
				Node child = childNodes.item(i);
				if (child instanceof Element) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * Something that can be executed from withing a catalog based migration.
	 */
	interface Operation {

		/**
		 * Creates a new drop operation.
		 * @param name the name of the item to drop
		 * @param ifExits should it be an idempotent operation or not?
		 * @return ongoing definition
		 */
		static OperationBuilder<DropOperation> drop(Name name, boolean ifExits) {
			return new DefaultOperationBuilder<DropOperation>(Operator.DROP).drop(name, ifExits);
		}

		/**
		 * Creates a new create operation.
		 * @param name the name of the item to create
		 * @param ifNotExists should it be an idempotent operation or not?
		 * @return ongoing definition
		 */
		static OperationBuilder<CreateOperation> create(Name name, boolean ifNotExists) {
			return new DefaultOperationBuilder<CreateOperation>(Operator.CREATE).create(name, ifNotExists);
		}

		/**
		 * Creates a new drop operation.
		 * @param item the item to create
		 * @param ifExits should it be an idempotent operation or not?
		 * @return ongoing definition
		 */
		static DropOperation drop(CatalogItem<?> item, boolean ifExits) {
			return new DefaultOperationBuilder<DropOperation>(Operator.DROP).drop(item, ifExits);
		}

		/**
		 * Creates a new create operation.
		 * @param item the item to drop
		 * @param ifNotExists should it be an idempotent operation or not?
		 * @return ongoing definition
		 */
		static CreateOperation create(CatalogItem<?> item, boolean ifNotExists) {
			return new DefaultOperationBuilder<CreateOperation>(Operator.CREATE).create(item, ifNotExists);
		}

		/**
		 * Creates a new refactoring operation.
		 * @param refactoring the refactoring to execute
		 * @return a new operation ready to execute.
		 */
		static Operation refactorWith(Refactoring refactoring) {
			return new DefaultRefactorOperation(refactoring);
		}

		/**
		 * Creates a new {@link ApplyOperation}. This operation is potentially
		 * destructive. It will load all supported item types from the database, drop them
		 * and eventually create the content of the catalog.
		 * @param definedAt the version which should be applied
		 * @return the operation ready to execute.
		 */
		static ApplyOperation apply(MigrationVersion definedAt) {
			return new DefaultApplyOperation(definedAt);
		}

		/**
		 * Create a new {@link VerifyOperation}.
		 * @param useCurrent use <code>true</code> to verify / assert the current version,
		 * use <code>false</code> to verify the previous.
		 * @return the operation ready to execute.
		 */
		static VerifyBuilder verify(boolean useCurrent) {
			return new DefaultOperationBuilder<>(null).verify(useCurrent);
		}

		/**
		 * Executes this operation in the given context.
		 * @param context the context in which to execute this operation
		 * @return counters with information about the changes to the schema
		 */
		Counters execute(OperationContext context);

	}

	/**
	 * An operation that requires a version to be executed.
	 */
	interface VersionSpecificOperation extends Operation {

		/**
		 * {@return the version at which this operation has been defined}
		 */
		MigrationVersion definedAt();

	}

	/**
	 * An operation that requires an item to be executed.
	 */
	interface ItemSpecificOperation extends Operation {

		/**
		 * {@return an optional reference to the item that is subject to this operation}
		 */
		Optional<Name> getReference();

		/**
		 * {@return an optional local item.}
		 */
		// Generic items, this is exactly what we want here
		@SuppressWarnings("squid:S1452")
		Optional<CatalogItem<?>> getLocalItem();

	}

	/**
	 * An operation that creates an item from the catalog inside the database.
	 */
	interface CreateOperation extends VersionSpecificOperation, ItemSpecificOperation {

	}

	/**
	 * An operation that drops an item from the catalog from the database.
	 */
	interface DropOperation extends VersionSpecificOperation, ItemSpecificOperation {

	}

	/**
	 * This operation loads all supported item types from the database, drops them and
	 * then creates all items of the local catalog.
	 */
	interface ApplyOperation extends VersionSpecificOperation {

	}

	/**
	 * This operation takes the current catalog and checks whether all items in the
	 * version second to last are defined in the same or equivalent fashion in the
	 * database or if both this catalog or the database are empty. The assertion is done
	 * before the most recent version so that - if necessary - all create and drop
	 * operations can be safely applied. Thus, you can even assert an empty catalog. This
	 * behaviour can be switched to using the current version by using the appropriate
	 * argument to builder method.
	 */
	interface VerifyOperation extends VersionSpecificOperation {

		/**
		 * Returns <code>true</code> if the current version should be verified, defaults
		 * to <code>false</code>.
		 * @return <code>true</code> if the current version should be verified, defaults
		 * to <code>false</code>
		 */
		boolean useCurrent();

		/**
		 * Returns <code>true</code> if the equivalent catalogs are allowed, defaults to
		 * <code>true</code>.
		 * @return <code>true</code> if the equivalent catalogs are allowed, defaults to
		 * <code>true</code>
		 */
		boolean allowEquivalent();

		/**
		 * {@return <code>true</code> if options should be included during verification}
		 */
		boolean includeOptions();

	}

	/**
	 * Specifies the version in which the item that is dealt with has been reference.
	 *
	 * @param <T> the type of operation to build
	 */
	interface OperationBuilder<T extends Operation> {

		T with(MigrationVersion version);

	}

	/**
	 * Specifies the version at which verification should take place.
	 */
	interface TerminalVerifyBuilder {

		VerifyOperation at(MigrationVersion version);

	}

	/**
	 * Allows configuring whether options should be included and whether equivalent but
	 * not identical catalogs are allowed.
	 */
	interface VerifyBuilder extends TerminalVerifyBuilder {

		VerifyBuilder includeOptions(boolean includeOptions);

		TerminalVerifyBuilder allowEquivalent(boolean allowEquivalent);

	}

	record OperationContext(Neo4jVersion version, Neo4jEdition edition, VersionedCatalog catalog,
			MigrationsConfig config, Supplier<Session> sessionSupplier) {
	}

	/**
	 * An operation that executes a {@link Refactoring}.
	 */
	static final class DefaultRefactorOperation implements Operation {

		final Refactoring refactoring;

		DefaultRefactorOperation(Refactoring refactoring) {
			this.refactoring = refactoring;
		}

		@Override
		public Counters execute(OperationContext context) {
			return this.refactoring.apply(new DefaultRefactoringContext(context.sessionSupplier, context.version));
		}

	}

	private static class DefaultOperationBuilder<T extends Operation> implements OperationBuilder<T>, VerifyBuilder {

		private final Operator operator;

		private Name reference;

		private CatalogItem<?> item;

		private boolean idempotent;

		private boolean useCurrent;

		private boolean allowEquivalent = true;

		private boolean includingOptions = false;

		DefaultOperationBuilder(final Operator operator) {
			this.operator = operator;
		}

		@SuppressWarnings({ "HiddenField" })
		OperationBuilder<T> drop(Name reference, boolean ifExits) {

			this.reference = reference;
			this.idempotent = ifExits;
			return this;
		}

		@SuppressWarnings({ "HiddenField" })
		OperationBuilder<T> create(Name reference, boolean ifNotExists) {

			this.reference = reference;
			this.idempotent = ifNotExists;
			return this;
		}

		@SuppressWarnings({ "HiddenField" })
		DropOperation drop(CatalogItem<?> item, boolean ifExits) {

			this.item = item;
			this.idempotent = ifExits;
			return new DefaultDropOperation(null, this.reference, item, this.idempotent);
		}

		@SuppressWarnings({ "HiddenField" })
		CreateOperation create(CatalogItem<?> item, boolean ifNotExists) {

			this.item = item;
			this.idempotent = ifNotExists;
			return new DefaultCreateOperation(null, this.reference, item, this.idempotent);
		}

		@SuppressWarnings({ "HiddenField" })
		VerifyBuilder verify(boolean useCurrent) {

			this.useCurrent = useCurrent;
			return this;
		}

		@SuppressWarnings({ "HiddenField" })
		@Override
		public VerifyBuilder includeOptions(boolean includeOptions) {

			this.includingOptions = includeOptions;
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
			return new DefaultVerifyOperation(this.useCurrent, this.includingOptions, this.allowEquivalent, version);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T with(MigrationVersion version) {

			if (this.operator == Operator.DROP) {
				return (T) new DefaultDropOperation(version, this.reference, this.item, this.idempotent);
			}
			else if (this.operator == Operator.CREATE) {
				return (T) new DefaultCreateOperation(version, this.reference, this.item, this.idempotent);
			}
			else {
				throw new UnsupportedOperationException();
			}
		}

	}

	/**
	 * Some state for all operations working on a specific item defined by a named
	 * reference.
	 */
	private abstract static class AbstractItemBasedOperation
			implements VersionSpecificOperation, ItemSpecificOperation {

		protected final MigrationVersion definedAt;

		protected final Name reference;

		protected final CatalogItem<?> localItem;

		protected final boolean idempotent;

		AbstractItemBasedOperation(MigrationVersion definedAt, Name reference, CatalogItem<?> localItem,
				boolean idempotent) {

			if (definedAt == null && localItem == null) {
				throw new IllegalArgumentException("Without a version, a concrete, local item is required.");
			}
			if (reference != null && localItem != null) {
				throw new IllegalArgumentException("Either reference or item is required, not both.");
			}

			this.definedAt = definedAt;
			this.reference = reference;
			this.localItem = localItem;
			this.idempotent = idempotent;
		}

		@SuppressWarnings("squid:S1452") // Generic items, this is exactly what we want
											// here
		CatalogItem<?> getRequiredItem(VersionedCatalog catalog) {

			// I want Java9+ and better optionals, so that I can or them
			if (this.localItem != null) {
				return this.localItem;
			}

			return catalog.getItem(this.reference, this.definedAt)
				.orElseThrow(() -> new MigrationsException(
						String.format("An item named '%s' has not been defined as of version %s.",
								this.reference.getValue(), this.definedAt.getValue())));
		}

		@Override
		public MigrationVersion definedAt() {
			return this.definedAt;
		}

		@Override
		public Optional<Name> getReference() {
			return Optional.ofNullable(this.reference);
		}

		@Override
		public Optional<CatalogItem<?>> getLocalItem() {
			return Optional.ofNullable(this.localItem);
		}

	}

	/**
	 * Executes creates.
	 */
	static final class DefaultCreateOperation extends AbstractItemBasedOperation implements CreateOperation {

		DefaultCreateOperation(MigrationVersion definedAt, Name reference, CatalogItem<?> item, boolean idempotent) {
			super(definedAt, reference, item, idempotent);
		}

		@Override
		public Counters execute(OperationContext context) {

			try (Session queryRunner = context.sessionSupplier.get()) {
				CatalogItem<?> item = getRequiredItem(context.catalog);
				Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
				RenderConfig config = RenderConfig.create()
					.idempotent(this.idempotent)
					.forVersionAndEdition(context.version, context.edition)
					.withAdditionalOptions(context.config().getConstraintRenderingOptions());

				if (this.idempotent && !context.version.hasIdempotentOperations()) {
					config = config.ignoreName();
					return createIfNotExists(context, item, queryRunner, renderer, config);
				}
				else {
					return schemaCounters(queryRunner.run(renderer.render(item, config)).consume().counters());
				}
			}
		}

		private Counters createIfNotExists(OperationContext context, CatalogItem<?> item, QueryRunner queryRunner,
				Renderer<CatalogItem<?>> renderer, RenderConfig config) {

			try {
				return schemaCounters(queryRunner.run(renderer.render(item, config)).consume().counters());
			}
			catch (Neo4jException ex) {
				// Directly throw anything that can't match
				if (!Neo4jCodes.CODES_FOR_EXISTING_CONSTRAINT.contains(ex.code())) {
					throw ex;
				}

				// Make sure the thing actually is there.
				List<CatalogItem<?>> items = (item instanceof Constraint)
						? queryRunner.run(context.version.getShowConstraints()).list(Constraint::parse)
						: queryRunner.run(context.version.getShowIndexes()).list(Index::parse);

				// If there are no constraints there at all, something fishy is going on
				// for sure
				// otherwise, there must now an equivalent version of it
				if (items.isEmpty() || items.stream().noneMatch(existingItem -> existingItem.isEquivalentTo(item))) {
					throw ex;
				}
			}
			return Counters.empty();
		}

	}

	/**
	 * Executes drops.
	 */
	static final class DefaultDropOperation extends AbstractItemBasedOperation implements DropOperation {

		DefaultDropOperation(MigrationVersion definedAt, Name reference, CatalogItem<?> item, boolean idempotent) {
			super(definedAt, reference, item, idempotent);
		}

		@Override
		public Counters execute(OperationContext context) {

			try (Session queryRunner = context.sessionSupplier.get()) {
				CatalogItem<?> item = getRequiredItem(context.catalog);
				Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
				RenderConfig config = RenderConfig.drop()
					.idempotent(this.idempotent)
					.forVersionAndEdition(context.version, context.edition)
					.withAdditionalOptions(context.config().getConstraintRenderingOptions());

				if (this.idempotent && !context.version.hasIdempotentOperations()) {
					config = config.ignoreName();
					return drop(context, item, queryRunner, renderer, config, true);
				}
				else {
					return schemaCounters(queryRunner.run(renderer.render(item, config)).consume().counters());
				}
			}
		}

		private Counters drop(OperationContext context, CatalogItem<?> item, QueryRunner queryRunner,
				Renderer<CatalogItem<?>> renderer, RenderConfig config, boolean fallbackToPrior) {

			try {
				return schemaCounters(queryRunner.run(renderer.render(item, config)).consume().counters());
			}
			catch (Neo4jException ex) {
				// Directly throw anything that can't match
				if (!Neo4jCodes.CONSTRAINT_DROP_FAILED.equals(ex.code())) {
					throw ex;
				}

				if (!(item instanceof Constraint || item instanceof Index)) {
					throw new IllegalStateException("Item type " + item.getClass() + " not supported");
				}

				// Make sure the thing actually not there.
				List<CatalogItem<?>> items = (item instanceof Constraint)
						? queryRunner.run(context.version.getShowConstraints()).list(Constraint::parse)
						: queryRunner.run(context.version.getShowIndexes()).list(Index::parse);

				if (items.isEmpty()) {
					return Counters.empty();
				}

				if (items.stream().anyMatch(existingIndex -> existingIndex.isEquivalentTo(item))) {
					throw ex;
				}

				if (!fallbackToPrior || getLocalItem().isPresent()) {
					return Counters.empty();
				}

				// If it has been defined in an older version users might have redefined
				// it in this version,
				// such that couldn't have been dropped
				return context.catalog.getItemPriorTo(this.reference, this.definedAt)
					.filter(v -> items.stream().anyMatch(existingIndex -> existingIndex.isEquivalentTo(v)))
					.map(olderItem -> drop(context, olderItem, queryRunner, renderer, config, false))
					.orElseGet(Counters::empty);
			}
		}

	}

	/**
	 * Default implementation of verification.
	 *
	 * @param useCurrent use the current (latest) version
	 * @param includeOptions include all options in verifcation
	 * @param allowEquivalent allow operations that are equivalent
	 * @param definedAt the that defined this item
	 */
	record DefaultVerifyOperation(boolean useCurrent, boolean includeOptions, boolean allowEquivalent,
			MigrationVersion definedAt) implements VerifyOperation {

		@Override
		public Counters execute(OperationContext context) {

			try (Session queryRunner = context.sessionSupplier.get()) {
				// Get all the constraints
				Catalog databaseCatalog = DatabaseCatalog.of(context.version, queryRunner, this.includeOptions);
				VersionedCatalog currentCatalog = context.catalog;

				CatalogDiff diff = CatalogDiff.between(databaseCatalog,
						this.useCurrent ? currentCatalog.getCatalogAt(this.definedAt)
								: currentCatalog.getCatalogPriorTo(this.definedAt));

				if (diff.identical()) {
					LOGGER.log(Level.FINE, "Database schema and catalog are identical.");
				}
				else if (diff.equivalent() && this.allowEquivalent) {
					LOGGER.warning(() -> buildEquivalentWarningMessage(diff));
				}
				else {
					throw new VerificationFailedException(diff.equivalent()
							? "Database schema and the catalog are equivalent but the verification requires them to be identical."
							: "Catalogs are neither identical nor equivalent.");
				}

				return Counters.empty();
			}
		}

		private String buildEquivalentWarningMessage(CatalogDiff diff) {
			StringBuilder message = new StringBuilder();
			Collection<CatalogItem<?>> itemsOnlyInRight = diff.getItemsOnlyInRight();
			message.append(
					"Items in the database are not identical to items in the schema catalog. The following items have different names but an equivalent definition:");
			diff.getItemsOnlyInLeft()
				.forEach(item -> itemsOnlyInRight.stream()
					.filter(item::isEquivalentTo)
					.findFirst()
					.ifPresent(equivalentItem -> message.append(System.lineSeparator())
						.append("* Database item `")
						.append(item.getName().getValue())
						.append("` matches catalog item `")
						.append(equivalentItem.getName().getValue())
						.append("`")));
			return message.toString();
		}
	}

	static final class VerificationFailedException extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 6481650211840799118L;

		VerificationFailedException(String message) {
			super(message);
		}

	}

	/**
	 * Drops everything from the database catalog, adds everything from the migrations
	 * catalog.
	 *
	 * @param definedAt the version that defined this operation
	 */
	record DefaultApplyOperation(MigrationVersion definedAt) implements ApplyOperation {

		@Override
		public Counters execute(OperationContext context) {

			try (Session queryRunner = context.sessionSupplier.get()) {
				// Get all the constraints
				Catalog databaseCatalog = DatabaseCatalog.of(context.version, queryRunner, false);

				// Make them go away
				RenderConfig dropConfig = RenderConfig.drop().forVersionAndEdition(context.version, context.edition);
				AtomicInteger constraintsRemoved = new AtomicInteger(0);
				AtomicInteger indexesRemoved = new AtomicInteger(0);
				databaseCatalog.getItems().forEach(catalogItem -> {
					Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, catalogItem);
					SummaryCounters counters = queryRunner.run(renderer.render(catalogItem, dropConfig))
						.consume()
						.counters();
					constraintsRemoved.addAndGet(counters.constraintsRemoved());
					indexesRemoved.addAndGet(counters.indexesRemoved());
				});

				// Add the new ones
				RenderConfig createConfig = RenderConfig.create()
					.forVersionAndEdition(context.version, context.edition)
					.withAdditionalOptions(context.config().getConstraintRenderingOptions());
				AtomicInteger constraintsAdded = new AtomicInteger(0);
				AtomicInteger indexesAdded = new AtomicInteger(0);
				context.catalog.getCatalogAt(this.definedAt).getItems().forEach(item -> {
					Renderer<CatalogItem<?>> renderer = Renderer.get(Renderer.Format.CYPHER, item);
					SummaryCounters counters = queryRunner.run(renderer.render(item, createConfig))
						.consume()
						.counters();
					constraintsAdded.addAndGet(counters.constraintsAdded());
					indexesAdded.addAndGet(counters.indexesAdded());
				});

				return schemaCounters(indexesAdded.get(), indexesRemoved.get(), constraintsAdded.get(),
						constraintsRemoved.get());
			}
		}
	}

}
