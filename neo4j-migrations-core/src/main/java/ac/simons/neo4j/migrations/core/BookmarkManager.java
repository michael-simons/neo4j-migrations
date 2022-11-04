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
package ac.simons.neo4j.migrations.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.neo4j.driver.Bookmark;

/**
 * Responsible for storing, updating and retrieving the bookmarks of Neo4j's transaction. The original class appeared for
 * the first time in <a href="https://github.com/spring-projects/spring-data-neo4j/tree/6.0.0">Spring Data Neo4j 6</a> by
 * the same author, under the same license.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Death Magnetic
 * @since 1.3.0
 */
final class BookmarkManager {

	private final Set<Bookmark> bookmarks = new HashSet<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	Collection<Bookmark> getBookmarks() {

		try {
			read.lock();
			return Set.copyOf(bookmarks);
		} finally {
			read.unlock();
		}
	}

	void updateBookmarks(Collection<Bookmark> usedBookmarks, Bookmark lastBookmark) {

		try {
			write.lock();
			bookmarks.removeAll(usedBookmarks);
			if (lastBookmark != null) {
				bookmarks.add(lastBookmark);
			}
		} finally {
			write.unlock();
		}
	}
}
