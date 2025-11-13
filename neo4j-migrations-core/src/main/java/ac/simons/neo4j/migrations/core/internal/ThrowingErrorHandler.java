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
package ac.simons.neo4j.migrations.core.internal;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A throwing error handler used when building {@link org.w3c.dom.Document documents} for
 * which a scheme is available. We do skip error {@literal cvc-id.1}, which indicates
 * unresolvable ids. This is on purpose, as we usually can resolve them in older
 * migrations and if not, we still can throw things around.
 *
 * @author Michael J. Simons
 * @since 1.7.0
 */
public final class ThrowingErrorHandler implements ErrorHandler {

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		throw exception;
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		// We do check for names later on, so we ignore this id
		// https://wiki.xmldation.com/Support/Validator/cvc-id-1
		if (!exception.getMessage().startsWith("cvc-id.1")) {
			throw exception;
		}
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		throw exception;
	}

}
