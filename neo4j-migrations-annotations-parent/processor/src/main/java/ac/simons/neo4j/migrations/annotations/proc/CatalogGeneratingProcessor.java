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
package ac.simons.neo4j.migrations.annotations.proc;

import ac.simons.neo4j.migrations.annotations.proc.support.PrimaryKeyNameGenerator;
import ac.simons.neo4j.migrations.schema.Label;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
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

	private PrimaryKeyNameGenerator primaryKeyNameGenerator;

	private TypeElement sdn6Node;
	private ExecutableElement sdn6NodeValue;
	private ExecutableElement sdn6NodePrimaryLabel;

	public CatalogGeneratingProcessor() {
	}

	CatalogGeneratingProcessor(PrimaryKeyNameGenerator primaryKeyNameGenerator) {
		this.primaryKeyNameGenerator = primaryKeyNameGenerator;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		if (this.primaryKeyNameGenerator == null) {
			this.primaryKeyNameGenerator = new DefaultPrimaryKeyNameGenerator();
		}

		Elements elementUtils = processingEnv.getElementUtils();
		this.sdn6Node = elementUtils.getTypeElement(FullyQualifiedNames.SDN6_NODE);
		this.sdn6NodeValue = (ExecutableElement) this.sdn6Node.getEnclosedElements().stream()
			.filter(e -> e.getSimpleName().contentEquals("value")).findFirst()
			.orElseThrow(IllegalStateException::new);
		this.sdn6NodePrimaryLabel = (ExecutableElement) this.sdn6Node.getEnclosedElements().stream()
			.filter(e -> e.getSimpleName().contentEquals("primaryLabel")).findFirst()
			.orElseThrow(IllegalStateException::new);
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
				.filter(e -> e.getKind().isClass())
				.map(TypeElement.class::cast)
				.forEach(t -> {

					DefaultNodeType nodeType = new DefaultNodeType(computeLabels(t));
					DefaultNodePropertyType propertyType = new DefaultNodePropertyType(nodeType, "todo");
					this.primaryKeyNameGenerator.generateName(t.getQualifiedName(), propertyType);
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

	List<Label> computeLabels(TypeElement typeElement) {
		Optional<Label> primaryLabel = typeElement.getAnnotationMirrors()
			.stream()
			.filter(m -> m.getAnnotationType().asElement().equals(sdn6Node))
			.findFirst()
			.flatMap(t -> {
				Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = t.getElementValues();
				if (elementValues.containsKey(sdn6NodePrimaryLabel)) {
					return Optional.of(Label.of((String) elementValues.get(sdn6NodePrimaryLabel).getValue()));
				}
				return Optional.empty();
			});

		return Collections.singletonList(
			primaryLabel.orElseGet(() -> Label.of(typeElement.getSimpleName().toString())));
	}

	String getOutputDir() {

		String subDir = processingEnv.getOptions().getOrDefault(OPTION_OUTPUT_DIR, "neo4j/migrations/");
		if (!subDir.endsWith("/")) {
			subDir += "/";
		}
		return subDir;
	}

}
