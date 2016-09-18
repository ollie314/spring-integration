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

package org.springframework.integration.gemfire.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.internal.cache.LocalRegion;

/**
 * @author David Turanski
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class GemfireInboundChannelAdapterTests {

	@Autowired
	SubscribableChannel channel1;

	@Autowired
	SubscribableChannel channel2;

	@Autowired
	SubscribableChannel channel3;

	@Autowired
	SubscribableChannel errorChannel;

	@Autowired
	LocalRegion region1;

	@Autowired
	LocalRegion region2;

	@Autowired
	LocalRegion region3;

	@Test
	public void testGemfireInboundChannelAdapterWithExpression() {

		EventHandler eventHandler1 = new EventHandler();
		channel1.subscribe(eventHandler1);

		region1.put("payload", "payload");

		assertEquals("payload", eventHandler1.event);
	}

	@Test
	public void testGemfireInboundChannelAdapterDefault() {
		EventHandler eventHandler2 = new EventHandler();
		channel2.subscribe(eventHandler2);

		region2.put("payload", "payload");

		assertTrue(eventHandler2.event instanceof EntryEvent);
		EntryEvent<?, ?> event = (EntryEvent<?, ?>) eventHandler2.event;
		assertEquals("payload", event.getNewValue());
	}

	@Test
	public void testErrorChannel() {
		channel3.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new MessagingException("got an error");
			}
		});
		ErrorHandler errorHandler = new ErrorHandler();
		errorChannel.subscribe(errorHandler);

		region3.put("payload", "payload");

		assertEquals(1, errorHandler.count);
	}

	static class ErrorHandler implements MessageHandler {

		public int count = 0;

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			assertTrue(message instanceof ErrorMessage);
			count++;
		}

	}

	static class EventHandler implements MessageHandler {

		public Object event = null;

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			event = message.getPayload();
		}

	}

}
