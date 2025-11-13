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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;

/**
 * Avoid mocking the {@link MapAccessor}.
 *
 * @author Michael J. Simons
 */
public final class MapAccessorAndRecordImpl implements MapAccessor, Record {

	private final Map<String, Value> content;

	public MapAccessorAndRecordImpl(Map<String, Value> content) {
		this.content = content;
	}

	@Override
	public List<String> keys() {
		return new ArrayList<>(this.content.keySet());
	}

	@Override
	public boolean containsKey(String key) {
		return this.content.containsKey(key);
	}

	@Override
	public Value get(String key) {
		return this.content.getOrDefault(key, Values.NULL);
	}

	@Override
	public int size() {
		return this.content.size();
	}

	@Override
	public List<Value> values() {
		return new ArrayList<>(this.content.values());
	}

	@Override
	public int index(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Value get(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Pair<String, Value>> fields() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Iterable<T> values(Function<Value, T> mapFunction) {
		return this.content.values().stream().map(mapFunction).toList();
	}

	@Override
	public Map<String, Object> asMap() {
		return this.content.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asObject()));
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapFunction) {
		return this.content.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> mapFunction.apply(e.getValue())));
	}

	@Override
	public Value get(String key, Value defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(String key, Object defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Number get(String key, Number defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entity get(String key, Entity defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node get(String key, Node defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path get(String key, Path defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Relationship get(String key, Relationship defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Object> get(String key, List<Object> defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> get(String key, List<T> defaultValue, Function<Value, T> mapFunc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Object> get(String key, Map<String, Object> defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Map<String, T> get(String key, Map<String, T> defaultValue, Function<Value, T> mapFunc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int get(String key, int defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long get(String key, long defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean get(String key, boolean defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String get(String key, String defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public float get(String key, float defaultValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double get(String key, double defaultValue) {
		throw new UnsupportedOperationException();
	}

}
