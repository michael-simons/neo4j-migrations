/*
 * Copyright 2020-2021 the original author or authors.
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
import java.util.Collections;
import java.util.List;

/**
 * Result of a clean operation. The result is immutable.
 *
 * @author Michael J. Simons
 * @since 1.1.0
 */
public final class CleanResult implements OperationResult {

	private final List<String> chainsDeleted;

	private final List<String> locksDeleted;

	private final List<String> constraintsDeleted;

	// TODO Number of nodes / relationships deleted

	CleanResult(List<String> chainsDeleted, List<String> locksDeleted, List<String> constraintsDeleted) {
		this.chainsDeleted = Collections.unmodifiableList(new ArrayList<>(chainsDeleted));
		this.locksDeleted = Collections.unmodifiableList(new ArrayList<>(locksDeleted));
		this.constraintsDeleted = Collections.unmodifiableList(new ArrayList<>(constraintsDeleted));
	}

	/**
	 * The list of chains deleted.
	 *
	 * @return The name of the chains' migration target
	 */
	public List<String> getChainsDeleted() {
		return chainsDeleted;
	}

	/**
	 * @return The list of locks deleted (if there has been any left)
	 */
	public List<String> getLocksDeleted() {
		return locksDeleted;
	}

	/**
	 * @return The list of constraints deleted
	 */
	public List<String> getConstraintsDeleted() {
		return constraintsDeleted;
	}
}
