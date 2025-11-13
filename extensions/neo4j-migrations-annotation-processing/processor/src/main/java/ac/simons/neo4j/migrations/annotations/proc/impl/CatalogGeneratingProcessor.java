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
package ac.simons.neo4j.migrations.annotations.proc.impl;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractAnnotationValueVisitor8;
import javax.lang.model.util.ElementKindVisitor8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import ac.simons.neo4j.migrations.annotations.proc.CatalogNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.ConstraintNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.ElementType;
import ac.simons.neo4j.migrations.annotations.proc.IndexNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.annotations.proc.SchemaName;
import ac.simons.neo4j.migrations.annotations.proc.impl.DefaultSchemaName.Target;
import ac.simons.neo4j.migrations.core.Neo4jEdition;
import ac.simons.neo4j.migrations.core.Neo4jVersion;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;

/**
 * Processes SDN and OGM entities and generates migration catalogs from them.
 *
 * @author Michael J. Simons
 * @since 1.11.0
 */
@SupportedAnnotationTypes({ FullyQualifiedNames.SDN6_NODE, FullyQualifiedNames.OGM_NODE,
		FullyQualifiedNames.OGM_RELATIONSHIP_ENTITY, FullyQualifiedNames.CATALOG_REQUIRED,
		FullyQualifiedNames.CATALOG_UNIQUE, FullyQualifiedNames.CATALOG_UNIQUE_PROPERTIES,
		FullyQualifiedNames.CATALOG_INDEX })
@SupportedOptions({ CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_CATALOG,
		CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_CONSTRAINTS,
		CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_INDEXES,
		CatalogGeneratingProcessor.OPTION_NAME_GENERATOR_OPTIONS, CatalogGeneratingProcessor.OPTION_OUTPUT_DIR,
		CatalogGeneratingProcessor.OPTION_TIMESTAMP, CatalogGeneratingProcessor.OPTION_DEFAULT_CATALOG_NAME,
		CatalogGeneratingProcessor.OPTION_ADD_RESET, CatalogGeneratingProcessor.OPTION_GENERATE_TYPE_CONSTRAINTS })
public final class CatalogGeneratingProcessor extends AbstractProcessor {

	static final String OPTION_NAME_GENERATOR_CATALOG = "org.neo4j.migrations.catalog_generator.catalog_name_generator";
	static final String OPTION_NAME_GENERATOR_CONSTRAINTS = "org.neo4j.migrations.catalog_generator.constraint_name_generator";
	static final String OPTION_NAME_GENERATOR_INDEXES = "org.neo4j.migrations.catalog_generator.index_name_generator";
	static final String OPTION_NAME_GENERATOR_OPTIONS = "org.neo4j.migrations.catalog_generator.naming_options";
	static final String OPTION_OUTPUT_DIR = "org.neo4j.migrations.catalog_generator.output_dir";
	static final String OPTION_TIMESTAMP = "org.neo4j.migrations.catalog_generator.timestamp";
	static final String OPTION_DEFAULT_CATALOG_NAME = "org.neo4j.migrations.catalog_generator.default_catalog_name";
	static final String OPTION_ADD_RESET = "org.neo4j.migrations.catalog_generator.add_reset";
	static final String OPTION_GENERATE_TYPE_CONSTRAINTS = "org.neo4j.migrations.catalog_generator.generate_type_constraints";
	static final String DEFAULT_MIGRATION_NAME = "Create_schema_from_domain_objects.xml";
	static final String DEFAULT_HEADER_FMT = "This file was generated by Neo4j-Migrations at %s.";

	static final Set<String> VALID_GENERATED_ID_TYPES = Set.of(Long.class.getName(), long.class.getName());

	private final Set<CatalogItem<?>> catalogItems = new LinkedHashSet<>();

	private CatalogNameGenerator catalogNameGenerator;

	private ConstraintNameGenerator constraintNameGenerator;

	private IndexNameGenerator indexNameGenerator;

	private Messager messager;

	private Types typeUtils;

	private ElementsSDN6 sdn6;

	private ElementsOGM ogm;

	private ElementsCatalog catalog;

	private boolean addReset;

	private Clock clock = Clock.systemDefaultZone();

	private boolean generateTypeConstraints;

