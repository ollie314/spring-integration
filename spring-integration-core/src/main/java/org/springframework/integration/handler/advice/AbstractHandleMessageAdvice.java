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

package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * The base {@link HandleMessageAdvice} for advices which can be applied only
 * for the {@link MessageHandler#handleMessage(Message)}.
 *
 * @author Artem Bilan
 * @since 4.3.1
 */
public abstract class AbstractHandleMessageAdvice implements HandleMessageAdvice {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@Override
	public final Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		Object invocationThis = invocation.getThis();
		Object[] arguments = invocation.getArguments();
		boolean isMessageHandler = invocationThis != null && invocationThis instanceof MessageHandler;
		boolean isMessageMethod = method.getName().equals("handleMessage")
				&& (arguments.length == 1 && arguments[0] instanceof Message);
		if (!isMessageHandler || !isMessageMethod) {
			if (this.logger.isWarnEnabled()) {
				String clazzName = invocationThis == null
						? method.getDeclaringClass().getName()
						: invocationThis.getClass().getName();
				this.logger.warn("This advice " + getClass().getName() +
						" can only be used for MessageHandlers; an attempt to advise method '"
						+ method.getName() + "' in '" + clazzName + "' is ignored.");
			}
			return invocation.proceed();
		}

		Message<?> message = (Message<?>) arguments[0];
		return doInvoke(invocation, message);
	}

	protected abstract Object doInvoke(MethodInvocation invocation, Message<?> message) throws Throwable;

}
