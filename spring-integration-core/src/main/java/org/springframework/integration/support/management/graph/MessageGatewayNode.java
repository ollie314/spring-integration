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

package org.springframework.integration.support.management.graph;

import org.springframework.integration.gateway.MessagingGatewaySupport;

/**
 * Represents an inbound gateway.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class MessageGatewayNode extends ErrorCapableEndpointNode {

	public MessageGatewayNode(int nodeId, String name, MessagingGatewaySupport gateway, String output, String errors) {
		super(nodeId, name, gateway, output, errors, new Stats(gateway));
	}


	public static final class Stats extends IntegrationNode.Stats {

		private final MessagingGatewaySupport gateway;

		private Stats(MessagingGatewaySupport gateway) {
			this.gateway = gateway;
		}

		@Override
		protected boolean isAvailable() {
			return this.gateway.isCountsEnabled();
		}

		public long getSendCount() {
			return this.gateway.getMessageCountLong();
		}

	}

}
