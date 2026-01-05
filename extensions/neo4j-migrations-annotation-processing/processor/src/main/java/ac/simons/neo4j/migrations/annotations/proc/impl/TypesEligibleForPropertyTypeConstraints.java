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
package ac.simons.neo4j.migrations.annotations.proc.impl;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.type.TypeMirror;

import ac.simons.neo4j.migrations.core.catalog.PropertyType;

final class TypesEligibleForPropertyTypeConstraints {

	static final Map<String, PropertyType> VALUES;

	static {
		var tmp = new HashMap<String, PropertyType>();
		tmp.put("java.lang.Boolean", PropertyType.BOOLEAN);
		tmp.put("boolean", PropertyType.BOOLEAN);
		tmp.put("java.lang.String", PropertyType.STRING);
		tmp.put("java.lang.Long", PropertyType.INTEGER);
		tmp.put("long", PropertyType.INTEGER);
		tmp.put("java.lang.Integer", PropertyType.INTEGER);
		tmp.put("int", PropertyType.INTEGER);
		tmp.put("java.lang.Short", PropertyType.INTEGER);
		tmp.put("short", PropertyType.INTEGER);
		tmp.put("java.lang.Double", PropertyType.FLOAT);
		tmp.put("double", PropertyType.FLOAT);
		tmp.put("java.lang.Float", PropertyType.FLOAT);
		tmp.put("float", PropertyType.FLOAT);
		tmp.put("java.time.LocalDate", PropertyType.DATE);
		tmp.put("java.time.LocalTime", PropertyType.LOCAL_TIME);
		tmp.put("java.time.OffsetTime", PropertyType.ZONED_TIME);
		tmp.put("java.time.LocalDateTime", PropertyType.LOCAL_DATETIME);
		tmp.put("java.time.OffsetDateTime", PropertyType.ZONED_DATETIME);
		tmp.put("java.time.ZonedDateTime", PropertyType.ZONED_DATETIME);
		tmp.put("org.neo4j.driver.types.IsoDuration", PropertyType.DURATION);
		tmp.put("org.neo4j.driver.types.Point", PropertyType.POINT);

		VALUES = Map.copyOf(tmp);
	}

	private TypesEligibleForPropertyTypeConstraints() {
	}

	static boolean contains(TypeMirror key) {
		return VALUES.containsKey(key.toString());
	}

	static PropertyType get(TypeMirror key) {
		return VALUES.get(key.toString());
	}

}