	/**
	 * Creates a new instance of this processor. It should not be necessary to call this
	 * directly, it will be done automatically by Javac.
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

	private <T> T nameGenerator(String optionName, Class<T> expectedType, Supplier<T> defaultSupplier,
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
			}
			else {
				return expectedType.cast(Class.forName(fqn).getConstructor(Map.class).newInstance(generatorOptions));
			}
		}
		catch (ClassCastException | InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException | ClassNotFoundException ex) {
			this.messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
					"Could not load `" + fqn + "`, using default for " + optionName);
			return defaultSupplier.get();
		}
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {

		super.init(processingEnv);

		this.messager = processingEnv.getMessager();

		Map<String, String> options = processingEnv.getOptions();
		if (this.catalogNameGenerator == null) {
			this.catalogNameGenerator = nameGenerator(OPTION_NAME_GENERATOR_CATALOG, CatalogNameGenerator.class, () -> {
				String defaultName = options.getOrDefault(OPTION_DEFAULT_CATALOG_NAME, DEFAULT_MIGRATION_NAME);
				return () -> defaultName;
			}, options);
		}

		if (this.constraintNameGenerator == null) {
			this.constraintNameGenerator = nameGenerator(OPTION_NAME_GENERATOR_CONSTRAINTS,
					ConstraintNameGenerator.class, DefaultConstraintNameGenerator::new, options);
		}

		if (this.indexNameGenerator == null) {
			this.indexNameGenerator = nameGenerator(OPTION_NAME_GENERATOR_INDEXES, IndexNameGenerator.class,
					DefaultIndexNameGenerator::new, options);
		}

		Elements elementUtils = processingEnv.getElementUtils();

		this.sdn6 = ElementsSDN6.of(elementUtils).orElse(null);
		this.ogm = ElementsOGM.of(elementUtils).orElse(null);
		this.catalog = ElementsCatalog.of(elementUtils).orElse(null);

		this.typeUtils = processingEnv.getTypeUtils();

		this.addReset = Boolean.parseBoolean(options.getOrDefault(OPTION_ADD_RESET, "false"));
		this.generateTypeConstraints = Boolean
			.parseBoolean(options.getOrDefault(OPTION_GENERATE_TYPE_CONSTRAINTS, "false"));

		String timestamp = options.get(OPTION_TIMESTAMP);
		if (timestamp != null && !timestamp.isEmpty()) {
			ZonedDateTime z = ZonedDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestamp));
			this.clock = Clock.fixed(z.toInstant(), z.getZone());
		}
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		if (roundEnv.processingOver()) {
			try {
				FileObject fileObject = this.processingEnv.getFiler()
					.createResource(StandardLocation.SOURCE_OUTPUT, "",
							getOutputDir() + this.catalogNameGenerator.getCatalogName());
				try (OutputStream out = new BufferedOutputStream(fileObject.openOutputStream())) {
					Renderer<Catalog> renderer = Renderer.get(Renderer.Format.XML, Catalog.class);
					Neo4jVersion neo4jVersion = Neo4jVersion.LATEST;
					XMLRenderingOptionsImpl o = new XMLRenderingOptionsImpl(true, this.addReset,
							String.format(DEFAULT_HEADER_FMT,
									ZonedDateTime.now(this.clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
					RenderConfig config = RenderConfig.create()
						.forVersionAndEdition(neo4jVersion, Neo4jEdition.ENTERPRISE)
						.withAdditionalOptions(Collections.singletonList(o));

					renderer.render(Catalog.of(this.catalogItems), config, out);
				}
			}
			catch (IOException ex) {
				this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
			}
		}
		else if (!annotations.isEmpty()) {

			if (this.sdn6 != null) {
				processSDN6IdAnnotations(roundEnv);
			}
			if (this.ogm != null) {
				processOGMIdAnnotations(roundEnv);
				processOGMIndexAnnotations(roundEnv, this.ogm.node());
				processOGMIndexAnnotations(roundEnv, this.ogm.relationshipEntity());
				processOGMCompositeIndexAnnotations(roundEnv);
				processOGMPropertyTypes(roundEnv);
			}
			if (this.catalog != null) {
				processCatalogAnnotations(roundEnv);
			}
		}

		return true;
	}

	private void processOGMPropertyTypes(RoundEnvironment roundEnv) {

		if (!this.generateTypeConstraints) {
			return;
		}

		roundEnv.getElementsAnnotatedWith(this.ogm.node()).stream().map(TypeElement.class::cast).forEach(t -> {
			var labels = computeLabelsOGM(t);
			var owner = new DefaultNodeType(t.getQualifiedName().toString(), labels);
			this.catalogItems.addAll(t.accept(new PropertyTypeConstraintGenerator<>(Mode.OGM, labels), owner));
		});

		roundEnv.getElementsAnnotatedWith(this.ogm.relationshipEntity())
			.stream()
			.map(TypeElement.class::cast)
			.forEach(t -> {
				var type = computeTypeOGM(t);
				var owner = new DefaultRelationshipType(t.getQualifiedName().toString(), type);
				this.catalogItems
					.addAll(t.accept(new PropertyTypeConstraintGenerator<>(Mode.OGM, List.of(type)), owner));
			});
	}

	private void processCatalogAnnotations(RoundEnvironment roundEnv) {

		// Keep them ordered by element
		Map<Element, Set<CatalogItem<?>>> items = new LinkedHashMap<>();
		Map<Element, Set<SchemaName>> existingLabelsAndTypes = new HashMap<>();

		UnaryOperator<Element> enclosingOrSelf = element -> (element.getKind() == ElementKind.FIELD)
				? element.getEnclosingElement() : element;
		for (Element element : roundEnv.getElementsAnnotatedWithAny(this.catalog.unique(),
				this.catalog.uniqueWrapper())) {
			Element enclosingElement = enclosingOrSelf.apply(element);
			items.computeIfAbsent(enclosingElement, ignored -> new LinkedHashSet<>())
				.addAll(processCatalogAnnotation(enclosingElement, element, this.catalog.unique(),
						this.catalog.uniqueWrapper(),
						existingLabelsAndTypes.computeIfAbsent(enclosingElement, ignore -> new HashSet<>())));
		}

		for (Element element : roundEnv.getElementsAnnotatedWith(this.catalog.required())) {
			Element enclosingElement = enclosingOrSelf.apply(element);
			items.computeIfAbsent(enclosingElement, ignored -> new LinkedHashSet<>())
				.addAll(processCatalogAnnotation(enclosingElement, element, this.catalog.required(), null,
						existingLabelsAndTypes.computeIfAbsent(enclosingElement, ignore -> new HashSet<>())));
		}

		for (Element element : roundEnv.getElementsAnnotatedWith(this.catalog.index())) {
			Element enclosingElement = enclosingOrSelf.apply(element);
			items.computeIfAbsent(enclosingElement, ignored -> new LinkedHashSet<>())
				.addAll(processCatalogAnnotation(enclosingElement, element, this.catalog.index(), null,
						existingLabelsAndTypes.computeIfAbsent(enclosingElement, ignore -> new HashSet<>())));
		}

		items.values().forEach(this.catalogItems::addAll);
	}

	private boolean isSDNOrOGMAnnotated(Element annotationType) {
		return annotationType.equals(this.sdn6.node()) || annotationType.equals(this.sdn6.relationshipProperties())
				|| annotationType.equals(this.ogm.node()) || annotationType.equals(this.ogm.relationshipEntity());
	}

	private boolean isCatalogAnnotated(Element annotationType) {
		return annotationType.equals(this.catalog.unique()) || annotationType.equals(this.catalog.uniqueWrapper())
				|| annotationType.equals(this.catalog.required());
	}

	/**
	 * Processes one catalog annotation.
	 * @param enclosingElement the enclosing element of the annotated element
	 * @param element the element on which the annotation was found
	 * @param existingLabelsAndTypes labels and types that have been previously discovered
	 * for the enclosing element
	 * @param annotationType the annotation processed
	 * @param wrapperType wrapper annotation if available
	 * @return a collection of catalog items
	 */
	@SuppressWarnings({ "squid:S6204" }) // toList vs Collectors.collect
	private Collection<CatalogItem<?>> processCatalogAnnotation(Element enclosingElement, Element element,
			TypeElement annotationType, TypeElement wrapperType, Set<SchemaName> existingLabelsAndTypes) {

		Mode mode;
		Target target;

		List<? extends AnnotationMirror> enclosingAnnotations = enclosingElement.getAnnotationMirrors();
		Predicate<Element> isCatalog = this::isCatalogAnnotated;

		Set<Element> annotationsPresent = enclosingAnnotations.stream()
			.map(am -> am.getAnnotationType().asElement())
			.filter(isCatalog.negate())
			.collect(Collectors.toSet());
		List<SchemaName> labels;

		if ((this.ogm == null && this.sdn6 == null)
				|| annotationsPresent.stream().noneMatch(this::isSDNOrOGMAnnotated)) {
			labels = List.of(DefaultSchemaName.label(enclosingElement.getSimpleName().toString()));
			mode = Mode.PURE;
			target = Target.UNDEFINED;
		}
		else if (this.ogm != null && this.sdn6 != null
				&& annotationsPresent.containsAll(Set.of(this.sdn6.node(), this.ogm.node()))) {
			this.messager.printMessage(Diagnostic.Kind.ERROR,
					"Mixing SDN and OGM annotations on the same class is not supported", element);
			return Collections.emptyList();
		}
		else if (this.sdn6 != null && annotationsPresent.contains(this.sdn6.node())) {
			labels = computeLabelsSDN6((TypeElement) enclosingElement);
			mode = Mode.SDN6;
			target = Target.NODE;
		}
		else if (this.sdn6 != null && annotationsPresent.contains(this.sdn6.relationshipProperties())) {
			labels = List.of(DefaultSchemaName
				.type(Identifiers.deriveRelationshipType(enclosingElement.getSimpleName().toString())));
			mode = Mode.SDN6;
			target = Target.REL;
		}
		else if (this.ogm != null && annotationsPresent.contains(this.ogm.relationshipEntity())) {
			labels = List.of(computeTypeOGM((TypeElement) enclosingElement));
			mode = Mode.OGM;
			target = Target.REL;
		}
		else {
			labels = computeLabelsOGM((TypeElement) enclosingElement);
			mode = Mode.OGM;
			target = Target.NODE;
		}

		if (target == Target.REL && annotationType.equals(this.catalog.unique())) {
			this.messager.printMessage(Diagnostic.Kind.ERROR, "Unique constraints on relationships are not supported",
					enclosingElement);
			return Collections.emptyList();
		}

		return element.getAnnotationMirrors()
			.stream()
			.flatMap(am -> optionalUnwrapWrapper(annotationType, wrapperType, element, am))
			.map(annotationMirror -> processCatalogAnnotation0(enclosingElement, element, annotationMirror, mode,
					target, labels, existingLabelsAndTypes))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private Stream<AnnotationMirror> optionalUnwrapWrapper(TypeElement annotationType, TypeElement wrapperType,
			Element element, AnnotationMirror am) {

		Element annotationElement = am.getAnnotationType().asElement();
		if (annotationElement.equals(annotationType)) {
			return Stream.of(am);
		}
		else if (annotationElement.equals(wrapperType)) {
			return element.getAnnotationMirrors()
				.stream()
				.filter(nested -> nested.getAnnotationType().asElement().equals(wrapperType))
				.flatMap(x -> {
					Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = x.getElementValues();
					return attributes.get(Attributes.get(wrapperType, Attributes.VALUE).orElseThrow())
						.accept(new WrappedAnnotationExtractor(), null)
						.stream();
				});
		}
		return Stream.empty();
	}

	@SuppressWarnings({ "squid:S6204", "squid:S1452" }) // toList vs Collectors.collect,
														// generic wildcard
	private CatalogItem<?> processCatalogAnnotation0(Element enclosingElement, Element annotatedElement,
			AnnotationMirror annotation, Mode mode, Target target, List<SchemaName> identifiersOfEnclosingElement,
			Set<SchemaName> existingIdentifiers) {

		TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
		Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = annotation.getElementValues();

		Set<String> propertyNames = extractPropertyNames(annotatedElement, mode, annotationType, attributes);
		if (propertyNames.isEmpty()) {
			return null;
		}

		AnnotationValue annotationValueLabel = Attributes.get(annotationType, Attributes.LABEL)
			.map(attributes::get)
			.orElse(null);
		AnnotationValue annotationValueType = Attributes.get(annotationType, Attributes.TYPE)
			.map(attributes::get)
			.orElse(null);
		if (annotationValueLabel != null && annotationValueType != null) {
			this.messager.printMessage(Diagnostic.Kind.ERROR, "Ambiguous annotation " + annotation, annotatedElement);
		}
		else if (target == Target.REL && annotationValueLabel != null) {
			this.messager.printMessage(Diagnostic.Kind.ERROR, "Overwriting explicit type with a label is not supported",
					annotatedElement);
		}
		else if (target == Target.NODE && annotationValueType != null) {
			this.messager.printMessage(Diagnostic.Kind.ERROR, "Overwriting explicit label with a type is not supported",
					annotatedElement);
		}
		AnnotationValue annotationValue = (target == Target.REL) ? annotationValueType : annotationValueLabel;
		List<SchemaName> labelsUsed = mergeIdentifier(enclosingElement, mode, target, identifiersOfEnclosingElement,
				existingIdentifiers, annotationValue);
		if (labelsUsed.isEmpty()) {
			return null;
		}
		existingIdentifiers.addAll(labelsUsed);

		DefaultNodeType node = new DefaultNodeType(((TypeElement) enclosingElement).getQualifiedName().toString(),
				labelsUsed);

		List<PropertyType<?>> properties = propertyNames.stream().map(node::addProperty).collect(Collectors.toList());

		String firstIdentifier = labelsUsed.get(0).getValue();
		if (annotationType == this.catalog.unique()) {
			String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE, properties);
			return Constraint.forNode(firstIdentifier).named(name).unique(propertyNames.toArray(String[]::new));
		}
		else if (annotationType == this.catalog.required()) {
			String name = this.constraintNameGenerator.generateName(Constraint.Type.EXISTS, properties);
			String propertyName = propertyNames.stream().findFirst().orElseThrow();
			return (target == Target.REL) ? Constraint.forRelationship(firstIdentifier).named(name).exists(propertyName)
					: Constraint.forNode(firstIdentifier).named(name).exists(propertyName);
		}
		else if (annotationType == this.catalog.index()) {
			Index.Type type = Attributes.get(annotationType, Attributes.INDEX_TYPE)
				.map(attributes::get)
				.map(AnnotationValue::getValue)
				.map(Object::toString)
				.map(Index.Type::valueOf)
				.orElse(Index.Type.PROPERTY);
			String name = this.indexNameGenerator.generateName(type, properties);
			Index.NamedSingleIdentifierBuilder builder = (target == Target.REL)
					? Index.forRelationship(firstIdentifier).named(name) : Index.forNode(firstIdentifier).named(name);
			Index index = switch (type) {
				case PROPERTY -> builder.onProperties(propertyNames.toArray(String[]::new));
				case FULLTEXT -> builder.fulltext(propertyNames.toArray(String[]::new));
				case TEXT -> builder.text(propertyNames.stream().findFirst().orElseThrow());
				default -> throw new UnsupportedOperationException("Unknown index type");
			};

			AnnotationValue annotationOptionsType = Attributes.get(annotationType, Attributes.OPTIONS)
				.map(attributes::get)
				.orElse(null);
			if (annotationOptionsType != null) {
				List<?> options = (List<?>) annotationOptionsType.getValue();
				String optionString = options.stream()
					.map(AnnotationMirror.class::cast)
					.map(this::extractOption)
					.filter(Objects::nonNull)
					.collect(Collectors.joining(", "));
				index = index.withOptions(optionString);
			}
			return index;
		}

		return null;
	}

