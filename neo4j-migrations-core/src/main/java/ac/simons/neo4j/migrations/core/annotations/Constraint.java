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
package ac.simons.neo4j.migrations.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ac.simons.neo4j.migrations.core.catalog.Constraint.Type;

/**
 * Reassembles the {@code ac.simons.neo4j.migrations.core.catalog.Constraint} catalog item. This annotation can be
 * used on both a type and on fields. You will want to use it on a type for building composite constraints (constraints
 * spanning more than one field) and on single fields for single property constraints.
 * <p>
 * When is this annotation useful?
 * <ul>
 *     <li>You have a plain Java application with some models but no object mapper on the classpath but you still want
 *     to derive content</li>
 *     <li>You are a Spring Data Neo4j (SDN6+) user and want to enrich your models with unique or existential constraints</li>
 *     <li>You are an Neo4j-OGM user but are looking for an object mapper agnostic way of enriching your model </li>
 * </ul>
 * <p>
 *
 * <i>Determining labels or types:</i> Depending on your approach and combination with other annotations (such as SDN6+ or
 * OGM annotations), the process is as following
 * <ul>
 *     <li>Explicitly use {@link #label()} or {@link #relationshipType()}</li>
 *     <li>On a plain class: The simple class name is used as label (we default to targeting nodes)</li>
 *     <li>If combined any of the following SDN6+ annotations {@code org.springframework.data.neo4j.core.schema.Node} or
 *     {@code org.springframework.data.neo4j.core.schema.RelationshipProperties}</li>, we default to the SDN6 approach</li>
 *     <li>If combined with an Neo4j-OGM annotation, we default to the Neo4j-OGM approach</li>
 * </ul>
 * <i>Note:</i> We follow the above algorithm strictly, and we don't try to resolve conflicts. If in doubt, the processor
 * will fail hard
 * <p>
 * <i>Determining node or relationship:</i> We default to nodes and labels. If you want to generate constraints for
 * relationships, you must either combine this annotation with one of {@code org.springframework.data.neo4j.core.schema.RelationshipProperties}
 * from SDN6+ or {@code org.neo4j.ogm.annotation.Relationship} or you can explicitly use the
 * <p>
 * This annotation can be used to derive catalogs that contain constraints that are only available in enterprise edition.
 * We don't check this during generation. Please make sure that your targeted database suits those constraints.
 * <p>
 * This annotation can also be repeated on a class. Again, if you decide to put contradicting information on a class, for
 * example targeting labels with one annotation and relationships with another, we won't stop you as long as we can resolve
 * both error free: You might end up with a catalog that defines several constraints coming from one type and aiming for
 * both nodes and relationship constraints.
 *
 * @author Michael J. Simons
 * @soundtrack Juse Ju - Shibuya Crossing
 * @since 1.15.0
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
@Repeatable(Constraints.class)
public @interface Constraint {

	/**
	 * @return The type of this constraint.
	 */
	Type type();

	/**
	 * Please take note that not all types of constraints do support multiple properties and that not all versions of
	 * Neo4j will support composite constraints for all types.
	 *
	 * @return One or more properties to be included in this constraint
	 */
	String[] properties() default {};

	/**
	 * If this use not {@literal null}, it has precedence over {@link #relationshipType()} and any other annotation
	 * used on this class.
	 *
	 * @return The target label
	 */
	String label() default "";

	/**
	 * If this is not {@literal null} but {@link #type()} is, it has precedence over any other annotation used on this
	 * class
	 *
	 * @return The target (relationship) type
	 */
	String relationshipType() default "";
}
