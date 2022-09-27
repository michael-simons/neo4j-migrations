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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.IndexNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;
import ac.simons.neo4j.migrations.annotations.proc.CatalogNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementKindVisitor8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * @author Michael J. Simons
 * @soundtrack Moonbootica - ...And Then We Started To Dance
 * @since 1.11.0
 */
@SupportedAnnotationTypes({
	FullyQualifiedNames.SDN6_NODE,
	FullyQualifiedNames.OGM_NODE,
	FullyQualifiedNames.OGM_RELATIONSHIP
})
@SupportedOptions({
	CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_CATALOG,
	CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_CONSTRAINTS,
	CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_INDEXES,
	CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_OPTIONS,
	CatalogGeneratingProcessor.OPTION_OUTPUT_DIR,
	CatalogGeneratingProcessor.OPTION_TIMESTAMP,
	CatalogGeneratingProcessor.OPTION_DEFAULT_CATALOG_NAME,
	CatalogGeneratingProcessor.OPTION_ADD_RESET
})
public final class CatalogGeneratingProcessor extends AbstractProcessor {

	static final String OPTION_NAME_GENERATOR_CATALOG = "org.neo4j.migrations.catalog_generator.catalog_name_generator";
	static final String OPTION_NAME_GENERATOR_CONSTRAINTS = "org.neo4j.migrations.catalog_generator.constraint_name_generator";
	static final String OPTION_NAME_GENERATOR_INDEXES = "org.neo4j.migrations.catalog_generator.index_name_generator";
	static final String OPTION_NAME_GENERATOR_OPTIONS = "org.neo4j.migrations.catalog_generator.naming_options";
	static final String OPTION_OUTPUT_DIR = "org.neo4j.migrations.catalog_generator.output_dir";
	static final String OPTION_TIMESTAMP = "org.neo4j.migrations.catalog_generator.timestamp";
	static final String OPTION_DEFAULT_CATALOG_NAME = "org.neo4j.migrations.catalog_generator.default_catalog_name";
	static final String OPTION_ADD_RESET = "org.neo4j.migrations.catalog_generator.add_reset";
	static final String DEFAULT_MIGRATION_NAME = "Create_schema_from_domain_objects.xml";
	static final String DEFAULT_HEADER_FMT = "This file was generated by Neo4j-Migrations at %s.";

