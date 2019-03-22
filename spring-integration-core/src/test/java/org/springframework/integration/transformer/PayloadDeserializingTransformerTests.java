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

package org.springframework.integration.transformer;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class PayloadDeserializingTransformerTests {

	@Test
	public void deserializeString() throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject("foo");
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		Message<?> result = transformer.transform(new GenericMessage<byte[]>(serialized));
		Object payload = result.getPayload();
		assertNotNull(payload);
		assertEquals(String.class, payload.getClass());
		assertEquals("foo", payload);
	}

	@Test
	public void deserializeObject() throws Exception {
		TestBean testBean = new TestBean("test");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(testBean);
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		Message<?> result = transformer.transform(new GenericMessage<byte[]>(serialized));
		Object payload = result.getPayload();
		assertNotNull(payload);
		assertEquals(TestBean.class, payload.getClass());
		assertEquals(testBean.name, ((TestBean) payload).name);
	}

	@Test
	public void deserializeObjectWhiteList() throws Exception {
		TestBean testBean = new TestBean("test");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(testBean);
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setWhiteListPatterns("com.*");
		try {
			transformer.transform(new GenericMessage<byte[]>(serialized));
			fail("expected security exception");
		}
		catch (MessageTransformationException e) {
			assertThat(e.getCause().getCause(), instanceOf(SecurityException.class));
			assertThat(e.getCause().getCause().getMessage(), startsWith("Attempt to deserialize unauthorized"));
		}
		transformer.setWhiteListPatterns("org.*");
		Message<?> result = transformer.transform(new GenericMessage<byte[]>(serialized));
		Object payload = result.getPayload();
		assertNotNull(payload);
		assertEquals(TestBean.class, payload.getClass());
		assertEquals(testBean.name, ((TestBean) payload).name);
	}

	@Test(expected = MessageTransformationException.class)
	public void invalidPayload() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.transform(new GenericMessage<byte[]>(bytes));
	}

	@Test
	public void customDeserializer() {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setConverter(source -> "Converted");
		Message<?> message = transformer.transform(MessageBuilder.withPayload("Test".getBytes()).build());
		assertEquals("Converted", message.getPayload());
	}

	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}
	}

}
