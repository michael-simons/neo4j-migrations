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
package ac.simons.neo4j.migrations.core;

import java.lang.reflect.Constructor;
import java.util.Optional;

/**
 * Interface to be implemented for Java-based migrations.
 *
 * @author Michael J. Simons
 * @since 0.0.1
 */
public non-sealed interface JavaBasedMigration extends Migration {

	/**
	 * Helper method for retrieving the default constructor of a given class. When such a
	 * constructor exist, it will be made accessible.
	 * @param c the class whose constructor should be returned
	 * @param <T> the type of the class
	 * @return the default constructor
	 * @throws NoSuchMethodException if there is no such default constructor
	 * @since 1.13.0
	 */
	@SuppressWarnings("squid:S3011") // Very much the point of the whole thing
	static <T extends JavaBasedMigration> Constructor<T> getDefaultConstructorFor(Class<T> c)
			throws NoSuchMethodException {
		Constructor<T> ctr = c.getDeclaredConstructor();
		ctr.setAccessible(true);
		return ctr;
	}

	@Override
	default MigrationVersion getVersion() {
		return MigrationVersion.of(getClass());
	}

	@Override
	default Optional<String> getOptionalDescription() {
		return getVersion().getOptionalDescription().or(() -> Optional.of(getClass().getSimpleName()));
	}

	@Override
	default String getSource() {
		return getClass().getName();
	}

	/**
	 * Return {@literal true} to mark this Java based migration explicitly as repeatable.
	 * By default, a Java based migration will be repeatable when it's name matches the
	 * repeatable version pattern {@literal Rxzy__Something_something.java}, it will
	 * always be repeated as the default checksum is empty.
	 * <p>
	 * You can take full control over this by overwriting this method and in addition,
	 * {@link #getChecksum()} and react in that according to your needs.
	 * @return {@literal true} if this is a repeatable migration
	 * @since 2.0.0
	 */
	default boolean isRepeatable() {
		return getVersion().isRepeatable();
	}

}