	static final Set<String> VALID_GENERATED_ID_TYPES = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(Long.class.getName(), long.class.getName())));

	private static final String ATTRIBUTE_TYPE = "type";
	private static final String ATTRIBUTE_VALUE = "value";
	private static final String ATTRIBUTE_LABEL = "label";
	private static final String ATTRIBUTE_LABELS = "labels";
	private static final String ATTRIBUTE_PRIMARY_LABEL = "primaryLabel";
	private static final String ATTRIBUTE_PROPERTIES = "properties";
	private static final String ATTRIBUTE_UNIQUE = "unique";

	private CatalogNameGenerator catalogNameGenerator;
	private ConstraintNameGenerator constraintNameGenerator;
	private IndexNameGenerator indexNameGenerator;

	private Messager messager;
	private Types typeUtils;

	private TypeElement sdn6Node;
	private ExecutableElement sdn6NodeValue;
	private ExecutableElement sdn6NodeLabels;
	private ExecutableElement sdn6NodePrimaryLabel;

	private TypeElement sdn6Id;
	private TypeElement sdn6GeneratedValue;
	private TypeElement commonsId;

	private TypeElement ogmNode;
	private ExecutableElement ogmNodeValue;
	private ExecutableElement ogmNodeLabel;

	private TypeElement ogmRelationship;
	private ExecutableElement ogmRelationshipType;
	private ExecutableElement ogmRelationshipValue;

	private TypeElement ogmId;
	private TypeElement ogmGeneratedValue;

	private TypeElement ogmCompositeIndexes;
	private ExecutableElement ogmCompositeIndexesValue;
	private TypeElement ogmCompositeIndex;
	private ExecutableElement ogmCompositeIndexValue;
	private ExecutableElement ogmCompositeIndexProperties;
	private ExecutableElement ogmCompositeIndexUnique;

	private TypeElement ogmIndex;
	private ExecutableElement ogmIndexUnique;

	private TypeElement ogmRequired;

	private final List<CatalogItem<?>> catalogItems = new ArrayList<>();

	private boolean addReset;

	private Clock clock = Clock.systemDefaultZone();

	/**
	 * Creates a new instance of this processor. It should not be necessary to call this directly, it will be done
	 * automatically by Javac.
	 */
	public CatalogGeneratingProcessor() {
	}

	CatalogGeneratingProcessor(CatalogNameGenerator catalogNameGenerator,
		ConstraintNameGenerator constraintNameGenerator, IndexNameGenerator indexNameGenerator) {
		this.catalogNameGenerator = catalogNameGenerator;
		this.constraintNameGenerator = constraintNameGenerator;
		this.indexNameGenerator = indexNameGenerator;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	<T> T nameGenerator(String optionName, Class<T> expectedType, Supplier<T> defaultSupplier,
		Map<String, String> options) {

		if (!options.containsKey(optionName)) {
			return defaultSupplier.get();
		}
		String fqn = options.get(optionName);
		Map<String, String> generatorOptions = null;
		if (options.containsKey(OPTION_NAME_GENERATOR_OPTIONS)) {
			generatorOptions = Arrays.stream(options.get(OPTION_NAME_GENERATOR_OPTIONS).split(","))
				.map(String::trim)
				.map(s -> s.split("="))
				.collect(Collectors.toMap(v -> v[0].trim(), v -> v[1].trim()));
		}

		try {
			if (generatorOptions == null) {
				return expectedType.cast(Class.forName(fqn).getConstructor().newInstance());
			} else {
				return expectedType.cast(Class.forName(fqn).getConstructor(Map.class).newInstance(generatorOptions));
			}
		} catch (ClassCastException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
			messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Could not load `" + fqn + "`, using default for " + optionName);
			return defaultSupplier.get();
		}
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {

		super.init(processingEnv);

		this.messager = processingEnv.getMessager();

		Map<String, String> options = processingEnv.getOptions();
		if (this.catalogNameGenerator == null) {
			this.catalogNameGenerator = nameGenerator(OPTION_NAME_GENERATOR_CATALOG, CatalogNameGenerator.class,
				() -> {
					String defaultName = options.getOrDefault(OPTION_DEFAULT_CATALOG_NAME, DEFAULT_MIGRATION_NAME);
					return () -> defaultName;
				}, options);
		}

		if (this.constraintNameGenerator == null) {
			this.constraintNameGenerator = nameGenerator(OPTION_NAME_GENERATOR_CONSTRAINTS,
				ConstraintNameGenerator.class,
				DefaultConstraintNameGenerator::new, options);
		}

		if (this.indexNameGenerator == null) {
			this.indexNameGenerator = nameGenerator(OPTION_NAME_GENERATOR_INDEXES, IndexNameGenerator.class, DefaultIndexNameGenerator::new, options);
		}

		Elements elementUtils = processingEnv.getElementUtils();
		this.sdn6Node = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_NODE);
		this.sdn6NodeValue = getAnnotationAttribute(sdn6Node, ATTRIBUTE_VALUE);
		this.sdn6NodeLabels = getAnnotationAttribute(sdn6Node, ATTRIBUTE_LABELS);
		this.sdn6NodePrimaryLabel = getAnnotationAttribute(sdn6Node, ATTRIBUTE_PRIMARY_LABEL);

		this.sdn6Id = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_ID);
		this.sdn6GeneratedValue = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_GENERATED_VALUE);
		this.commonsId = elementUtils.getTypeElement(FullyQualifiedNames.COMMONS_ID);

		this.ogmNode = elementUtils.getTypeElement(FullyQualifiedNames.OGM_NODE);
		this.ogmNodeValue = getAnnotationAttribute(ogmNode, ATTRIBUTE_VALUE);
		this.ogmNodeLabel = getAnnotationAttribute(ogmNode, ATTRIBUTE_LABEL);

		this.ogmRelationship = elementUtils.getTypeElement(FullyQualifiedNames.OGM_RELATIONSHIP);
		this.ogmRelationshipValue = getAnnotationAttribute(ogmRelationship, ATTRIBUTE_VALUE);
		this.ogmRelationshipType = getAnnotationAttribute(ogmRelationship, ATTRIBUTE_TYPE);

		this.ogmId = elementUtils.getTypeElement(FullyQualifiedNames.OGM_ID);
		this.ogmGeneratedValue = elementUtils.getTypeElement(FullyQualifiedNames.OGM_GENERATED_VALUE);

		this.ogmCompositeIndexes = elementUtils.getTypeElement(FullyQualifiedNames.OGM_COMPOSITE_INDEXES);
		this.ogmCompositeIndexesValue = getAnnotationAttribute(ogmCompositeIndexes, ATTRIBUTE_VALUE);

		this.ogmCompositeIndex = elementUtils.getTypeElement(FullyQualifiedNames.OGM_COMPOSITE_INDEX);
		this.ogmCompositeIndexValue = getAnnotationAttribute(ogmCompositeIndex, ATTRIBUTE_VALUE);
		this.ogmCompositeIndexProperties = getAnnotationAttribute(ogmCompositeIndex, ATTRIBUTE_PROPERTIES);
		this.ogmCompositeIndexUnique = getAnnotationAttribute(ogmCompositeIndex, ATTRIBUTE_UNIQUE);

		this.ogmIndex = elementUtils.getTypeElement(FullyQualifiedNames.OGM_INDEX);
		this.ogmIndexUnique = getAnnotationAttribute(ogmIndex, ATTRIBUTE_UNIQUE);

		this.ogmRequired = elementUtils.getTypeElement(FullyQualifiedNames.OGM_REQUIRED);

		this.typeUtils = processingEnv.getTypeUtils();

		this.addReset = Boolean.parseBoolean(options.getOrDefault(OPTION_ADD_RESET, "false"));
		String timestamp = options.get(OPTION_TIMESTAMP);
		if (timestamp != null && !timestamp.isEmpty()) {
			ZonedDateTime z = ZonedDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestamp));
			this.clock = Clock.fixed(z.toInstant(), z.getZone());
		}
	}

	static ExecutableElement getAnnotationAttribute(TypeElement annotation, String name) {
		if (annotation == null) {
			return null;
		}
		return (ExecutableElement) annotation.getEnclosedElements().stream()
			.filter(e -> e.getSimpleName().contentEquals(name)).findFirst()
			.orElseThrow(NoSuchElementException::new);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		if (roundEnv.processingOver()) {
			try {
				FileObject fileObject = processingEnv.getFiler()
					.createResource(StandardLocation.SOURCE_OUTPUT, "",
						getOutputDir() + catalogNameGenerator.getCatalogName());
				try (OutputStream out = new BufferedOutputStream(fileObject.openOutputStream())) {
					Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
					Neo4jVersion neo4jVersion = Neo4jVersion.LATEST;
					XMLRenderingOptionsImpl o = new XMLRenderingOptionsImpl(true, addReset, String.format(
						DEFAULT_HEADER_FMT, ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
					RenderConfig config = RenderConfig.create()
						.idempotent(neo4jVersion.hasIdempotentOperations())
						.forVersionAndEdition(neo4jVersion, Neo4jEdition.ENTERPRISE)
						.withAdditionalOptions(Collections.singletonList(o));

					renderer.render(Catalog.of(catalogItems), config, out);
				}
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
			}
		} else if (!annotations.isEmpty()) {

			if (sdn6Node != null) {
				processSDN6IdAnnotations(roundEnv);
			}
			if (ogmNode != null) {
				processOGMIdAnnotations(roundEnv);
				processOGMIndexAnnotations(roundEnv, ogmNode);
				processOGMIndexAnnotations(roundEnv, ogmRelationship);
				processOGMCompositeIndexAnnotations(roundEnv);
			}
		}

		return true;
	}

	private void processSDN6IdAnnotations(RoundEnvironment roundEnv) {

		Set<Element> supportedSDN6Annotations = new HashSet<>();
		supportedSDN6Annotations.add(sdn6Id);
		supportedSDN6Annotations.add(commonsId);

		roundEnv.getElementsAnnotatedWith(sdn6Node)
			.stream()
			.filter(this::requiresPrimaryKeyConstraintSDN6)
			.map(TypeElement.class::cast)
			.forEach(t -> {
				List<SchemaName> labels = computeLabelsSDN6(t);
				PropertyType<NodeType> idProperty = t.accept(new PropertySelector(supportedSDN6Annotations),
					new DefaultNodeType(t.getQualifiedName().toString(), labels));
				String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
					Collections.singleton(idProperty));
				catalogItems.add(Constraint.forNode(labels.get(0).getValue()).named(name)
					.unique(idProperty.getName()));
			});
	}

	private void processOGMIdAnnotations(RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(ogmNode)
			.stream()
			.filter(this::requiresPrimaryKeyConstraintOGM)
			.map(TypeElement.class::cast)
			.forEach(t -> {
				List<SchemaName> labels = computeLabelsOGM(t);
				PropertyType<NodeType> idProperty = t.accept(new PropertySelector(Collections.singleton(ogmId)),
					new DefaultNodeType(t.getQualifiedName().toString(), labels));
				String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
					Collections.singleton(idProperty));
				catalogItems.add(Constraint.forNode(labels.get(0).getValue()).named(name)
					.unique(idProperty.getName()));
			});
	}

	private void processOGMIndexAnnotations(RoundEnvironment roundEnv, TypeElement processedAnnotation) {
		roundEnv.getElementsAnnotatedWith(processedAnnotation)
			.stream()
			.map(TypeElement.class::cast)
			.forEach(t -> {
				List<SchemaName> labels = processedAnnotation == ogmNode ? computeLabelsOGM(t) : Collections.singletonList(computeTypeOGM(t));
				catalogItems.addAll(t.accept(new OGMIndexVisitor<>(labels, processedAnnotation),
					new DefaultNodeType(t.getQualifiedName().toString(), labels)));
			});
	}

	private void processOGMCompositeIndexAnnotations(RoundEnvironment roundEnv) {

		Set<? extends Element> nodes = roundEnv.getElementsAnnotatedWith(ogmNode);
		Set<? extends Element> composeIndexNodes = roundEnv.getElementsAnnotatedWith(ogmCompositeIndex);
		Set<? extends Element> composeIndexesNodes = roundEnv.getElementsAnnotatedWith(ogmCompositeIndexes);
		Predicate<Element> elementsAnnotatedWithCompositeIndex = composeIndexNodes::contains;
		elementsAnnotatedWithCompositeIndex = elementsAnnotatedWithCompositeIndex.or(composeIndexesNodes::contains);
		nodes
			.stream()
			.filter(elementsAnnotatedWithCompositeIndex)
			.map(TypeElement.class::cast)
			.forEach(t -> catalogItems.addAll(computeOGMCompositeIndexes(t)));
	}

	@SuppressWarnings({"unchecked", "squid:S1452"})
	Collection<CatalogItem<?>> computeOGMCompositeIndexes(TypeElement typeElement) {

		List<SchemaName> labels = computeLabelsOGM(typeElement);
		DefaultNodeType node = new DefaultNodeType(typeElement.getQualifiedName().toString(), labels);
		return typeElement.getAnnotationMirrors().stream()
			.flatMap(m -> {
				if (m.getAnnotationType().asElement().equals(ogmCompositeIndex)) {
					return Stream.of(m);
				} else if (m.getAnnotationType().asElement().equals(ogmCompositeIndexes)) {
					List<AnnotationValue> values = (List<AnnotationValue>) m.getElementValues()
						.get(ogmCompositeIndexesValue).getValue();
					return values.stream().map(AnnotationValue::getValue).map(AnnotationMirror.class::cast);
				}
				return Stream.empty();
			})
			.map(t -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
				List<AnnotationValue> values = new ArrayList<>();
				if (attributes.containsKey(ogmCompositeIndexValue)) {
					values.addAll((List<AnnotationValue>) attributes.get(ogmCompositeIndexValue).getValue());
				}
				if (attributes.containsKey(ogmCompositeIndexProperties)) {
					values.addAll((List<AnnotationValue>) attributes.get(ogmCompositeIndexProperties).getValue());
				}

				boolean isUnique =
					attributes.containsKey(ogmCompositeIndexUnique) && (boolean) attributes.get(ogmCompositeIndexUnique)
						.getValue();
				List<PropertyType<?>> properties = values.stream()
					.map(v -> node.addProperty((String) v.getValue()))
					.collect(Collectors.toList());
				String[] propertyNames = properties.stream().map(PropertyType::getName).toArray(String[]::new);

				if (propertyNames.length == 0) {
					messager.printMessage(Diagnostic.Kind.ERROR, String.format("Cannot use %s without any properties on %s", FullyQualifiedNames.OGM_COMPOSITE_INDEX, typeElement), typeElement);
					return null;
				}

				CatalogItem<?> item;
				if (isUnique) {
					String name = constraintNameGenerator.generateName(Constraint.Type.KEY, properties);
					item = Constraint.forNode(node.getLabels().get(0).getValue()).named(name).key(propertyNames);
				} else {
					String indexName = indexNameGenerator.generateName(Index.Type.PROPERTY, properties);
					item = Index.forNode(node.getLabels().get(0).getValue()).named(indexName).onProperties(propertyNames);
				}
				return item;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	boolean requiresPrimaryKeyConstraintSDN6(Element e) {
		Collection<TypeElement> idAnnotations = Arrays.asList(sdn6Id, commonsId);
		TypeElement generatedValueAnnotation = sdn6GeneratedValue;
		String generatorAttributeName = "generatorClass";
		String internalIdGeneratorClass = "org.springframework.data.neo4j.core.schema.GeneratedValue.InternalIdGenerator";
		return e.accept(new RequiresPrimaryKeyConstraintPredicate(idAnnotations, generatedValueAnnotation, generatorAttributeName, internalIdGeneratorClass), false);
	}

	boolean requiresPrimaryKeyConstraintOGM(Element e) {
		Collection<TypeElement> idAnnotations = Arrays.asList(ogmId);
		TypeElement generatedValueAnnotation = ogmGeneratedValue;
		String generatorAttributeName = "strategy";
		String internalIdGeneratorClass = "org.neo4j.ogm.id.InternalIdStrategy";
		return e.accept(new RequiresPrimaryKeyConstraintPredicate(idAnnotations, generatedValueAnnotation, generatorAttributeName, internalIdGeneratorClass), false);
	}

	/**
	 * @param typeElement The type element to process
	 * @return An ordered list so that we can pass it to any API that requires the labels to be in order (primary first).
	 */
	@SuppressWarnings("unchecked")
	private List<SchemaName> computeLabelsSDN6(TypeElement typeElement) {

		Set<SchemaName> result = new LinkedHashSet<>();
		BiConsumer<Boolean, AnnotationMirror> computation = (addSimpleName, t) -> {
			Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
			if (attributes.containsKey(sdn6NodePrimaryLabel)) {
				result.add(DefaultSchemaName.of((String) attributes.get(sdn6NodePrimaryLabel).getValue()));
			}

			List<AnnotationValue> values = new ArrayList<>();
			if (attributes.containsKey(sdn6NodeValue)) {
				values.addAll((List<AnnotationValue>) attributes.get(sdn6NodeValue).getValue());
			}
			if (attributes.containsKey(sdn6NodeLabels)) {
				values.addAll((List<AnnotationValue>) attributes.get(sdn6NodeLabels).getValue());
			}
			values.stream().map(v -> DefaultSchemaName.of((String) v.getValue())).forEach(result::add);

			if (result.isEmpty() && Boolean.TRUE.equals(addSimpleName)) {
				result.add(DefaultSchemaName.of(typeElement.getSimpleName().toString()));
			}
		};
		traverseClassHierarchy(sdn6Node, typeElement, computation, true);
		return new ArrayList<>(result);
	}

	private List<SchemaName> computeLabelsOGM(TypeElement typeElement) {
		return computeOGMModel(ogmNode, typeElement, UnaryOperator.identity(), ogmNodeLabel, ogmNodeValue);
	}

	private SchemaName computeTypeOGM(TypeElement typeElement) {
		List<SchemaName> names = computeOGMModel(ogmRelationship, typeElement, s -> s.toUpperCase(Locale.ROOT), ogmRelationshipType, ogmRelationshipValue);
		if (names.size() != 1) {
			messager.printMessage(Diagnostic.Kind.ERROR, String.format("More than one relationship type found on %s", typeElement), typeElement);
		}
		return names.get(0);
	}

	/**
	 * @param entityAnnotation   The entity type to travers
	 * @param typeElement        The type element to process
	 * @param simpleNameFilter   Filter for the simple name
	 * @param selectedAttributes the attributes of the annotation to process
	 * @return An ordered list so that we can pass it to any API that requires the labels to be in order (primary first).
	 */
	@SuppressWarnings("unchecked")
	private List<SchemaName> computeOGMModel(TypeElement entityAnnotation, TypeElement typeElement, UnaryOperator<String> simpleNameFilter, ExecutableElement... selectedAttributes) {

		Set<SchemaName> result = new LinkedHashSet<>();
		BiConsumer<Boolean, AnnotationMirror> labelComputation = (addSimpleName, t) -> {
			Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
			for (ExecutableElement selectedAttribute : selectedAttributes) {
				if (attributes.containsKey(selectedAttribute)) {
					result.add(DefaultSchemaName.of((String) attributes.get(selectedAttribute).getValue()));
				}
			}

			if (result.isEmpty() && Boolean.TRUE.equals(addSimpleName)) {
				result.add(DefaultSchemaName.of(simpleNameFilter.apply(typeElement.getSimpleName().toString())));
			}
		};
		traverseClassHierarchy(entityAnnotation, typeElement, labelComputation, true);
		return new ArrayList<>(result);
	}

	private void traverseClassHierarchy(
		TypeElement requiredAnnotation,
		TypeElement typeElement,
		BiConsumer<Boolean, AnnotationMirror> consumer,
		boolean addSimpleName
	) {
		typeElement.getAnnotationMirrors().stream()
			.filter(m -> m.getAnnotationType().asElement().equals(requiredAnnotation))
			.findFirst()
			.ifPresent(v -> consumer.accept(addSimpleName, v));

		typeElement.getInterfaces()
			.stream()
			.map(i -> typeUtils.asElement(i))
			.map(TypeElement.class::cast)
			.forEach(i -> traverseClassHierarchy(requiredAnnotation, i, consumer, false));

		findSuperclassOfInterest(typeElement).ifPresent(superclass -> traverseClassHierarchy(requiredAnnotation, superclass, consumer, false));
	}

	Optional<TypeElement> findSuperclassOfInterest(TypeElement typeElement) {
		return Optional.ofNullable(typeElement.getSuperclass())
			.map(typeUtils::asElement)
			.map(TypeElement.class::cast)
			.filter(e -> !e.getQualifiedName().contentEquals("java.lang.Object"));
	}

	String getOutputDir() {

		String subDir = processingEnv.getOptions().getOrDefault(OPTION_OUTPUT_DIR, "neo4j-migrations");
		if (!subDir.endsWith("/")) {
			subDir += "/";
		}
		return subDir;
	}

	@SuppressWarnings("squid:S110") // Not something we need or can do anything about (Number of parents)
	class OGMIndexVisitor<E extends ElementType<E>> extends ElementKindVisitor8<List<CatalogItem<?>>, WriteableElementType<E>> {

		private final List<SchemaName> schemaNames;

		private final boolean isRelationship;

		OGMIndexVisitor(List<SchemaName> schemaNames, TypeElement processedAnnotation) {
			this.schemaNames = schemaNames;
			this.isRelationship = processedAnnotation == ogmRelationship;
		}

		@Override
		public List<CatalogItem<?>> visitType(TypeElement e, WriteableElementType<E> owner) {
			return e.getEnclosedElements().stream()
				.map(ee -> ee.accept(this, owner))
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		}

		@Override
		public List<CatalogItem<?>> visitVariableAsField(VariableElement e, WriteableElementType<E> owner) {

			List<? extends AnnotationMirror> indexAnnotations = e.getAnnotationMirrors().stream()
				.filter(a -> {
					Element element = a.getAnnotationType().asElement();
					return element.equals(ogmIndex) || element.equals(ogmRequired);
				})
				.collect(Collectors.toList());

			if (indexAnnotations.isEmpty()) {
				return Collections.emptyList();
			}

			boolean isUnique = indexAnnotations.stream()
				.anyMatch(a -> {
					Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = a.getElementValues();
					if (attributes.containsKey(ogmIndexUnique)) {
						return (boolean) attributes.get(ogmIndexUnique).getValue();
					}
					return false;
				});

			if (isUnique && isRelationship) {
				messager.printMessage(Diagnostic.Kind.ERROR,
					String.format("Unique constraints defined at %s are not allowed on relationships", e.getEnclosingElement()), e.getEnclosingElement());
				return Collections.emptyList();
			}

			PropertyType<E> property = owner.addProperty(e.getSimpleName().toString());
			if (isUnique) {
				String name = constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
					Collections.singleton(property));
				return Collections.singletonList(
					Constraint.forNode(schemaNames.get(0).getValue()).named(name).unique(property.getName()));
			}

			boolean isRequired = indexAnnotations.stream().anyMatch(a -> a.getAnnotationType().asElement().equals(ogmRequired));
			if (isRelationship) {
				return handleRelationship(property, isRequired);
			}
			return handleNode(property, isRequired);
		}

		@SuppressWarnings("squid:S1452")
		List<CatalogItem<?>> handleNode(PropertyType<E> property, boolean isRequired) {
			if (isRequired) {
				String name = constraintNameGenerator.generateName(Constraint.Type.EXISTS,
					Collections.singleton(property));
				return Collections.singletonList(
					Constraint.forNode(schemaNames.get(0).getValue()).named(name).exists(property.getName()));
			}

			String name = indexNameGenerator.generateName(Index.Type.PROPERTY, Collections.singleton(property));
			return Collections.singletonList(
				Index.forNode(schemaNames.get(0).getValue()).named(name).onProperties(property.getName()));
		}

		@SuppressWarnings({"unchecked", "squid:S1452"})
		List<CatalogItem<?>> handleRelationship(PropertyType<E> property, boolean isRequired) {
			if (isRequired) {
				String name = constraintNameGenerator.generateName(Constraint.Type.EXISTS,
					Collections.singleton(property));
				return Collections.singletonList(
					Constraint.forRelationship(schemaNames.get(0).getValue()).named(name).exists(property.getName()));
			}

			String name = indexNameGenerator.generateName(Index.Type.PROPERTY, Collections.singleton(property));
			return Collections.singletonList(
				Index.forRelationship(schemaNames.get(0).getValue()).named(name).onProperties(property.getName()));
		}
	}

	@SuppressWarnings("squid:S110") // Not something we need or can do anything about (Number of parents)
	class PropertySelector extends ElementKindVisitor8<PropertyType<NodeType>, DefaultNodeType> {

		private final Set<Element> requiredAnnotations;

		PropertySelector(Set<Element> requiredAnnotations) {
			this.requiredAnnotations = requiredAnnotations;
		}

		@Override
		public PropertyType<NodeType> visitType(TypeElement e, DefaultNodeType owner) {
			Optional<PropertyType<NodeType>> property = e.getEnclosedElements().stream()
				.filter(ee -> ee.getKind() == ElementKind.FIELD)
				.map(ee -> ee.accept(this, owner))
				.filter(Objects::nonNull)
				.findFirst();

			return property.orElseGet(
				() -> findSuperclassOfInterest(e).map(superclass -> superclass.accept(this, owner))
					.orElseThrow(() -> {
						String annotations = requiredAnnotations.stream()
							.map(Element::asType).map(TypeMirror::toString)
							.collect(Collectors.joining(", "));
						String msg = String.format("No property with any of the required annotations (%s) was found on %s", annotations, e);
						return new IllegalStateException(msg);
					}));
		}

		@SuppressWarnings("unchecked")
		@Override
		public PropertyType<NodeType> visitVariableAsField(VariableElement e, DefaultNodeType owner) {

			boolean requiredAnnotationPresent = e.getAnnotationMirrors().stream()
				.map(AnnotationMirror::getAnnotationType)
				.map(DeclaredType::asElement)
				.anyMatch(requiredAnnotations::contains);
			return requiredAnnotationPresent ? owner.addProperty(e.getSimpleName().toString()) : null;
		}
	}

	/**
	 * Visitor that computes if an SDN 6 annotated type requires a primary key constraint.
	 */
	@SuppressWarnings("squid:S110") // Not something we need or can do anything about (Number of parents)
	class RequiresPrimaryKeyConstraintPredicate extends ElementKindVisitor8<Boolean, Boolean> {

		private final Collection<TypeElement> idAnnotations;

		private final TypeElement generatedValueAnnotation;

		private final String generatorAttributeName;

		private final String internalIdGeneratorClass;

		RequiresPrimaryKeyConstraintPredicate(Collection<TypeElement> idAnnotations, TypeElement generatedValueAnnotation, String generatorAttributeName, String internalIdGeneratorClass) {
			this.idAnnotations = idAnnotations;
			this.generatedValueAnnotation = generatedValueAnnotation;
			this.generatorAttributeName = generatorAttributeName;
			this.internalIdGeneratorClass = internalIdGeneratorClass;
		}

		@Override
		protected Boolean defaultAction(Element e, Boolean aBoolean) {
			return aBoolean;
		}

		@Override
		public Boolean visitType(TypeElement e, Boolean includeAbstractClasses) {
			boolean isNonAbstractClass = e.getKind().isClass() && !e.getModifiers().contains(Modifier.ABSTRACT);
			if (!isNonAbstractClass && Boolean.FALSE.equals(includeAbstractClasses)) {
				return false;
			}
			if (e.getEnclosedElements().stream().noneMatch(ee -> ee.accept(this, false))) {
				return findSuperclassOfInterest(e).map(superclass -> superclass.accept(this, true)).orElse(false);
			}
			return true;
		}

		@Override
		public Boolean visitVariableAsField(VariableElement e, Boolean defaultValue) {

			Set<Element> fieldAnnotations = e.getAnnotationMirrors().stream()
				.map(AnnotationMirror::getAnnotationType).map(DeclaredType::asElement).collect(Collectors.toSet());
			if (fieldAnnotations.stream().noneMatch(idAnnotations::contains)) {
				return defaultValue;
			}
			return e.getAnnotationMirrors().stream()
				.filter(m -> m.getAnnotationType().asElement().equals(generatedValueAnnotation))
				.noneMatch(generatedValue -> isUsingInternalIdGenerator(e, generatedValue));
		}

		private boolean isUsingInternalIdGenerator(VariableElement e, AnnotationMirror generatedValue) {

			Map<String, ? extends AnnotationValue> values = generatedValue
				.getElementValues().entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), Map.Entry::getValue));

			DeclaredType generatorClassValue = values.containsKey(generatorAttributeName) ?
				(DeclaredType) values.get(generatorAttributeName).getValue() : null;
			DeclaredType valueValue = values.containsKey(ATTRIBUTE_VALUE) ?
				(DeclaredType) values.get(ATTRIBUTE_VALUE).getValue() : null;

			String name = null;
			if (generatorClassValue != null && valueValue != null && !generatorClassValue.equals(valueValue)) {
				messager.printMessage(
					Diagnostic.Kind.ERROR,
					String.format("Different @AliasFor mirror values for annotation [%s]!", generatedValue.getAnnotationType()),
					e
				);
			} else if (generatorClassValue != null) {
				name = generatorClassValue.toString();
			} else if (valueValue != null) {
				name = valueValue.toString();
			}

			// The defaults will not be materialized
			return (name == null || internalIdGeneratorClass.equals(name)) && VALID_GENERATED_ID_TYPES.contains(e.asType().toString());
		}
	}
}
