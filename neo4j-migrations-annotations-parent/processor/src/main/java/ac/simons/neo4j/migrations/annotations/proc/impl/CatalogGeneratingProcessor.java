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
import ac.simons.neo4j.migrations.core.catalog.Constraint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
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

	private ConstraintNameGenerator constraintNameGenerator;

	private Types typeUtils;

	private TypeElement sdn6Node;
	private ExecutableElement sdn6NodeValue;
	private ExecutableElement sdn6NodeLabels;
	private ExecutableElement sdn6NodePrimaryLabel;

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
				.filter(e -> e.getKind().isClass() && !e.getModifiers().contains(Modifier.ABSTRACT))
				.map(TypeElement.class::cast)
				.forEach(t -> {

					DefaultNodeType nodeType = new DefaultNodeType(t.getQualifiedName().toString(), computeLabels(t, true));
					DefaultNodePropertyType propertyType = new DefaultNodePropertyType(nodeType, "todo");
					this.constraintNameGenerator.generateName(Constraint.Type.UNIQUE, propertyType);
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

	@SuppressWarnings("unchecked")
	List<Label> computeLabels(TypeElement typeElement, boolean addSimpleName) {

		List<Label> result = new ArrayList<>();
		typeElement.getAnnotationMirrors().stream()
			.filter(m -> m.getAnnotationType().asElement().equals(sdn6Node))
			.findFirst()
			.ifPresent(t -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = t.getElementValues();
				if (elementValues.containsKey(sdn6NodePrimaryLabel)) {
					result.add(Label.of((String) elementValues.get(sdn6NodePrimaryLabel).getValue()));

				}

				List<AnnotationValue> values = new ArrayList<>();
				if (elementValues.containsKey(sdn6NodeValue)) {
					values.addAll((List<AnnotationValue>) elementValues.get(sdn6NodeValue).getValue());
				}
				if (elementValues.containsKey(sdn6NodeLabels)) {
					values.addAll((List<AnnotationValue>) elementValues.get(sdn6NodeLabels).getValue());
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
		TypeElement superclass = (TypeElement) typeUtils.asElement(typeElement.getSuperclass());
		if (superclass != null && !superclass.getQualifiedName().contentEquals("java.lang.Object")) {
			result.addAll(computeLabels(superclass, false));
		}
		return result;
	}

	String getOutputDir() {

		String subDir = processingEnv.getOptions().getOrDefault(OPTION_OUTPUT_DIR, "neo4j/migrations/");
		if (!subDir.endsWith("/")) {
			subDir += "/";
		}
		return subDir;
	}

}
