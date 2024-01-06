/*
 * Copyright 2020-2024 the original author or authors.
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
 * @author shanon84
 * @since 2.8.2
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface Index {
	/**
	 * If this is not {@literal null} it has precedence over an implicit label (either no class annotations or one without
	 * a dedicated label) but not over OGM or SDN6 annotations specifying the label or type explicitly.
	 * Its use must be consistent throughout the class.
	 *
	 * @return The target label
	 */
	String label() default "";

	/**
	 * Use this if you want to define composite, Index when using {@link Index} on the class level.
	 * Leave it empty when using on field level, otherwise an exception will be thrown.
	 *
	 * @return The list of properties to include in the composite.
	 */
	String[] properties() default {};

	/**
	 * index type for the index.
	 * @return indexType
	 */
	Type indexType() default Type.PROPERTY;

	/**
	 * Use this if you want to have more configured options for your index.
	 * Be aware to activate the option to allow further options on index creation.
	 * Example:
	 * "`fulltext.analyzer`:whitespace"
	 * to set the fulltext analyzer for your index.
	 *
	 * @return array of options
	 */
	Option[] options() default {};

	/**
	 * An enumeration of possible index types.
	 */
	enum Type {
		/**
		 * An index on properties. The actual type depends on the options.
		 */
		PROPERTY,
		/**
		 * Fulltext indexes for long text properties.
		 */
		FULLTEXT,
		/**
		 * Text indexes for 4.4 and later.
		 */
		TEXT
	}

	/**
	 * key value based option for index creation.
	 */
	@interface Option {
		/**
		 * key string for the option
		 * @return key string
		 */
		String key() default "";

		/**
		 * value string for the option
		 * @return value string
		 */
		String value() default "";
	}
}
