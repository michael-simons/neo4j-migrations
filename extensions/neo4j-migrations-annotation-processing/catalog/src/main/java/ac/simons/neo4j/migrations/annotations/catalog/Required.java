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
package ac.simons.neo4j.migrations.annotations.catalog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark one or more fields of a class as unique properties for a given node
 * or relationship. You will want to use it on a type for building composite constraints
 * (constraints spanning more than one field) and on single fields for single property
 * constraints.
 * <p>
 * When is this annotation useful?
 * <ul>
 * <li>You have a plain Java application with some models but no object mapper on the
 * classpath and you still want to derive content</li>
 * <li>You are a Spring Data Neo4j (SDN6+) user and want to enrich your models with unique
 * or existential constraints</li>
 * <li>You are an Neo4j-OGM user but are looking for an object mapper agnostic way of
 * enriching your model</li>
 * </ul>
 * <p>
 *
 * <i>Determining labels or types:</i> Depending on your approach and combination with
 * other annotations (such as SDN6+ or OGM annotations), the process is as following
 * <ul>
 * <li>Explicitly use {@link #label()} or {@link #type()} ()}</li>
 * <li>On a plain class: The simple class name is used as label (we default to targeting
 * nodes)</li>
 * <li>If combined any of the following SDN6+ annotations
 * {@code org.springframework.data.neo4j.core.schema.Node} or
 * {@code org.springframework.data.neo4j.core.schema.RelationshipProperties}, we default
 * to the SDN6 approach</li>
 * <li>If combined with an Neo4j-OGM annotation, we default to the Neo4j-OGM approach</li>
 * </ul>
 * <i>Note:</i> We follow the above algorithm strictly, and we don't try to resolve
 * conflicts. If in doubt, the processor will fail hard
 * <p>
 * <i>Determining node or relationship:</i> We default to nodes and labels. If you want to
 * generate constraints for relationships, you must either combine this annotation with
 * one of {@code org.springframework.data.neo4j.core.schema.RelationshipProperties} from
 * SDN6+ or {@code org.neo4j.ogm.annotation.Relationship} or you can explicitly use the
 * {@link #type()} attribute of this annotation.
 * <p>
 * This annotation can be used to derive catalogs that contain constraints that are only
 * available in enterprise edition. We don't check this during generation. Please make
 * sure that your targeted database suits those constraints.
 * <p>
 * This annotation can also be repeated on a class. If you decide to put contradicting
 * information on a class, for example targeting labels with one annotation and
 * relationships with another, we won't stop you as long as we can resolve both error
 * free: You might end up with a catalog that defines several constraints coming from one
 * type and aiming for both nodes and relationship constraints.
 *
 * @author Michael J. Simons
 * @since 1.15.0
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Documented
public @interface Required {

	/**
	 * If this is not {@literal null}, it has precedence over {@link #type()} and the
	 * simple class name, but not over dedicated OGM or SDN6 annotations explicitly
	 * specifying the label or type explicitly. Its use must be consistent throughout the
	 * class.
	 * @return the target label
	 */
	String label() default "";

	/**
	 * Can be used to specify the type (and therefor a relationship target), has no
	 * precedence over dedicated OGM or SDN6 annotations with explicit label or type
	 * values.
	 * @return the target (relationship) type
	 */
	String type() default "";

	/**
	 * {@return the property required to exist on the given label or type}
	 */
	String property() default "";

}
