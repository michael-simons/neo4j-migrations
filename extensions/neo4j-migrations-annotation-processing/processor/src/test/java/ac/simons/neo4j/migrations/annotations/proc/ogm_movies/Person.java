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
package ac.simons.neo4j.migrations.annotations.proc.ogm_movies;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * @author Michael J. Simons
 */
@NodeEntity
public final class Person {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private Integer born;

	@JsonCreator
	public Person(String name, Integer born) {
		this.name = name;
		this.born = born;
	}

	/**
	 * Make OGM happy.
	 */
	Person() {
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Integer getBorn() {
		return this.born;
	}

	public void setBorn(Integer born) {
		this.born = born;
	}

}
