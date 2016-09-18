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

package org.springframework.integration.endpoint;

/**
 * A message source that can limit the number of remote objects it fetches.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public abstract class AbstractFetchLimitingMessageSource<T> extends AbstractMessageSource<T>
		implements MessageSourceManagement {

	private volatile int maxFetchSize = Integer.MIN_VALUE;

	@Override
	public void setMaxFetchSize(int maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}

	@Override
	public int getMaxFetchSize() {
		return this.maxFetchSize;
	}

	@Override
	protected Object doReceive() {
		return doReceive(this.maxFetchSize);
	}

	/**
	 * Subclasses must implement this method. Typically the returned value will be the
	 * payload of type T, but the returned value may also be a Message instance whose
	 * payload is of type T.
	 * @param maxFetchSize the maximum number of messages to fetch if a fetch is
	 * necessary.
	 * @return The value returned.
	 */
	protected abstract Object doReceive(int maxFetchSize);

}
