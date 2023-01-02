/*
 * Copyright 2020-2023 the original author or authors.
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
package ac.simons.neo4j.migrations.annotations.proc.sdn6.labels;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
public final class InheritedLabels {

	@Node(primaryLabel = "Base", labels = { "Bases" })
	private static abstract class BaseClass {
		@Id private Long id;
	}

	@Node(primaryLabel = "Child", labels = { "Person" })
	private static class Child extends BaseClass {
		private String name;
	}

	@Node
	private static abstract class A {
		@Id private Long id;
	}

	private static abstract class B extends A {
	}

	@Node
	private static abstract class C extends B {
	}

	private interface X {
	}

	@Node
	private interface Y {
	}

	@Node(primaryLabel = "Foo")
	private static class Foo extends C implements X, Y {
	}

	private InheritedLabels() {
	}
}
