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

package org.springframework.integration.xml.splitter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class XPathMessageSplitterTests {

	private XPathMessageSplitter splitter;

	private final QueueChannel replyChannel = new QueueChannel();


	@Before
	public void setUp() {
		String splittingXPath = "/orders/order";
		this.splitter = new XPathMessageSplitter(splittingXPath);
		this.splitter.setOutputChannel(replyChannel);
		this.splitter.setRequiresReply(true);
	}


	@Test
	public void splitDocument() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<orders><order>one</order><order>two</order><order>three</order></orders>");
		this.splitter.handleMessage(new GenericMessage<>(doc));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertThat(message.getPayload(), instanceOf(Node.class));
			assertThat(message.getPayload(), not(instanceOf(Document.class)));
			assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize(), greaterThan(0));
		}
	}

	@Test(expected = ReplyRequiredException.class)
	public void splitDocumentThatDoesNotMatch() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<wrongDocument/>");
		this.splitter.handleMessage(new GenericMessage<>(doc));
	}

	@Test
	public void splitDocumentWithCreateDocumentsTrue() throws Exception {
		this.splitter.setCreateDocuments(true);
		Document doc = XmlTestUtil.getDocumentForString("<orders><order>one</order><order>two</order><order>three</order></orders>");
		this.splitter.handleMessage(new GenericMessage<>(doc));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertThat(message.getPayload(), instanceOf(Document.class));
			Document docPayload = (Document) message.getPayload();
			assertEquals("Wrong root element name", "order", docPayload.getDocumentElement().getLocalName());
			assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize(), greaterThan(0));
		}
	}

	@Test
	public void splitStringXml() throws Exception {
		String payload = "<orders><order>one</order><order>two</order><order>three</order></orders>";
		this.splitter.handleMessage(new GenericMessage<>(payload));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertEquals("Wrong number of messages", 3, docMessages.size());
		for (Message<?> message : docMessages) {
			assertThat(message.getPayload(), instanceOf(String.class));
			assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize(), greaterThan(0));
		}
	}

	@Test(expected = MessageHandlingException.class)
	public void invalidPayloadType() {
		this.splitter.handleMessage(new GenericMessage<>(123));
	}

}
