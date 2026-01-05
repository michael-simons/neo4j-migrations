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
package ac.simons.neo4j.migrations.core.catalog;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ac.simons.neo4j.migrations.core.internal.Strings;
import ac.simons.neo4j.migrations.core.internal.XMLSchemaConstants;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.w3c.dom.Element;

/**
 * A somewhat Neo4j version independent representation of an index.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.7.0
 */
// This is about the additional field deconstructedIdentifiers. It is immutable and
// directly derived
// from the identifier in a one-to-one onto relationship and therefor ok.
@SuppressWarnings("squid:S2160")
public final class Index extends AbstractCatalogItem<Index.Type> {

	private static final String PROVIDER_PATTERN_FMT = "(?i)`?indexProvider`?\\s*:\\s*(['\"])%s-\\d\\.0(['\"])";

	private static final Pattern PROVIDER_PATTERN_RANGE = Pattern.compile(String.format(PROVIDER_PATTERN_FMT, "range"));

	private static final Pattern PROVIDER_PATTERN_BTREE = Pattern
		.compile(String.format(PROVIDER_PATTERN_FMT, "native-btree"));

	private static final String TOKEN_NAMES = "tokenNames";

	private static final String LABELS_OR_TYPES = "labelsOrTypes";

	private static final String[] NAME_KEYS = { "indexName", "name" };

	private static final String PROPERTIES_KEY = "properties";

	private static final String ENTITY_TYPE_KEY = "entityType";

	private static final String INDEX_TYPE_KEY = "type";

	private static final String UNIQUENESS_KEY = "uniqueness";

	private static final String OWNING_CONSTRAINT_KEY = "owningConstraint";

	private static final UnaryOperator<String> UNESCAPE_PIPE = s -> s.replace("\\|", "|");

