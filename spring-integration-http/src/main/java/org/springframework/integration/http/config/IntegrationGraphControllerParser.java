/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.config;

import java.util.Collections;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.integration.http.support.HttpContextUtils;

/**
 * The {@link BeanDefinitionParser} for the {@code <int-http:graph-controller>} component.
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class IntegrationGraphControllerParser implements BeanDefinitionParser {

	private final IntegrationGraphControllerRegistrar graphControllerRegistrar =
			new IntegrationGraphControllerRegistrar();

	@Override
	public BeanDefinition parse(final Element element, ParserContext parserContext) {
		if (HttpContextUtils.SERVLET_PRESENT) {
			this.graphControllerRegistrar.registerBeanDefinitions(
					new StandardAnnotationMetadata(IntegrationGraphControllerParser.class) {

						@Override
						public Map<String, Object> getAnnotationAttributes(String annotationType) {
							return Collections.<String, Object>singletonMap("value", element.getAttribute("path"));
						}

					}, parserContext.getRegistry());
		}
		else {
			parserContext.getReaderContext().warning("The 'IntegrationGraphController' isn't registered " +
					"with the application context because" +
					" there is no 'org.springframework.web.servlet.DispatcherServlet' in the classpath.", element);
		}

		return null;
	}

}
