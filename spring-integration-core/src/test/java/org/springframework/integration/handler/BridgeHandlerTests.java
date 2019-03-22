/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.handler;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.MessageMatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 */
public class BridgeHandlerTests {

	private final BridgeHandler handler = new BridgeHandler();

	@Factory
	public static Matcher<Message<?>> sameExceptImmutableHeaders(Message<?> expected) {
		return new MessageMatcher(expected);
	}

	@Test
	public void simpleBridge() {
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		Message<?> request = new GenericMessage<String>("test");
		handler.handleMessage(request);
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertThat(reply, sameExceptImmutableHeaders(request));
	}

	@Test
	public void missingOutputChannelVerifiedAtRuntime() {
		Message<?> request = new GenericMessage<String>("test");
		try {
			handler.handleMessage(request);
			fail("Expected exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause(), instanceOf(DestinationResolutionException.class));
		}
	}

	@Test(timeout = 1000)
	public void missingOutputChannelAllowedForReplyChannelMessages() throws Exception {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> request = MessageBuilder.withPayload("tst").setReplyChannel(replyChannel).build();
		handler.handleMessage(request);
		assertThat(replyChannel.receive(), sameExceptImmutableHeaders(request));
	}

}