	private static final Set<String> REQUIRED_KEYS_35 = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList(NAME_KEYS[0], INDEX_TYPE_KEY, TOKEN_NAMES, PROPERTIES_KEY)));

	private static final Set<String> REQUIRED_KEYS_40 = Collections.unmodifiableSet(new HashSet<>(
			Arrays.asList(NAME_KEYS[1], INDEX_TYPE_KEY, ENTITY_TYPE_KEY, LABELS_OR_TYPES, PROPERTIES_KEY)));

	private final Set<String> deconstructedIdentifiers;

	Index(String name, Type type, TargetEntityType targetEntityType, Collection<String> deconstructedIdentifiers,
			Collection<String> properties) {
		this(name, type, targetEntityType, deconstructedIdentifiers, properties, null);
	}

	Index(String name, Type type, TargetEntityType targetEntityType, Collection<String> deconstructedIdentifiers,
			Collection<String> properties, String options) {
		super(name, type, targetEntityType, join(deconstructedIdentifiers), properties, options);
		if (deconstructedIdentifiers.size() > 1 && type != Type.FULLTEXT) {
			throw new IllegalArgumentException(
					"Multiple labels or types are only allowed to be specified with fulltext indexes.");
		}
		if (properties.size() > 1 && type == Type.TEXT) {
			throw new IllegalArgumentException("Text indexes only allow exactly one single property.");
		}
		this.deconstructedIdentifiers = deconstructedIdentifiers.stream()
			.map(UNESCAPE_PIPE)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Starts defining a new instance of a node index.
	 * @param label the label on which the index should be applied
	 * @return the ongoing builder
	 */
	public static Builder forNode(String label) {
		return new DefaultBuilder(TargetEntityType.NODE, new String[] { label });
	}

	/**
	 * Starts defining a new instance of a node index.
	 * @param labels the labels on which the index should be applied
	 * @return the ongoing builder
	 */
	public static FulltextBuilder forNode(String... labels) {
		return new DefaultBuilder(TargetEntityType.NODE, labels);
	}

	/**
	 * Starts defining a new instance of a node index.
	 * @param type the type on which the index should be applied
	 * @return the ongoing builder
	 */
	public static Builder forRelationship(String type) {
		return new DefaultBuilder(TargetEntityType.RELATIONSHIP, new String[] { type });
	}

	/**
	 * Starts defining a new instance of a relationship index.
	 * @param types the type on which the index should be applied
	 * @return the ongoing builder
	 */
	public static FulltextBuilder forRelationship(String... types) {
		return new DefaultBuilder(TargetEntityType.RELATIONSHIP, types);
	}

	/**
	 * Creates an index from a xml definition.
	 * @param indexElement as defined in {@code migration.xsd}.
	 * @return the new index if the element as parseable
	 */
	public static Index parse(Element indexElement) {

		String name = indexElement.getAttribute(XMLSchemaConstants.NAME);
		String typeValue = indexElement.getAttribute(XMLSchemaConstants.TYPE);
		Index.Type type = Strings.isBlank(typeValue) ? Type.PROPERTY
				: Index.Type.valueOf(typeValue.toUpperCase(Locale.ROOT));
		Target target = extractTarget(indexElement);

		Set<String> properties = extractProperties(indexElement);
		String options = extractOptions(indexElement);

		String at = "(?<!\\\\)\\|";
		return new Index(name, type, target.targetEntityType(), Arrays.asList(target.identifier().split(at)),
				new LinkedHashSet<>(properties), options);
	}

	/**
	 * Parses an index from a {@link MapAccessor}.
	 * @param row the result row
	 * @return an index
	 * @throws IllegalArgumentException if the row cannot be processed
	 */
	public static Index parse(MapAccessor row) {

		if (!REQUIRED_KEYS_35.stream().allMatch(row::containsKey)
				&& !REQUIRED_KEYS_40.stream().allMatch(row::containsKey)) {
			throw new IllegalArgumentException("Required keys are missing in the row describing the index");
		}

		List<String> labelsOrTypes;
		if (row.get(TOKEN_NAMES).isNull()) {
			// will be null in the case of lookup index
			Value columnOfInterest = row.get(LABELS_OR_TYPES);
			labelsOrTypes = columnOfInterest.isNull() ? Collections.emptyList()
					: columnOfInterest.asList(Value::asString);
		}
		else {
			labelsOrTypes = row.get(TOKEN_NAMES).asList(Value::asString);
		}

		String name = !row.get(NAME_KEYS[0]).isNull() ? row.get(NAME_KEYS[0]).asString()
				: row.get(NAME_KEYS[1]).asString();

		String indexType = row.get(INDEX_TYPE_KEY).asString();
		String entityType = !row.get(ENTITY_TYPE_KEY).isNull() ? row.get(ENTITY_TYPE_KEY).asString() : "NODE";
		List<String> properties = row.get(PROPERTIES_KEY).isNull() // lookup index
				? Collections.emptyList() : row.get(PROPERTIES_KEY).asList(Value::asString);
		TargetEntityType targetEntityType = TargetEntityType.valueOf(entityType);

		Index.Type type = switch (indexType) {
			case "LOOKUP" -> Type.LOOKUP;
			case "node_label_property", "BTREE", "RANGE" -> // One of the Neo4j 5+ future
															// indexes.
				Type.PROPERTY;
			case "FULLTEXT", "node_fulltext" -> Type.FULLTEXT;
			case "node_unique_property" -> Type.CONSTRAINT_BACKING_INDEX;
			case "VECTOR" -> Type.VECTOR;
			default -> throw new IllegalArgumentException("Unsupported index type " + name);
		};

		// The definition of indexes backing unique constraints various a bit for
		// different Neo4j versions
		if (isConstraintBackingIndex(row)) {
			type = Type.CONSTRAINT_BACKING_INDEX;
		}

		String options = resolveOptions(row).orElse(null);
		return new Index(name, type, targetEntityType, labelsOrTypes, new LinkedHashSet<>(properties), options);
	}

	static boolean isConstraintBackingIndex(MapAccessor row) {
		return (!row.get(UNIQUENESS_KEY).isNull() && row.get(UNIQUENESS_KEY).asString().equals("UNIQUE"))
				|| !row.get(OWNING_CONSTRAINT_KEY).isNull();
	}

	private static String join(Collection<String> deconstructedIdentifiers) {
		return deconstructedIdentifiers.stream().map(UNESCAPE_PIPE).collect(Collectors.joining(", ", "[", "]"));
	}

	@Override
	public String toString() {
		return getName().getValue() + getType() + getIdentifier();
	}

	@Override
	public Index withName(String newName) {

		if (Objects.equals(super.getName().getValue(), newName)) {
			return this;
		}

		return new Index(newName, getType(), getTargetEntityType(), getDeconstructedIdentifiers(), getProperties(),
				this.options);
	}

	/**
	 * Creates a copy of this index with the specific set of options added. Will return
	 * {@literal this} instance if the options are identical to current options.
	 * @param newOptions the new options to use
	 * @return a (potentially) new index
	 * @since 1.13.0
	 */
	public Index withOptions(String newOptions) {

		if (Objects.equals(super.options, newOptions)) {
			return this;
		}

		return new Index(getName().getValue(), getType(), getTargetEntityType(), getDeconstructedIdentifiers(),
				getProperties(), newOptions);
	}

	/**
	 * Creates a copy of this index with the given type. Will return {@literal this}
	 * instance if the type is identical to the current one.
	 * @param newType the new type to use
	 * @return a (potentially) new index
	 * @since 1.13.0
	 */
	public Index withType(Index.Type newType) {

		if (Objects.equals(this.getType(), newType)) {
			return this;
		}

		return new Index(getName().getValue(), newType, getTargetEntityType(), getDeconstructedIdentifiers(),
				getProperties(), this.options);
	}

	/**
	 * {@literal true}, if {@literal item} is an index of the same type for the same
	 * entity containing the same properties. Also index name and options will be
	 * compared.
	 * @param that the other item to compare to
	 * @return {@literal true} if this and the other {@literal item} are equivalent
	 */
	@Override
	public boolean isEquivalentTo(CatalogItem<?> that) {

		if (this == that) {
			return true;
		}

		if (!(that instanceof Index other)) {
			return false;
		}

		return this.getType().equals(other.getType()) && this.getTargetEntityType().equals(other.getTargetEntityType())
				&& this.getIdentifier().equals(other.getIdentifier())
				&& this.getProperties().equals(other.getProperties());
	}

	Collection<String> getDeconstructedIdentifiers() {
		return Collections.unmodifiableSet(this.deconstructedIdentifiers);
	}

	boolean isRangePropertyIndex() {
		return isPropertyIndexMatching(PROVIDER_PATTERN_RANGE);
	}

	boolean isBtreePropertyIndex() {
		return isPropertyIndexMatching(PROVIDER_PATTERN_BTREE);
	}

	private boolean isPropertyIndexMatching(Pattern pattern) {
		return this.getType() == Type.PROPERTY && getOptionalOptions().filter(pattern.asPredicate()).isPresent();
	}

	/**
	 * Enumerates the different kinds of indexes.
	 */
	public enum Type implements ItemType {

		/**
		 * An index on properties. The actual type depends on the options.
		 */
		PROPERTY,
		/**
		 * A lookup index. Those indexes are paramount for the database to perform proper.
		 * They provide mapping from labels to nodes, or from relationships types to
		 * relationships, instead of between properties and entities and should usually
		 * not be changed.
		 */
		LOOKUP,
		/**
		 * Fulltext indexes for long text properties.
		 */
		FULLTEXT,
		/**
		 * Text indexes for 4.4 and later.
		 */
		TEXT,
		/**
		 * Point indexes for 5.0 and later.
		 */
		POINT,
		/**
		 * An index backing a constraint.
		 */
		CONSTRAINT_BACKING_INDEX,
		/**
		 * A vector index.
		 */
		VECTOR;

		@Override
		public String getName() {
			return name().toLowerCase(Locale.ROOT);
		}

	}

	/**
	 * Programmatic way of defining indexes.
	 */
	public interface Builder {

		/**
		 * Adds a name to the index.
		 * @param name the new name
		 * @return the next step when building an index
		 */
		NamedSingleIdentifierBuilder named(String name);

	}

	/**
	 * A fulltext builder, the only index allowing multiple types when being created.
	 */
	public interface FulltextBuilder {

		/**
		 * Adds a name to the index.
		 * @param name the new name
		 * @return the next step when building an index
		 */
		NamedFulltextBuilder named(String name);

	}

	/**
	 * Marker for the step after an index has been given a name.
	 */
	public interface NamedBuilder {

	}

	/**
	 * Allows to specify the type of the constraint. Not all types are exposed, they
	 * should usually not be touch manually.
	 */
	public interface NamedSingleIdentifierBuilder extends NamedFulltextBuilder {

		/**
		 * Creates a property index.
		 * @param properties the properties to be included in the index
		 * @return the next step
		 */
		Index onProperties(String... properties);

		/**
		 * Creates a native Neo4j text index.
		 * @param property the property to be included in the index
		 * @return the next step
		 */
		Index text(String property);

	}

	/**
	 * Fulltext is the only index available for multiple labels and multiple types in on
	 * go.
	 */
	public interface NamedFulltextBuilder extends NamedBuilder {

		/**
		 * Creates a fulltext index.
		 * @param properties the properties to be included in the index
		 * @return the next step
		 */
		Index fulltext(String... properties);

	}

	private static final class DefaultBuilder
			implements Builder, FulltextBuilder, NamedSingleIdentifierBuilder, NamedFulltextBuilder {

		private final TargetEntityType targetEntity;

		private final String[] identifiers;

		private String name;

		private DefaultBuilder(TargetEntityType targetEntity, String[] identifiers) {
			this.targetEntity = targetEntity;
			this.identifiers = identifiers;
		}

		@Override
		public DefaultBuilder named(String newName) {

			this.name = newName;
			return this;
		}

		@Override
		public Index onProperties(String... properties) {
			return makeIndex(properties, Type.PROPERTY);
		}

		@Override
		public Index text(String property) {
			return makeIndex(new String[] { property }, Type.TEXT);
		}

		@Override
		public Index fulltext(String... properties) {
			return makeIndex(properties, Type.FULLTEXT);
		}

		private Index makeIndex(String[] properties, Type text) {
			return new Index(this.name, text, this.targetEntity, Arrays.asList(this.identifiers),
					Arrays.asList(properties));
		}

	}

}