	private String extractOption(AnnotationMirror annotation) {
		TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
		Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = annotation.getElementValues();
		AnnotationValue annotationKeyType = Attributes.get(annotationType, Attributes.KEY)
			.map(attributes::get)
			.orElse(null);

		AnnotationValue annotationValueType = Attributes.get(annotationType, Attributes.VALUE)
			.map(attributes::get)
			.orElse(null);

		if (annotationKeyType == null || annotationValueType == null) {
			return null;
		}
		String key = (String) annotationKeyType.getValue();
		String value = (String) annotationValueType.getValue();

		return key + ": " + value;
	}

	private Set<String> extractPropertyNames(Element annotatedElement, Mode mode, TypeElement annotationType,
			Map<? extends ExecutableElement, ? extends AnnotationValue> attributes) {

		ExecutableElement propertiesAttribute = Attributes.get(annotationType, Attributes.PROPERTIES)
			.or(() -> Attributes.get(annotationType, Attributes.PROPERTY))
			.orElseThrow();
		String fieldName = (annotatedElement.getKind() == ElementKind.FIELD)
				? annotatedElement.getSimpleName().toString() : null;

		// Extract the actual properties
		Set<String> propertyNames = new HashSet<>();
		if (attributes.containsKey(propertiesAttribute)) {
			var value = attributes.get(propertiesAttribute).getValue();
			if (value instanceof List<?> annotationValues) {
				annotationValues.stream()
					.map(AnnotationValue.class::cast)
					.map(AnnotationValue::getValue)
					.map(String.class::cast)
					.forEach(propertyNames::add);
			}
			else if (value instanceof String stringValue) {
				propertyNames.add(stringValue);
			}
		}

		Optional<String> additionalName = switch (mode) {
			case SDN6 -> extractPropertyName(annotatedElement, this.sdn6.property());
			case OGM -> extractPropertyName(annotatedElement, this.ogm.property());
			case PURE -> Optional.empty();
		};

		if (propertyNames.isEmpty()) {
			propertyNames.add(additionalName.orElse(fieldName));
		}
		else if (additionalName.isPresent() && !propertyNames.contains(additionalName.get())) {
			this.messager.printMessage(Diagnostic.Kind.ERROR,
					String.format("Contradicting properties: %s vs %s",
							propertyNames.stream().collect(Collectors.joining(",", "(", ")")), additionalName.get()),
					annotatedElement);
		}
		else if (annotatedElement.getKind() == ElementKind.FIELD && propertyNames.size() > 1) {
			this.messager.printMessage(Diagnostic.Kind.ERROR,
					"Please annotate the class and not a field for composite constraints.", annotatedElement);
		}

		return propertyNames;
	}

