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

package org.springframework.integration.amqp.outbound;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.log4j.Level;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.AmqpReplyTimeoutException;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate.RabbitMessageFuture;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.amqp.rule.BrokerRunning;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.rule.Log4jLevelAdjuster;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class AsyncAmqpGatewayTests {

	@ClassRule
	public static BrokerRunning brokerRunning = BrokerRunning.isRunningWithEmptyQueues("asyncQ1", "asyncRQ1");

	@Rule
	public Log4jLevelAdjuster adjuster = new Log4jLevelAdjuster(Level.TRACE, "org.springframework.integration",
			"org.springframework.amqp");

	@AfterClass
	public static void tearDown() {
		brokerRunning.removeTestQueues();
	}

	@Test
	public void testConfirmsAndReturns() throws Exception {
		CachingConnectionFactory ccf = new CachingConnectionFactory("localhost");
		ccf.setPublisherConfirms(true);
		ccf.setPublisherReturns(true);
		RabbitTemplate template = new RabbitTemplate(ccf);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(ccf);
		container.setBeanName("replyContainer");
		container.setQueueNames("asyncRQ1");
		container.afterPropertiesSet();
		container.start();
		AsyncRabbitTemplate asyncTemplate = spy(new AsyncRabbitTemplate(template, container));
		asyncTemplate.setEnableConfirms(true);
		asyncTemplate.setMandatory(true);
		asyncTemplate.start();

		SimpleMessageListenerContainer receiver = new SimpleMessageListenerContainer(ccf);
		receiver.setBeanName("receiver");
		receiver.setQueueNames("asyncQ1");
		final CountDownLatch waitForAckBeforeReplying = new CountDownLatch(1);
		MessageListenerAdapter messageListener = new MessageListenerAdapter(new Object() {

			@SuppressWarnings("unused")
			public String handleMessage(String foo) {
				try {
					waitForAckBeforeReplying.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return foo.toUpperCase();
			}

		});
		receiver.setMessageListener(messageListener);
		receiver.afterPropertiesSet();
		receiver.start();

		AsyncAmqpOutboundGateway gateway = new AsyncAmqpOutboundGateway(asyncTemplate);
		Log logger = spy(TestUtils.getPropertyValue(gateway, "logger", Log.class));
		given(logger.isDebugEnabled()).willReturn(true);
		final CountDownLatch replyTimeoutLatch = new CountDownLatch(1);
		willAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				invocation.callRealMethod();
				replyTimeoutLatch.countDown();
				return null;
			}

		}).given(logger).debug(Matchers.startsWith("Reply not required and async timeout for"));
		new DirectFieldAccessor(gateway).setPropertyValue("logger", logger);
		QueueChannel outputChannel = new QueueChannel();
		outputChannel.setBeanName("output");
		QueueChannel returnChannel = new QueueChannel();
		returnChannel.setBeanName("returns");
		QueueChannel ackChannel = new QueueChannel();
		ackChannel.setBeanName("acks");
		QueueChannel errorChannel = new QueueChannel();
		errorChannel.setBeanName("errors");
		gateway.setOutputChannel(outputChannel);
		gateway.setReturnChannel(returnChannel);
		gateway.setConfirmAckChannel(ackChannel);
		gateway.setConfirmNackChannel(ackChannel);
		gateway.setConfirmCorrelationExpressionString("#this");
		gateway.setExchangeName("");
		gateway.setRoutingKey("asyncQ1");
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();

		Message<?> message = MessageBuilder.withPayload("foo").setErrorChannel(errorChannel).build();

		gateway.handleMessage(message);

		Message<?> ack = ackChannel.receive(10000);
		assertNotNull(ack);
		assertEquals("foo", ack.getPayload());
		assertEquals(true, ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM));
		waitForAckBeforeReplying.countDown();

		Message<?> received = outputChannel.receive(10000);
		assertNotNull(received);
		assertEquals("FOO", received.getPayload());

		// timeout tests
		asyncTemplate.setReceiveTimeout(10);

		receiver.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(org.springframework.amqp.core.Message message) {
			}

		});
		// reply timeout with no requiresReply
		message = MessageBuilder.withPayload("bar").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);
		assertTrue(replyTimeoutLatch.await(10, TimeUnit.SECONDS));

		// reply timeout with requiresReply
		gateway.setRequiresReply(true);
		message = MessageBuilder.withPayload("baz").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);

		received = errorChannel.receive(10000);
		assertThat(received, instanceOf(ErrorMessage.class));
		ErrorMessage error = (ErrorMessage) received;
		assertThat(error.getPayload(), instanceOf(MessagingException.class));
		assertThat(error.getPayload().getCause(), instanceOf(AmqpReplyTimeoutException.class));
		asyncTemplate.setReceiveTimeout(30000);
		receiver.setMessageListener(messageListener);

		// error on sending result
		DirectChannel errorForce = new DirectChannel();
		errorForce.setBeanName("errorForce");
		errorForce.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				throw new RuntimeException("intentional");
			}

		});
		gateway.setOutputChannel(errorForce);
		message = MessageBuilder.withPayload("qux").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);
		received = errorChannel.receive(10000);
		assertThat(received, instanceOf(ErrorMessage.class));
		error = (ErrorMessage) received;
		assertThat(error.getPayload(), instanceOf(MessagingException.class));
		assertEquals("QUX", ((MessagingException) error.getPayload()).getFailedMessage().getPayload());

		gateway.setRoutingKey(UUID.randomUUID().toString());
		message = MessageBuilder.withPayload("fiz").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);
		Message<?> returned = returnChannel.receive(10000);
		assertNotNull(returned);
		assertEquals("fiz", returned.getPayload());
		ackChannel.receive(10000);
		ackChannel.purge(null);

		// Simulate a nack - it's hard to get Rabbit to generate one
		// We must have consumed all the real acks by now, though, to prevent partial stubbing errors

		RabbitMessageFuture future = asyncTemplate.new RabbitMessageFuture(null, null);
		willReturn(future).given(asyncTemplate).sendAndReceive(anyString(), anyString(),
				any(org.springframework.amqp.core.Message.class));
		DirectFieldAccessor dfa = new DirectFieldAccessor(future);
		dfa.setPropertyValue("nackCause", "nacknack");
		SettableListenableFuture<Boolean> confirmFuture = new SettableListenableFuture<Boolean>();
		confirmFuture.set(false);
		dfa.setPropertyValue("confirm", confirmFuture);

		message = MessageBuilder.withPayload("buz").setErrorChannel(errorChannel).build();
		gateway.handleMessage(message);

		ack = ackChannel.receive(10000);
		assertNotNull(ack);
		assertEquals("buz", ack.getPayload());
		assertEquals("nacknack", ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM_NACK_CAUSE));
		assertEquals(false, ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM));

		asyncTemplate.stop();
		receiver.stop();
		ccf.destroy();
	}

}
