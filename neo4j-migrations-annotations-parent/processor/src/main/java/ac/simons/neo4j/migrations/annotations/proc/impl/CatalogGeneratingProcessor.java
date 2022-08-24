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
import ac.simons.neo4j.migrations.annotations.proc.Label;
import ac.simons.neo4j.migrations.annotations.proc.NodeType;
import ac.simons.neo4j.migrations.annotations.proc.PropertyType;
import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
	FullyQualifiedNames.SDN6_NODE
})
@SupportedOptions({
	CatalogGeneratingProcessor.OPTION_OUTPUT_DIR,
	CatalogGeneratingProcessor.OPTION_WRITE_TIMESTAMP,
	CatalogGeneratingProcessor.OPTION_TIMESTAMP
})
public final class CatalogGeneratingProcessor extends AbstractProcessor {

	static final String OPTION_OUTPUT_DIR = "org.neo4j.migrations.catalog_generator.output_dir";
	static final String OPTION_WRITE_TIMESTAMP = "org.neo4j.migrations.catalog_generator.write_timestamp";
	static final String OPTION_TIMESTAMP = "org.neo4j.migrations.catalog_generator.timestamp";

	static final Set<String> VALID_GENERATED_ID_TYPES = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(Long.class.getName(), long.class.getName())));

	private ConstraintNameGenerator constraintNameGenerator;

	private Messager messager;
	private Types typeUtils;

	private TypeElement sdn6Node;
	private ExecutableElement sdn6NodeValue;
	private ExecutableElement sdn6NodeLabels;
	private ExecutableElement sdn6NodePrimaryLabel;

	private TypeElement sdn6Id;
	private TypeElement sdn6GeneratedValue;
	private TypeElement commonsId;

	private final List<Constraint> constraints = new ArrayList<>();

	public CatalogGeneratingProcessor() {
	}

	CatalogGeneratingProcessor(ConstraintNameGenerator constraintNameGenerator) {
		this.constraintNameGenerator = constraintNameGenerator;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		if (this.constraintNameGenerator == null) {
			this.constraintNameGenerator = new DefaultConstraintNameGenerator();
		}

		Elements elementUtils = processingEnv.getElementUtils();
		this.sdn6Node = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_NODE);
		this.sdn6NodeValue = getAnnotationAttribute(sdn6Node, "value");
		this.sdn6NodeLabels = getAnnotationAttribute(sdn6Node, "labels");
		this.sdn6NodePrimaryLabel = getAnnotationAttribute(sdn6Node, "primaryLabel");

		this.sdn6Id = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_ID);
		this.sdn6GeneratedValue = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_GENERATED_VALUE);
		this.commonsId = elementUtils.getTypeElement(FullyQualifiedNames.COMMONS_ID);

		this.messager = processingEnv.getMessager();
		this.typeUtils = processingEnv.getTypeUtils();
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
				/*
				String subDir = processingEnv.getOptions().getOrDefault(NATIVE_IMAGE_SUBDIR_OPTION, "");
				if (!(subDir.isEmpty() || subDir.endsWith("/"))) {
					subDir += "/";
				}
				String reflectionConfigPath = String.format("META-INF/native-image/%sreflection-config.json", subDir);
				 */
				FileObject fileObject = processingEnv.getFiler()
					.createResource(StandardLocation.CLASS_OUTPUT, "", getOutputDir() + "i_was_there.txt");
				try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fileObject.openOutputStream()))) {
					out.write("Hello");
				}
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
			}
		} else if (!annotations.isEmpty()) {

			roundEnv.getElementsAnnotatedWith(sdn6Node)
				.stream()
				.filter(this::requiresPrimaryKeyConstraint)
				.map(TypeElement.class::cast)
				.forEach(t -> {
					List<Label> labels = computeLabels(t, true);
					PropertyType<NodeType> idProperty = t.accept(new PropertyTypeExtractingVisitor(), new DefaultNodeType(t.getQualifiedName().toString(), labels));
					String name = this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE, Collections.singleton(idProperty));
					constraints.add(Constraint.forNode(labels.get(0).getValue()).named(name)
						.unique(idProperty.getName()));
				});


			/*
			roundEnv.getElementsAnnotatedWith(RegisterForReflection.class)
				.stream()
				.filter(e -> e.getKind().isClass() && registersElements(e.getAnnotation(RegisterForReflection.class)))
				.map(TypeElement.class::cast)
				.map(e -> {
					RegisterForReflection registerForReflection = e.getAnnotation(RegisterForReflection.class);
					Entry entry = new Entry(e.getQualifiedName().toString());
					entry.setAllDeclaredMethods(registerForReflection.allDeclaredMethods());
					entry.setAllDeclaredConstructors(registerForReflection.allDeclaredConstructors());
					return entry;
				})
				.forEach(entries::add);
			 */
			// do the magic
		}

		return true;
	}

	List<Constraint> getConstraints() {
		return Collections.unmodifiableList(constraints);
	}

	boolean requiresPrimaryKeyConstraint(Element e) {
		return e.accept(new RequiresPrimaryKeyConstraint(), false);
	}

	class PropertyTypeExtractingVisitor extends ElementKindVisitor8<PropertyType<NodeType>, DefaultNodeType> {

		@Override
		public PropertyType<NodeType> visitType(TypeElement e, DefaultNodeType nodeType) {
			Optional<PropertyType<NodeType>> property = e.getEnclosedElements().stream()
				.map(ee -> ee.accept(this, nodeType))
				.filter(Objects::nonNull)
				.findFirst();

			return property.orElseGet(() -> findSuperclassOfInterest(e).map(superclass -> superclass.accept(this, nodeType))
				.orElseThrow(() -> new IllegalStateException(
					"This visitor might only be used after a certain type has been marked for inclusion in the generated catalog.")));
		}

		@Override
		public PropertyType<NodeType> visitVariableAsField(VariableElement e, DefaultNodeType nodeType) {

			Set<Element> fieldAnnotations = e.getAnnotationMirrors().stream()
				.map(AnnotationMirror::getAnnotationType).map(DeclaredType::asElement).collect(Collectors.toSet());
			if (fieldAnnotations.contains(sdn6Id) || fieldAnnotations.contains(commonsId)) {
				return nodeType.addProperty(e.getSimpleName().toString());
			}
			return null;
		}
	}

	class RequiresPrimaryKeyConstraint extends ElementKindVisitor8<Boolean, Boolean> {

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

	@SuppressWarnings("unchecked")
	List<Label> computeLabels(TypeElement typeElement, boolean addSimpleName) {

		List<Label> result = new ArrayList<>();
		typeElement.getAnnotationMirrors().stream()
			.filter(m -> m.getAnnotationType().asElement().equals(sdn6Node))
			.findFirst()
			.ifPresent(t -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> attributes = t.getElementValues();
				if (attributes.containsKey(sdn6NodePrimaryLabel)) {
					result.add(Label.of((String) attributes.get(sdn6NodePrimaryLabel).getValue()));
				}

				List<AnnotationValue> values = new ArrayList<>();
				if (attributes.containsKey(sdn6NodeValue)) {
					values.addAll((List<AnnotationValue>) attributes.get(sdn6NodeValue).getValue());
				}
				if (attributes.containsKey(sdn6NodeLabels)) {
					values.addAll((List<AnnotationValue>) attributes.get(sdn6NodeLabels).getValue());
				}
				values.stream().map(v -> Label.of((String) v.getValue())).forEach(result::add);
			});

		if (result.isEmpty() && addSimpleName) {
			result.add(Label.of(typeElement.getSimpleName().toString()));
		}

		typeElement.getInterfaces()
			.stream()
			.map(i -> typeUtils.asElement(i))
			.map(TypeElement.class::cast)
			.forEach(i -> result.addAll(computeLabels(i, false)));

		findSuperclassOfInterest(typeElement).ifPresent(superclass -> result.addAll(computeLabels(superclass, false)));
		return result;
	}

	Optional<TypeElement> findSuperclassOfInterest(TypeElement typeElement) {
		return Optional.ofNullable(typeElement.getSuperclass())
			.map(typeUtils::asElement)
			.map(TypeElement.class::cast)
			.filter(e -> !e.getQualifiedName().contentEquals("java.lang.Object"));
	}

	String getOutputDir() {

		String subDir = processingEnv.getOptions().getOrDefault(OPTION_OUTPUT_DIR, "neo4j/migrations/");
		if (!subDir.endsWith("/")) {
			subDir += "/";
		}
		return subDir;
	}

}