	private List<SchemaName> mergeIdentifier(Element enclosingElement, Mode mode, Target target,
			List<SchemaName> identifiersOfEnclosingElement, Set<SchemaName> existingIdentifiers,
			AnnotationValue annotationValue) {

		if (annotationValue == null && mode != Mode.PURE) {
			return identifiersOfEnclosingElement;
		}

		SchemaName explicitName;
		if (annotationValue == null) {
			explicitName = identifiersOfEnclosingElement.get(0);
		}
		else {
			String value = (String) annotationValue.getValue();
			explicitName = (target == Target.NODE) ? DefaultSchemaName.label(value) : DefaultSchemaName.type(value);
		}
		SchemaName simpleName = DefaultSchemaName.label(enclosingElement.getSimpleName().toString());
		if (target == Target.REL) {
			simpleName = DefaultSchemaName.type(Identifiers.deriveRelationshipType(simpleName.getValue()));
		}

		List<SchemaName> result = new ArrayList<>();
		if (mode == Mode.PURE && !existingIdentifiers.isEmpty() && !existingIdentifiers.contains(explicitName)) {
			String val1 = Stream.concat(existingIdentifiers.stream(), Stream.of(explicitName))
				.map(SchemaName::getValue)
				.map(v -> String.format("`%s`", v))
				.collect(Collectors.joining(", "));
			this.messager.printMessage(Diagnostic.Kind.ERROR, String.format("Contradicting labels found: %s", val1),
					enclosingElement);
		}
		else if (mode != Mode.PURE && !simpleName.equals(identifiersOfEnclosingElement.get(0))) {
			this.messager.printMessage(Diagnostic.Kind.ERROR,
					String.format("Explicit identifier `%s` on class contradicts identifier on annotation: `%s`",
							identifiersOfEnclosingElement.get(0).getValue(), explicitName.getValue()),
					enclosingElement);
		}
		else {
			result.add(explicitName);
		}

		return result;
	}

	private Optional<String> extractPropertyName(Element annotatedElement, TypeElement annotation) {
		List<? extends AnnotationMirror> annotations = annotatedElement.getAnnotationMirrors();

		return annotations.stream()
			.filter(am -> am.getAnnotationType().asElement().equals(annotation))
			.findFirst()
			.flatMap(p -> {
				Map<String, ? extends AnnotationValue> values = p.getElementValues()
					.entrySet()
					.stream()
					.collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), Map.Entry::getValue));

				String nameValue = values.containsKey(Attributes.NAME) ? (String) values.get(Attributes.NAME).getValue()
						: null;
				String valueValue = values.containsKey(Attributes.VALUE)
						? (String) values.get(Attributes.VALUE).getValue() : null;

