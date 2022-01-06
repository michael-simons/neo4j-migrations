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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.function.UnaryOperator;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

/**
 * Default implementation of the {@link MigrationContext}, including the logic of wrapping driver and blocking sessions
 * into proxy objects taking care of bookmarks.
 *
 * @author Michael J. Simons
 * @since 1.3.0
 */
final class DefaultMigrationContext implements MigrationContext {

	private static final Method WITH_IMPERSONATED_USER = findWithImpersonatedUser();
	private final UnaryOperator<SessionConfig.Builder> applySchemaDatabase;

	private static Method findWithImpersonatedUser() {
		try {
			return SessionConfig.Builder.class.getMethod("withImpersonatedUser", String.class);
		} catch (NoSuchMethodException e) {
			return null; // This is fine
		}
	}

	private final MigrationsConfig config;

	private final BookmarkManager bookmarkManager;

	private final Driver driver;

	DefaultMigrationContext(MigrationsConfig config, Driver driver) {

		if (config.getOptionalImpersonatedUser().isPresent() && WITH_IMPERSONATED_USER == null) {
			throw new IllegalArgumentException(
				"User impersonation requires a driver that supports `withImpersonatedUser`.");
		}

		this.config = config;
		this.bookmarkManager = new BookmarkManager();
		this.driver = (Driver) Proxy.newProxyInstance(this.getClass().getClassLoader(),
			new Class<?>[] { Driver.class }, new DriverProxy(bookmarkManager, driver));
		this.applySchemaDatabase = this.config.getOptionalSchemaDatabase().map(schemaDatabase ->
			(UnaryOperator<SessionConfig.Builder>) builder -> builder.withDatabase(schemaDatabase)
		).orElseGet(UnaryOperator::identity);
	}

	@Override
	public MigrationsConfig getConfig() {
		return config;
	}

	@Override
	public Driver getDriver() {
		return driver;
	}

	@Override
	public SessionConfig getSessionConfig() {
		return getSessionConfig(UnaryOperator.identity());
	}

	@Override
	public SessionConfig getSessionConfig(UnaryOperator<SessionConfig.Builder> configCustomizer) {

		SessionConfig.Builder builder = SessionConfig.builder()
			.withDefaultAccessMode(AccessMode.WRITE)
			.withBookmarks(bookmarkManager.getBookmarks());
		this.config.getOptionalDatabase().ifPresent(builder::withDatabase);
		this.config.getOptionalImpersonatedUser().ifPresent(user -> setWithImpersonatedUser(builder, user));

		return configCustomizer.apply(builder).build();
	}

	static void setWithImpersonatedUser(SessionConfig.Builder builder, String user) {
		try {
			// This is fine, when an impersonated user is present, the availability of
			// this method has been checked.
			// noinspection ConstantConditions
			WITH_IMPERSONATED_USER.invoke(builder, user);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new MigrationsException("Could not impersonate a user on the driver level", e);
		}
	}

	static SessionConfig.Builder copyIntoBuilder(SessionConfig sessionConfig) {

		SessionConfig.Builder builder = SessionConfig.builder();
		builder.withBookmarks(sessionConfig.bookmarks());
		sessionConfig.database().ifPresent(builder::withDatabase);
		builder.withDefaultAccessMode(sessionConfig.defaultAccessMode());
		sessionConfig.fetchSize().ifPresent(builder::withFetchSize);
		sessionConfig.impersonatedUser().ifPresent(user -> setWithImpersonatedUser(builder, user));

		return builder;
	}

	@Override
	public Session getSchemaSession() {
		return getDriver().session(getSessionConfig(applySchemaDatabase));
	}

	/**
	 * This proxy catches all calls to {@link Driver#session()} and {@link Driver#session(SessionConfig)} and
	 * makes sure that a session config with the current set of bookmarks is used correctly.
	 */
	static class DriverProxy implements InvocationHandler {

		private final BookmarkManager bookmarkManager;
		private final Driver target;

		DriverProxy(BookmarkManager bookmarkManager, Driver target) {
			this.bookmarkManager = bookmarkManager;
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			try {
				if ("session".equals(method.getName())) {
					SessionConfig sessionConfig;
					Collection<Bookmark> existingBookmarks = bookmarkManager.getBookmarks();
					if (args.length == 0) {
						// There is no session config
						sessionConfig = SessionConfig.builder().withBookmarks(existingBookmarks).build();
					} else {
						SessionConfig existingConfig = (SessionConfig) args[0];
						// {@literal null} is the default, so if there's something non-null in,
						// probably someone had thought about already
						if (existingConfig.bookmarks() != null) {
							sessionConfig = existingConfig;
						} else {
							sessionConfig = copyIntoBuilder(existingConfig)
								.withBookmarks(existingBookmarks)
								.build();
						}
					}
					Session session = target.session(sessionConfig);
					return Proxy.newProxyInstance(this.getClass().getClassLoader(),
						new Class<?>[] { Session.class },
						new SessionProxy(bookmarkManager, existingBookmarks, session));
				} else {
					return method.invoke(target, args);
				}
			} catch (InvocationTargetException ite) {
				throw ite.getCause();
			}
		}
	}

	/**
	 * This proxy catches the {@link Session#close()} call on the blocking session, retrieving the latest bookmark
	 * and stores it with the bookmark manager.
	 */
	static class SessionProxy implements InvocationHandler {

		private final BookmarkManager bookmarkManager;
		/**
		 * The bookmarks used when the {@link #target target session} has been initialized. They need to
		 * be kept around separately, as the session doesn't allow to retrieve the config back after creation.
		 */
		private final Collection<Bookmark> usedBookmarks;
		private final Session target;

		SessionProxy(BookmarkManager bookmarkManager, Collection<Bookmark> usedBookmarks, Session target) {
			this.bookmarkManager = bookmarkManager;
			this.usedBookmarks = usedBookmarks;
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			try {
				if ("close".equals(method.getName())) {
					bookmarkManager.updateBookmarks(usedBookmarks, target.lastBookmark());
					target.close();
					return null;
				}
				return method.invoke(target, args);
			} catch (InvocationTargetException ite) {
				throw ite.getCause();
			}
		}
	}
}
