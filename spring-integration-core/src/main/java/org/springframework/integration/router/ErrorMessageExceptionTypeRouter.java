/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.ClassUtils;

/**
 * A Message Router that resolves the target {@link MessageChannel} for
 * messages whose payload is an Exception. The channel resolution is based upon
 * the most specific cause of the error for which a channel-mapping exists.
 * <p>
 * The channel-mapping can be specified for the super classes to avoid mapping duplication
 * for the particular exception implementation.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class ErrorMessageExceptionTypeRouter extends AbstractMappingMessageRouter {

	private volatile Map<String, Class<?>> classNameMappings = new ConcurrentHashMap<String, Class<?>>();

	private volatile boolean initialized;

	@Override
	@ManagedAttribute
	public void setChannelMappings(Map<String, String> channelMappings) {
		super.setChannelMappings(channelMappings);
		if (this.initialized) {
			populateClassNameMapping(channelMappings.keySet());
		}
	}

	private void populateClassNameMapping(Set<String> classNames) {
		Map<String, Class<?>> newClassNameMappings = new ConcurrentHashMap<String, Class<?>>();
		for (String className : classNames) {
			newClassNameMappings.put(className, resolveClassFromName(className));
		}
		this.classNameMappings = newClassNameMappings;
	}

	private Class<?> resolveClassFromName(String className) {
		try {
			return ClassUtils.forName(className, getApplicationContext().getClassLoader());
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Cannot load class for channel mapping.", e);
		}
	}

	@Override
	@ManagedOperation
	public void setChannelMapping(String key, String channelName) {
		super.setChannelMapping(key, channelName);
		Map<String, Class<?>> newClassNameMappings = new ConcurrentHashMap<String, Class<?>>(this.classNameMappings);
		newClassNameMappings.put(key, resolveClassFromName(key));
		this.classNameMappings = newClassNameMappings;
	}

	@Override
	@ManagedOperation
	public void removeChannelMapping(String key) {
		super.removeChannelMapping(key);
		Map<String, Class<?>> newClassNameMappings = new ConcurrentHashMap<String, Class<?>>(this.classNameMappings);
		newClassNameMappings.remove(key);
		this.classNameMappings = newClassNameMappings;
	}

	@Override
	@ManagedOperation
	public void replaceChannelMappings(Properties channelMappings) {
		super.replaceChannelMappings(channelMappings);
		populateClassNameMapping(this.channelMappings.keySet());
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		populateClassNameMapping(this.channelMappings.keySet());
		this.initialized = true;
	}

	@Override
	protected List<Object> getChannelKeys(Message<?> message) {
		String mostSpecificCause = null;
		Object payload = message.getPayload();
		if (payload instanceof Throwable) {
			Throwable cause = (Throwable) payload;
			while (cause != null) {
				for (Map.Entry<String, Class<?>> entry : this.classNameMappings.entrySet()) {
					String channelKey = entry.getKey();
					Class<?> exceptionClass = entry.getValue();
					if (exceptionClass.isInstance(cause)) {
						mostSpecificCause = channelKey;
					}
				}
				cause = cause.getCause();
			}
		}
		return Collections.<Object>singletonList(mostSpecificCause);
	}

}
