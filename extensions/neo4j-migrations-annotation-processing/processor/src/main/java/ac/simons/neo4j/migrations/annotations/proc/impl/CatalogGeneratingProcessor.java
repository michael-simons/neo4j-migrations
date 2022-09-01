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
import ac.simons.neo4j.migrations.annotations.proc.IndexNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.Label;
import ac.simons.neo4j.migrations.annotations.proc.CatalogNameGenerator;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Catalog;
import ac.simons.neo4j.migrations.core.catalog.CatalogItem;
import ac.simons.neo4j.migrations.core.catalog.Constraint;
import ac.simons.neo4j.migrations.core.catalog.Index;
import ac.simons.neo4j.migrations.core.catalog.RenderConfig;
import ac.simons.neo4j.migrations.core.catalog.Renderer;
import ac.simons.neo4j.migrations.core.internal.Neo4jEdition;
import ac.simons.neo4j.migrations.core.internal.Neo4jVersion;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementKindVisitor8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * @author Michael J. Simons
 * @soundtrack Moonbootica - ...And Then We Started To Dance
 * @since TBA
 */
@SupportedAnnotationTypes({
	FullyQualifiedNames.SDN6_NODE,
	FullyQualifiedNames.OGM_NODE
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
	private final Set<Element> supportedSDN6Annotations = new HashSet<>();

	private TypeElement ogmNode;
	private ExecutableElement ogmNodeValue;
	private ExecutableElement ogmNodeLabel;

	private TypeElement ogmIndex;
	private ExecutableElement ogmIndexUnique;

	private final List<CatalogItem<?>> catalogItems = new ArrayList<>();

	private boolean addReset;

	private Clock clock = Clock.systemDefaultZone();

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
		this.sdn6NodeValue = getAnnotationAttribute(sdn6Node, "value");
		this.sdn6NodeLabels = getAnnotationAttribute(sdn6Node, "labels");
		this.sdn6NodePrimaryLabel = getAnnotationAttribute(sdn6Node, "primaryLabel");

		this.sdn6Id = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_ID);
		this.sdn6GeneratedValue = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_GENERATED_VALUE);
		this.commonsId = elementUtils.getTypeElement(FullyQualifiedNames.COMMONS_ID);
		this.supportedSDN6Annotations.add(sdn6Id);
		this.supportedSDN6Annotations.add(commonsId);

		this.ogmNode = elementUtils.getTypeElement(FullyQualifiedNames.OGM_NODE);
		this.ogmNodeValue = getAnnotationAttribute(ogmNode, "value");
		this.ogmNodeLabel = getAnnotationAttribute(ogmNode, "label");

		this.ogmIndex = elementUtils.getTypeElement(FullyQualifiedNames.OGM_INDEX);
		this.ogmIndexUnique = getAnnotationAttribute(ogmIndex, "unique");

		this.typeUtils = processingEnv.getTypeUtils();

		this.addReset = Boolean.parseBoolean(options.getOrDefault(OPTION_ADD_RESET, "false"));
		String timestamp = options.get(OPTION_TIMESTAMP);
		if (timestamp != null && !timestamp.isEmpty()) {
			ZonedDateTime z = ZonedDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestamp));
			this.clock = Clock.fixed(z.toInstant(), z.getZone());
		}
	}

	static ExecutableElement getAnnotationAttribute(TypeElement annotation, String name) {
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

			roundEnv.getElementsAnnotatedWith(sdn6Node)
				.stream()
				.filter(this::requiresPrimaryKeyConstraintSDN6)
				.map(TypeElement.class::cast)
				.forEach(t -> {
					List<Label> labels = computeLabelsSDN6(t, true);
					PropertyType<NodeType> idProperty = t.accept(new SharedPropertyTypeSelector(supportedSDN6Annotations),
						new DefaultNodeType(t.getQualifiedName().toString(), labels));
					String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
						Collections.singleton(idProperty));
					catalogItems.add(Constraint.forNode(labels.get(0).getValue()).named(name)
						.unique(idProperty.getName()));
				});

			roundEnv.getElementsAnnotatedWith(ogmNode)
				.stream()
				.map(TypeElement.class::cast)
				.forEach(t -> {
					List<Label> labels = computeLabelsOGM(t, true);
					catalogItems.addAll(t.accept(new OGMIndexVisitor(labels),
						new DefaultNodeType(t.getQualifiedName().toString(), labels)));
					/*
					PropertyType<NodeType> idProperty = t.accept(new PropertyTypeExtractingVisitor(), new DefaultNodeType(t.getQualifiedName().toString(), labels));
					String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE, Collections.singleton(idProperty));
					constraints.add(Constraint.forNode(labels.get(0).getValue()).named(name)
						.unique(idProperty.getName()));
					*/
				});
		}

		return true;
	}

	List<CatalogItem<?>> getCatalogItems() {
		return Collections.unmodifiableList(catalogItems);
	}

	boolean requiresPrimaryKeyConstraintSDN6(Element e) {
		return e.accept(new SDN6RequiresPrimaryKeyConstraintPredicate(), false);
	}

	@SuppressWarnings("unchecked")
	List<Label> computeLabelsSDN6(TypeElement typeElement, boolean addSimpleName) {

		Set<Label> result = new LinkedHashSet<>();
		typeElement.getAnnotationMirrors().stream()
			.filter(m -> m.getAnnotationType().asElement().equals(sdn6Node))
			.findFirst()
			.ifPresent(t -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
				if (attributes.containsKey(sdn6NodePrimaryLabel)) {
					result.add(DefaultLabel.of((String) attributes.get(sdn6NodePrimaryLabel).getValue()));
				}

				List<AnnotationValue> values = new ArrayList<>();
				if (attributes.containsKey(sdn6NodeValue)) {
					values.addAll((List<AnnotationValue>) attributes.get(sdn6NodeValue).getValue());
				}
				if (attributes.containsKey(sdn6NodeLabels)) {
					values.addAll((List<AnnotationValue>) attributes.get(sdn6NodeLabels).getValue());
				}
				values.stream().map(v -> DefaultLabel.of((String) v.getValue())).forEach(result::add);
			});

		if (result.isEmpty() && addSimpleName) {
			result.add(DefaultLabel.of(typeElement.getSimpleName().toString()));
		}

		typeElement.getInterfaces()
			.stream()
			.map(i -> typeUtils.asElement(i))
			.map(TypeElement.class::cast)
			.forEach(i -> result.addAll(computeLabelsSDN6(i, false)));

		findSuperclassOfInterest(typeElement).ifPresent(
			superclass -> result.addAll(computeLabelsSDN6(superclass, false)));
		return new ArrayList<>(result);
	}

	List<Label> computeLabelsOGM(TypeElement typeElement, boolean addSimpleName) {

		Set<Label> result = new LinkedHashSet<>();
		typeElement.getAnnotationMirrors().stream()
			.filter(m -> m.getAnnotationType().asElement().equals(ogmNode))
			.findFirst()
			.ifPresent(t -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
				if (attributes.containsKey(ogmNodeLabel)) {
					result.add(DefaultLabel.of((String) attributes.get(ogmNodeLabel).getValue()));
				}
				if (attributes.containsKey(ogmNodeValue)) {
					result.add(DefaultLabel.of((String) attributes.get(ogmNodeValue).getValue()));
				}
			});

		if (result.isEmpty() && addSimpleName) {
			result.add(DefaultLabel.of(typeElement.getSimpleName().toString()));
		}

		typeElement.getInterfaces()
			.stream()
			.map(i -> typeUtils.asElement(i))
			.map(TypeElement.class::cast)
			.forEach(i -> result.addAll(computeLabelsOGM(i, false)));

		findSuperclassOfInterest(typeElement).ifPresent(
			superclass -> result.addAll(computeLabelsOGM(superclass, false)));
		return new ArrayList<>(result);
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

	class OGMIndexVisitor extends ElementKindVisitor8<List<CatalogItem<?>>, DefaultNodeType> {

		private final List<Label> labels;

		OGMIndexVisitor(List<Label> labels) {
			this.labels = labels;
		}

		@Override
		public List<CatalogItem<?>> visitType(TypeElement e, DefaultNodeType nodeType) {
			return e.getEnclosedElements().stream()
				.map(ee -> ee.accept(this, nodeType))
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		}

		@Override
		public List<CatalogItem<?>> visitVariableAsField(VariableElement e, DefaultNodeType nodeType) {

			List<? extends AnnotationMirror> indexAnnotations = e.getAnnotationMirrors().stream()
				.filter(a -> a.getAnnotationType().asElement().equals(ogmIndex))
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

			PropertyType<NodeType> property = nodeType.addProperty(e.getSimpleName().toString());
			if (isUnique) {
				String name = constraintNameGenerator.generateName(Constraint.Type.UNIQUE,
					Collections.singleton(property));
				return Collections.singletonList(
					Constraint.forNode(labels.get(0).getValue()).named(name).unique(property.getName()));
			}
			String name = indexNameGenerator.generateName(Index.Type.PROPERTY,
				Collections.singleton(property));
			return Collections.singletonList(
				Index.forNode(labels.get(0).getValue()).named(name).onProperties(property.getName()));
		}
	}

	class SharedPropertyTypeSelector extends ElementKindVisitor8<PropertyType<NodeType>, DefaultNodeType> {

		private final Set<Element> requiredAnnotations;

		SharedPropertyTypeSelector(Set<Element> requiredAnnotations) {
			this.requiredAnnotations = requiredAnnotations;
		}

		@Override
		public PropertyType<NodeType> visitType(TypeElement e, DefaultNodeType nodeType) {
			Optional<PropertyType<NodeType>> property = e.getEnclosedElements().stream()
				.map(ee -> ee.accept(this, nodeType))
				.filter(Objects::nonNull)
				.findFirst();

			return property.orElseGet(
				() -> findSuperclassOfInterest(e).map(superclass -> superclass.accept(this, nodeType))
					.orElseThrow(() -> new IllegalStateException(
						"This visitor might only be used after a certain type has been marked for inclusion in the generated catalog.")));
		}

		@Override
		public PropertyType<NodeType> visitVariableAsField(VariableElement e, DefaultNodeType nodeType) {

			boolean requiredAnnotationPresent = e.getAnnotationMirrors().stream()
				.map(AnnotationMirror::getAnnotationType)
				.map(DeclaredType::asElement)
				.anyMatch(requiredAnnotations::contains);
			return requiredAnnotationPresent ? nodeType.addProperty(e.getSimpleName().toString()) : null;
		}
	}

	/**
	 * Visitor that computes if an SDN 6 annotated type requires a primary key constraint.
	 */
	class SDN6RequiresPrimaryKeyConstraintPredicate extends ElementKindVisitor8<Boolean, Boolean> {

		@Override
		protected Boolean defaultAction(Element e, Boolean aBoolean) {
			return aBoolean;
		}

		@Override
		public Boolean visitType(TypeElement e, Boolean includeAbstractClasses) {
			boolean isNonAbstractClass = e.getKind().isClass() && !e.getModifiers().contains(Modifier.ABSTRACT);
			if (!isNonAbstractClass && !includeAbstractClasses) {
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
			if (!(fieldAnnotations.contains(sdn6Id) || fieldAnnotations.contains(commonsId))) {
				return defaultValue;
			}
			return e.getAnnotationMirrors().stream()
				.filter(m -> m.getAnnotationType().asElement().equals(sdn6GeneratedValue))
				.noneMatch(generatedValue -> isUsingInternalIdGenerator(e, generatedValue));
		}

		private boolean isUsingInternalIdGenerator(VariableElement e, AnnotationMirror generatedValue) {

			Map<String, ? extends AnnotationValue> values = generatedValue
				.getElementValues().entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().getSimpleName().toString(), Map.Entry::getValue));

			DeclaredType generatorClassValue = values.containsKey("generatorClass") ?
				(DeclaredType) values.get("generatorClass").getValue() : null;
			DeclaredType valueValue = values.containsKey("value") ?
				(DeclaredType) values.get("value").getValue() : null;

			String name = null;
			if (generatorClassValue != null && valueValue != null && !generatorClassValue.equals(valueValue)) {
				messager.printMessage(
					Diagnostic.Kind.ERROR,
					"Different @AliasFor mirror values for annotation [org.springframework.data.neo4j.core.schema.GeneratedValue]!",
					e
				);
			} else if (generatorClassValue != null) {
				name = generatorClassValue.toString();
			} else if (valueValue != null) {
				name = valueValue.toString();
			}

			// The defaults will not be materialized
			return
				(name == null || "org.springframework.data.neo4j.core.schema.GeneratedValue.InternalIdGenerator".equals(
					name))
				&& VALID_GENERATED_ID_TYPES.contains(e.asType().toString());
		}
	}
}