				if (nameValue != null && valueValue != null && !nameValue.equals(valueValue)) {
					this.messager.printMessage(Diagnostic.Kind.ERROR,
							String.format("Different @AliasFor or @ValueFor mirror values for annotation [%s]!",
									p.getAnnotationType()),
							annotatedElement);
				}
				else if (nameValue != null) {
					return Optional.of(nameValue);
				}
				else if (valueValue != null) {
					return Optional.of(valueValue);
				}
				return Optional.empty();
			});
	}

	private void processSDN6IdAnnotations(RoundEnvironment roundEnv) {

		Set<Element> supportedSDN6Annotations = new HashSet<>();
		supportedSDN6Annotations.add(this.sdn6.id());
		supportedSDN6Annotations.add(this.sdn6.commonsId());

		roundEnv.getElementsAnnotatedWith(this.sdn6.node()).stream().map(TypeElement.class::cast).forEach(t -> {
			List<SchemaName> labels = computeLabelsSDN6(t);
			DefaultNodeType owner = new DefaultNodeType(t.getQualifiedName().toString(), labels);
			if (requiresPrimaryKeyConstraintSDN6(t)) {
				PropertyType<NodeType> idProperty = t
					.accept(new IdPropertySelector(Mode.SDN6, supportedSDN6Annotations), owner);
				String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
						Collections.singleton(idProperty));
				this.catalogItems
					.add(Constraint.forNode(labels.get(0).getValue()).named(name).unique(idProperty.getName()));
			}
			if (this.generateTypeConstraints) {
				this.catalogItems.addAll(t.accept(new PropertyTypeConstraintGenerator<>(Mode.SDN6, labels), owner));
			}
		});
	}

	private void processOGMIdAnnotations(RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(this.ogm.node())
			.stream()
			.filter(this::requiresPrimaryKeyConstraintOGM)
			.map(TypeElement.class::cast)
			.forEach(t -> {
				List<SchemaName> labels = computeLabelsOGM(t);
				PropertyType<NodeType> idProperty = t.accept(
						new IdPropertySelector(Mode.OGM, Collections.singleton(this.ogm.id())),
						new DefaultNodeType(t.getQualifiedName().toString(), labels));
				String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
						Collections.singleton(idProperty));
				this.catalogItems.add(
						Constraint
							.forNode(labels.stream()
								.map(SchemaName::getValue)
								.findFirst()
								.orElseGet(() -> t.getSimpleName().toString()))
							.named(name)
							.unique(idProperty.getName()));
			});
	}

	private void processOGMIndexAnnotations(RoundEnvironment roundEnv, TypeElement processedAnnotation) {
		roundEnv.getElementsAnnotatedWith(processedAnnotation).stream().map(TypeElement.class::cast).forEach(t -> {
			List<SchemaName> labels = Optional.of(this.ogm)
				.map(ElementsOGM::node)
				.filter(v -> v == processedAnnotation)
				.map(v -> computeLabelsOGM(t))
				.orElseGet(() -> Collections.singletonList(computeTypeOGM(t)));
			this.catalogItems.addAll(t.accept(new OGMIndexVisitor<>(labels, processedAnnotation),
					new DefaultNodeType(t.getQualifiedName().toString(), labels)));
		});
	}

	private void processOGMCompositeIndexAnnotations(RoundEnvironment roundEnv) {

		if (this.ogm == null) {
			return;
		}

		Set<? extends Element> nodes = roundEnv.getElementsAnnotatedWith(this.ogm.node());
		Set<? extends Element> composeIndexNodes = roundEnv.getElementsAnnotatedWith(this.ogm.compositeIndex());
		Set<? extends Element> composeIndexesNodes = roundEnv.getElementsAnnotatedWith(this.ogm.compositeIndexes());
		Predicate<Element> elementsAnnotatedWithCompositeIndex = composeIndexNodes::contains;
		elementsAnnotatedWithCompositeIndex = elementsAnnotatedWithCompositeIndex.or(composeIndexesNodes::contains);
		nodes.stream()
			.filter(elementsAnnotatedWithCompositeIndex)
			.map(TypeElement.class::cast)
			.forEach(t -> this.catalogItems.addAll(computeOGMCompositeIndexes(t)));
	}

	@SuppressWarnings({ "unchecked", "squid:S1452", "squid:S6204" })
	private Collection<CatalogItem<?>> computeOGMCompositeIndexes(TypeElement typeElement) {

		List<SchemaName> labels = computeLabelsOGM(typeElement);
		DefaultNodeType node = new DefaultNodeType(typeElement.getQualifiedName().toString(), labels);
		return typeElement.getAnnotationMirrors().stream().flatMap(m -> {
			if (m.getAnnotationType().asElement().equals(this.ogm.compositeIndex())) {
				return Stream.of(m);
			}
			else if (m.getAnnotationType().asElement().equals(this.ogm.compositeIndexes())) {
				List<AnnotationValue> values = (List<AnnotationValue>) m.getElementValues()
					.get(this.ogm.compositeIndexesValue())
					.getValue();
				return values.stream().map(AnnotationValue::getValue).map(AnnotationMirror.class::cast);
			}
			return Stream.empty();
		}).map(t -> {
			Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
			List<AnnotationValue> values = new ArrayList<>();
			if (attributes.containsKey(this.ogm.compositeIndexValue())) {
				values.addAll((List<AnnotationValue>) attributes.get(this.ogm.compositeIndexValue()).getValue());
			}
			if (attributes.containsKey(this.ogm.compositeIndexProperties())) {
				values.addAll((List<AnnotationValue>) attributes.get(this.ogm.compositeIndexProperties()).getValue());
			}

			boolean isUnique = attributes.containsKey(this.ogm.compositeIndexUnique())
					&& (boolean) attributes.get(this.ogm.compositeIndexUnique()).getValue();
			List<PropertyType<?>> properties = values.stream()
				.map(v -> node.addProperty((String) v.getValue()))
				.collect(Collectors.toList());
			String[] propertyNames = properties.stream().map(PropertyType::getName).toArray(String[]::new);

			if (propertyNames.length == 0) {
				this.messager.printMessage(Diagnostic.Kind.ERROR,
						String.format("Cannot use %s without any properties on %s",
								FullyQualifiedNames.OGM_COMPOSITE_INDEX, typeElement),
						typeElement);
				return null;
			}

			CatalogItem<?> item;
			if (isUnique) {
				String name = this.constraintNameGenerator.generateName(Constraint.Type.KEY, properties);
				item = Constraint.forNode(node.getLabels().get(0).getValue()).named(name).key(propertyNames);
			}
			else {
				String indexName = this.indexNameGenerator.generateName(Index.Type.PROPERTY, properties);
				item = Index.forNode(node.getLabels().get(0).getValue()).named(indexName).onProperties(propertyNames);
			}
			return item;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private boolean requiresPrimaryKeyConstraintSDN6(Element e) {
		Collection<TypeElement> idAnnotations = Arrays.asList(this.sdn6.id(), this.sdn6.commonsId());
		TypeElement generatedValueAnnotation = this.sdn6.generatedValue();
		String generatorAttributeName = "generatorClass";
		String internalIdGeneratorClass = "org.springframework.data.neo4j.core.schema.GeneratedValue.InternalIdGenerator";
		return e.accept(new RequiresPrimaryKeyConstraintPredicate(idAnnotations, generatedValueAnnotation,
				generatorAttributeName, internalIdGeneratorClass), false);
	}

	private boolean requiresPrimaryKeyConstraintOGM(Element e) {
		Collection<TypeElement> idAnnotations = Arrays.asList(this.ogm.id());
		TypeElement generatedValueAnnotation = this.ogm.generatedValue();
		String generatorAttributeName = "strategy";
		String internalIdGeneratorClass = "org.neo4j.ogm.id.InternalIdStrategy";
		return e.accept(new RequiresPrimaryKeyConstraintPredicate(idAnnotations, generatedValueAnnotation,
				generatorAttributeName, internalIdGeneratorClass), false);
	}

	/**
	 * Computes SDN 6 labels.
	 * @param typeElement the type element to process
	 * @return an ordered list so that we can pass it to any API that requires the labels
	 * to be in order (primary first).
	 */
	@SuppressWarnings("unchecked")
	private List<SchemaName> computeLabelsSDN6(TypeElement typeElement) {

		Set<SchemaName> result = new LinkedHashSet<>();
		BiConsumer<Boolean, AnnotationMirror> computation = (addSimpleName, t) -> {
			Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
			if (attributes.containsKey(this.sdn6.nodePrimaryLabel())) {
				result.add(DefaultSchemaName.label((String) attributes.get(this.sdn6.nodePrimaryLabel()).getValue()));
			}

			List<AnnotationValue> values = new ArrayList<>();
			if (attributes.containsKey(this.sdn6.nodeValue())) {
				values.addAll((List<AnnotationValue>) attributes.get(this.sdn6.nodeValue()).getValue());
			}
			if (attributes.containsKey(this.sdn6.nodeLabels())) {
				values.addAll((List<AnnotationValue>) attributes.get(this.sdn6.nodeLabels()).getValue());
			}
			values.stream().map(v -> DefaultSchemaName.label((String) v.getValue())).forEach(result::add);

			if (result.isEmpty() && Boolean.TRUE.equals(addSimpleName)) {
				result.add(DefaultSchemaName.label(typeElement.getSimpleName().toString()));
			}
		};
		traverseClassHierarchy(this.sdn6.node(), typeElement, computation, true);
		return new ArrayList<>(result);
	}

	private List<SchemaName> computeLabelsOGM(TypeElement typeElement) {
		return computeOGMModel(this.ogm.node(), typeElement, DefaultSchemaName::label, UnaryOperator.identity(),
				this.ogm.nodeLabel(), this.ogm.nodeValue());
	}

	private SchemaName computeTypeOGM(TypeElement typeElement) {
		List<SchemaName> names = computeOGMModel(this.ogm.relationshipEntity(), typeElement, DefaultSchemaName::type,
				Identifiers::deriveRelationshipType, this.ogm.relationshipType(), this.ogm.relationshipValue());
		if (names.size() != 1) {
			this.messager.printMessage(Diagnostic.Kind.ERROR,
					String.format("More than one relationship type found on %s", typeElement), typeElement);
		}
		return names.get(0);
	}

	/**
	 * Computes thhe OGM model.
	 * @param entityAnnotation the entity type to travers
	 * @param typeElement the type element to process
	 * @param schemaNameProvider factory for schema names.
	 * @param simpleNameFilter filter for the simple name
	 * @param selectedAttributes the attributes of the annotation to process
	 * @return an ordered list so that we can pass it to any API that requires the labels
	 * to be in order (primary first).
	 */
	private List<SchemaName> computeOGMModel(TypeElement entityAnnotation, TypeElement typeElement,
			Function<String, SchemaName> schemaNameProvider, UnaryOperator<String> simpleNameFilter,
			ExecutableElement... selectedAttributes) {

		Set<SchemaName> result = new LinkedHashSet<>();
		BiConsumer<Boolean, AnnotationMirror> labelComputation = (addSimpleName, t) -> {
			Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
			for (ExecutableElement selectedAttribute : selectedAttributes) {
				if (attributes.containsKey(selectedAttribute)) {
					result.add(schemaNameProvider.apply((String) attributes.get(selectedAttribute).getValue()));
				}
			}

			if (result.isEmpty() && Boolean.TRUE.equals(addSimpleName)) {
				result.add(schemaNameProvider.apply(simpleNameFilter.apply(typeElement.getSimpleName().toString())));
			}
		};
		traverseClassHierarchy(entityAnnotation, typeElement, labelComputation, true);
		return new ArrayList<>(result);
	}

	private void traverseClassHierarchy(TypeElement requiredAnnotation, TypeElement typeElement,
			BiConsumer<Boolean, AnnotationMirror> consumer, boolean addSimpleName) {
		typeElement.getAnnotationMirrors()
			.stream()
			.filter(m -> m.getAnnotationType().asElement().equals(requiredAnnotation))
			.findFirst()
			.ifPresent(v -> consumer.accept(addSimpleName, v));

		typeElement.getInterfaces()
			.stream()
			.map(i -> this.typeUtils.asElement(i))
			.map(TypeElement.class::cast)
			.forEach(i -> traverseClassHierarchy(requiredAnnotation, i, consumer, false));

		findSuperclassOfInterest(typeElement)
			.ifPresent(superclass -> traverseClassHierarchy(requiredAnnotation, superclass, consumer, false));
	}

	private Optional<TypeElement> findSuperclassOfInterest(TypeElement typeElement) {
		return Optional.ofNullable(typeElement.getSuperclass())
			.map(this.typeUtils::asElement)
			.map(TypeElement.class::cast)
			.filter(e -> !e.getQualifiedName().contentEquals("java.lang.Object"));
	}

	private String getOutputDir() {

		String subDir = this.processingEnv.getOptions().getOrDefault(OPTION_OUTPUT_DIR, "neo4j-migrations");
		if (!subDir.endsWith("/")) {
			subDir += "/";
		}
		return subDir;
	}

	/**
	 * Added as a simple indicator what set of annotations are processed.
	 */
	enum Mode {

		PURE, SDN6, OGM

	}

	/**
	 * Helper to extract values of wrapper annotations for repeatable ones.
	 */
	static class WrappedAnnotationExtractor extends AbstractAnnotationValueVisitor8<List<AnnotationMirror>, Void> {

		private final List<AnnotationMirror> wrappedAnnotations = new ArrayList<>();

		@Override
		public List<AnnotationMirror> visitBoolean(boolean b, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitByte(byte b, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitChar(char c, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitDouble(double d, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitFloat(float f, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitInt(int i, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitLong(long i, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitShort(short s, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitString(String s, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitType(TypeMirror t, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitEnumConstant(VariableElement c, Void unused) {
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitAnnotation(AnnotationMirror a, Void unused) {
			this.wrappedAnnotations.add(a);
			return this.wrappedAnnotations;
		}

		@Override
		public List<AnnotationMirror> visitArray(List<? extends AnnotationValue> vals, Void unused) {
			vals.forEach(val -> val.accept(this, null));
			return this.wrappedAnnotations;
		}

	}

	@SuppressWarnings("squid:S110") // Not something we need or can do anything about
									// (Number of parents)
	class PropertyTypeConstraintGenerator<E extends ElementType<E>>
			extends ElementKindVisitor8<List<CatalogItem<?>>, WriteableElementType<E>> {

		private final Mode mode;

		private final List<SchemaName> labels;

		PropertyTypeConstraintGenerator(Mode mode, List<SchemaName> labels) {
			this.mode = mode;
			this.labels = labels;
		}

		@Override
		public List<CatalogItem<?>> visitType(TypeElement e, WriteableElementType<E> owner) {

			return e.getEnclosedElements()
				.stream()
				.map(ee -> ee.accept(this, owner))
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.toList();
		}

		@Override
		public List<CatalogItem<?>> visitRecordComponent(RecordComponentElement e, WriteableElementType<E> owner) {

			return checkIfEligibleForPropertyTypeConstraint(e, owner);
		}

		@Override
		public List<CatalogItem<?>> visitVariableAsField(VariableElement e, WriteableElementType<E> owner) {

			return checkIfEligibleForPropertyTypeConstraint(e, owner);
		}

		List<CatalogItem<?>> checkIfEligibleForPropertyTypeConstraint(Element e, WriteableElementType<E> owner) {
			var type = e.asType();
			if (isTransient(e) || !TypesEligibleForPropertyTypeConstraints.contains(type)) {
				return List.of();
			}

			Optional<String> additionalName = switch (this.mode) {
				case SDN6 -> extractPropertyName(e, CatalogGeneratingProcessor.this.sdn6.property());
				case OGM -> extractPropertyName(e, CatalogGeneratingProcessor.this.ogm.property());
				case PURE -> Optional.empty();
			};
			var property = owner.addProperty(additionalName.orElseGet(() -> e.getSimpleName().toString()));
			var name = CatalogGeneratingProcessor.this.constraintNameGenerator
				.generateName(Constraint.Type.PROPERTY_TYPE, List.of(property));
			return List.of(Constraint.forNode(this.labels.get(0).getValue())
				.named(name)
				.type(property.getName(), TypesEligibleForPropertyTypeConstraints.get(type)));
		}

		boolean isTransient(Element ex) {
			Predicate<AnnotationMirror> sdnTransient = (CatalogGeneratingProcessor.this.sdn6 != null)
					? a -> a.getAnnotationType()
						.asElement()
						.equals(CatalogGeneratingProcessor.this.sdn6.transientProperty())
					: a -> false;
			Predicate<AnnotationMirror> ogmTransient = (CatalogGeneratingProcessor.this.ogm != null)
					? a -> a.getAnnotationType()
						.asElement()
						.equals(CatalogGeneratingProcessor.this.ogm.transientProperty())
					: a -> false;
			return ex.getAnnotationMirrors().stream().anyMatch(sdnTransient.or(ogmTransient))
					|| ex.getModifiers().contains(Modifier.TRANSIENT);
		}

	}

	@SuppressWarnings("squid:S110") // Not something we need or can do anything about
									// (Number of parents)
	class OGMIndexVisitor<E extends ElementType<E>>
			extends ElementKindVisitor8<List<CatalogItem<?>>, WriteableElementType<E>> {

		private final List<SchemaName> schemaNames;

		private final boolean isRelationship;

		OGMIndexVisitor(List<SchemaName> schemaNames, TypeElement processedAnnotation) {
			this.schemaNames = schemaNames;
			this.isRelationship = processedAnnotation == CatalogGeneratingProcessor.this.ogm.relationshipEntity();
		}

		@Override
		public List<CatalogItem<?>> visitType(TypeElement e, WriteableElementType<E> owner) {
			return e.getEnclosedElements()
				.stream()
				.map(ee -> ee.accept(this, owner))
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.toList();
		}

		@Override
		public List<CatalogItem<?>> visitVariableAsField(VariableElement e, WriteableElementType<E> owner) {

			List<? extends AnnotationMirror> indexAnnotations = e.getAnnotationMirrors().stream().filter(a -> {
				Element element = a.getAnnotationType().asElement();
				return element.equals(CatalogGeneratingProcessor.this.ogm.index())
						|| element.equals(CatalogGeneratingProcessor.this.ogm.required());
			}).toList();

			if (indexAnnotations.isEmpty()) {
				return Collections.emptyList();
			}

			boolean isUnique = indexAnnotations.stream().anyMatch(a -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = a.getElementValues();
				if (attributes.containsKey(CatalogGeneratingProcessor.this.ogm.indexUnique())) {
					return (boolean) attributes.get(CatalogGeneratingProcessor.this.ogm.indexUnique()).getValue();
				}
				return false;
			});

			if (isUnique && this.isRelationship) {
				CatalogGeneratingProcessor.this.messager.printMessage(Diagnostic.Kind.ERROR,
						String.format("Unique constraints defined at %s are not allowed on relationships",
								e.getEnclosingElement()),
						e.getEnclosingElement());
				return Collections.emptyList();
			}

			PropertyType<E> property = owner.addProperty(e.getSimpleName().toString());
			if (isUnique) {
				String name = CatalogGeneratingProcessor.this.constraintNameGenerator
					.generateName(Constraint.Type.UNIQUE, Collections.singleton(property));
				return Collections.singletonList(
						Constraint.forNode(this.schemaNames.get(0).getValue()).named(name).unique(property.getName()));
			}

			boolean isRequired = indexAnnotations.stream()
				.anyMatch(
						a -> a.getAnnotationType().asElement().equals(CatalogGeneratingProcessor.this.ogm.required()));
			if (this.isRelationship) {
				return handleRelationship(property, isRequired);
			}
			return handleNode(property, isRequired);
		}

		@SuppressWarnings("squid:S1452")
		List<CatalogItem<?>> handleNode(PropertyType<E> property, boolean isRequired) {
			if (isRequired) {
				String name = CatalogGeneratingProcessor.this.constraintNameGenerator
					.generateName(Constraint.Type.EXISTS, Collections.singleton(property));
				return Collections.singletonList(
						Constraint.forNode(this.schemaNames.get(0).getValue()).named(name).exists(property.getName()));
			}

			String name = CatalogGeneratingProcessor.this.indexNameGenerator.generateName(Index.Type.PROPERTY,
					Collections.singleton(property));
			return Collections.singletonList(
					Index.forNode(this.schemaNames.get(0).getValue()).named(name).onProperties(property.getName()));
		}

		@SuppressWarnings({ "unchecked", "squid:S1452" })
		List<CatalogItem<?>> handleRelationship(PropertyType<E> property, boolean isRequired) {
			if (isRequired) {
				String name = CatalogGeneratingProcessor.this.constraintNameGenerator
					.generateName(Constraint.Type.EXISTS, Collections.singleton(property));
				return Collections.singletonList(Constraint.forRelationship(this.schemaNames.get(0).getValue())
					.named(name)
					.exists(property.getName()));
			}

			String name = CatalogGeneratingProcessor.this.indexNameGenerator.generateName(Index.Type.PROPERTY,
					Collections.singleton(property));
			return Collections.singletonList(Index.forRelationship(this.schemaNames.get(0).getValue())
				.named(name)
				.onProperties(property.getName()));
		}

	}

	@SuppressWarnings("squid:S110") // Not something we need or can do anything about
									// (Number of parents)
	class IdPropertySelector extends ElementKindVisitor8<PropertyType<NodeType>, DefaultNodeType> {

		private final Mode mode;

		private final Set<Element> requiredAnnotations;

		IdPropertySelector(Mode mode, Set<Element> requiredAnnotations) {
			this.mode = mode;
			this.requiredAnnotations = requiredAnnotations;
		}

		@Override
		public PropertyType<NodeType> visitType(TypeElement e, DefaultNodeType owner) {
			Optional<PropertyType<NodeType>> property = e.getEnclosedElements()
				.stream()
				.filter(ee -> ee.getKind() == ElementKind.FIELD)
				.map(ee -> ee.accept(this, owner))
				.filter(Objects::nonNull)
				.findFirst();

			return property
				.orElseGet(() -> findSuperclassOfInterest(e).map(superclass -> superclass.accept(this, owner))
					.orElseThrow(() -> {
						String annotations = this.requiredAnnotations.stream()
							.map(Element::asType)
							.map(TypeMirror::toString)
							.collect(Collectors.joining(", "));
						String msg = String.format(
								"No property with any of the required annotations (%s) was found on %s", annotations,
								e);
						return new IllegalStateException(msg);
					}));
		}

		@SuppressWarnings("unchecked")
		@Override
		public PropertyType<NodeType> visitVariableAsField(VariableElement e, DefaultNodeType owner) {

			boolean requiredAnnotationPresent = e.getAnnotationMirrors()
				.stream()
				.map(AnnotationMirror::getAnnotationType)
				.map(DeclaredType::asElement)
				.anyMatch(this.requiredAnnotations::contains);

			if (!requiredAnnotationPresent) {
				return null;
			}

			Optional<String> additionalName = switch (this.mode) {
				case SDN6 -> extractPropertyName(e, CatalogGeneratingProcessor.this.sdn6.property());
				case OGM -> extractPropertyName(e, CatalogGeneratingProcessor.this.ogm.property());
				case PURE -> Optional.empty();
			};

			return owner.addProperty(additionalName.orElseGet(() -> e.getSimpleName().toString()));
		}

	}

	/**
	 * Visitor that computes if an SDN 6 annotated type requires a primary key constraint.
	 */
	@SuppressWarnings("squid:S110") // Not something we need or can do anything about
									// (Number of parents)
	class RequiresPrimaryKeyConstraintPredicate extends ElementKindVisitor8<Boolean, Boolean> {

		private final Collection<TypeElement> idAnnotations;

		private final TypeElement generatedValueAnnotation;

		private final String generatorAttributeName;

		private final String internalIdGeneratorClass;

		private final Set<TypeElement> processed = new HashSet<>();

		RequiresPrimaryKeyConstraintPredicate(Collection<TypeElement> idAnnotations,
				TypeElement generatedValueAnnotation, String generatorAttributeName, String internalIdGeneratorClass) {
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
			if (!this.processed.add(e)) {
				return false;
			}

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
		public Boolean visitRecordComponent(RecordComponentElement e, Boolean defaultValue) {

			return checkFieldOrRecordComponent(e, defaultValue);
		}

		@Override
		public Boolean visitVariableAsField(VariableElement e, Boolean defaultValue) {

			return checkFieldOrRecordComponent(e, defaultValue);
		}

		private boolean checkFieldOrRecordComponent(Element e, Boolean defaultValue) {

			Set<Element> fieldAnnotations = e.getAnnotationMirrors()
				.stream()
				.map(AnnotationMirror::getAnnotationType)
				.map(DeclaredType::asElement)
				.collect(Collectors.toSet());
			if (fieldAnnotations.stream().noneMatch(this.idAnnotations::contains)) {
				return defaultValue;
			}
			return e.getAnnotationMirrors()
				.stream()
				.filter(m -> m.getAnnotationType().asElement().equals(this.generatedValueAnnotation))
				.noneMatch(generatedValue -> isUsingInternalIdGenerator(e, generatedValue));
		}

		private boolean isUsingInternalIdGenerator(Element e, AnnotationMirror generatedValue) {

			Map<String, ? extends AnnotationValue> values = generatedValue.getElementValues()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), Map.Entry::getValue));

			DeclaredType generatorClassValue = values.containsKey(this.generatorAttributeName)
					? (DeclaredType) values.get(this.generatorAttributeName).getValue() : null;
			DeclaredType valueValue = values.containsKey(Attributes.VALUE)
					? (DeclaredType) values.get(Attributes.VALUE).getValue() : null;

			String name = null;
			if (generatorClassValue != null && valueValue != null && !generatorClassValue.equals(valueValue)) {
				CatalogGeneratingProcessor.this.messager.printMessage(Diagnostic.Kind.ERROR,
						String.format("Different @AliasFor mirror values for annotation [%s]!",
								generatedValue.getAnnotationType()),
						e);
			}
			else if (generatorClassValue != null) {
				name = generatorClassValue.toString();
			}
			else if (valueValue != null) {
				name = valueValue.toString();
			}

			// The defaults will not be materialized
			return (name == null || this.internalIdGeneratorClass.equals(name))
					&& VALID_GENERATED_ID_TYPES.contains(e.asType().toString());
		}

	}

}
