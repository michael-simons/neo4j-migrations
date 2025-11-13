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
package ac.simons.neo4j.migrations.cli.internal;

/**
 * Subset of {code org.graalvm.sdk:graal-sdk} to avoid having the SDK on the classpath.
 * Original licensed under The Universal Permissive License (UPL), Version 1.0.
 *
 * @author GraalVM Team and others
 */
public final class ImageInfo {

	/**
	 * Holds the string that is the name of the system property providing information
	 * about the context in which code is currently executing. If the property returns the
	 * string given by {@literal buildtime} the code is executing in the context of image
	 * building (e.g. in a static initializer of a class that will be contained in the
	 * image). If the property returns the string given by
	 * {@link #PROPERTY_IMAGE_CODE_VALUE_RUNTIME} the code is executing at image runtime.
	 * Otherwise, the property is not set.
	 */
	public static final String PROPERTY_IMAGE_CODE_KEY = "org.graalvm.nativeimage.imagecode";

	/**
	 * Holds the string that will be returned by the system property for
	 * {@link ImageInfo#PROPERTY_IMAGE_CODE_KEY} if code is executing at image runtime.
	 */
	public static final String PROPERTY_IMAGE_CODE_VALUE_RUNTIME = "runtime";

	private ImageInfo() {
	}

	/**
	 * Returns true if (at the time of the call) code is executing at image runtime. This
	 * method will be const-folded. It can be used to hide parts of an application that
	 * only work when running as native image.
	 * @return {@literal true} if this app runs in a native image runtime
	 */
	public static boolean inImageRuntimeCode() {
		return PROPERTY_IMAGE_CODE_VALUE_RUNTIME.equals(System.getProperty(PROPERTY_IMAGE_CODE_KEY));
	}

}
