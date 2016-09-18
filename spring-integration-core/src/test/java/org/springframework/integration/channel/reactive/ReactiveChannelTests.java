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

package org.springframework.integration.channel.reactive;

import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.ReactiveChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ReactiveChannelTests {

	@Autowired
	private MessageChannel reactiveChannel;

	@Test
	@SuppressWarnings("unchecked")
	public void testReactiveMessageChannel() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();

		for (int i = 0; i < 10; i++) {
			this.reactiveChannel.send(MessageBuilder.withPayload(i).setReplyChannel(replyChannel).build());
		}

		for (int i = 0; i < 10; i++) {
			Message<?> receive = replyChannel.receive(10000);
			assertNotNull(receive);
			assertThat(receive.getPayload(), isOneOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
		}
	}

	@Configuration
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		public MessageChannel reactiveChannel() {
			return new ReactiveChannel();
		}

		@ServiceActivator(inputChannel = "reactiveChannel")
		public String handle(int payload) {
			/* TODO doesn't work yet
			if (payload == 5) {
				throw new IllegalStateException("intentional");
			}*/
			return "" + payload;
		}

	}

}
