/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.handler.advice;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice.MessageHandlingExpressionEvaluatingAdviceException;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
@RunWith(SpringRunner.class)
public class ExpressionEvaluatingRequestHandlerAdviceTests {

	@Autowired
	@Qualifier("advised.input")
	private MessageChannel in;

	@Autowired
	private EERHAConfig config;

	@Test
	public void test() {
		this.in.send(new GenericMessage<>("good"));
		this.in.send(new GenericMessage<>("junk"));
		assertThat(config.successful, instanceOf(AdviceMessage.class));
		assertThat(config.successful.getPayload(), equalTo("good was successful"));
		assertThat(config.failed, instanceOf(ErrorMessage.class));
		Object evaluationResult = ((MessageHandlingExpressionEvaluatingAdviceException) config.failed.getPayload())
				.getEvaluationResult();
		assertThat((String) evaluationResult, startsWith("junk was bad, with reason:"));
	}

	@Configuration
	@EnableIntegration
	public static class EERHAConfig {

		@Bean
		public IntegrationFlow advised() {
			return f -> f.handle((GenericHandler<String>) (payload, headers) -> {
				if (payload.equals("good")) {
					return null;
				}
				else {
					throw new RuntimeException("some failure");
				}
			}, c -> c.advice(expressionAdvice()));
		}

		@Bean
		public Advice expressionAdvice() {
			ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
			advice.setSuccessChannelName("success.input");
			advice.setOnSuccessExpressionString("payload + ' was successful'");
			advice.setFailureChannelName("failure.input");
			advice.setOnFailureExpressionString(
					"payload + ' was bad, with reason: ' + #exception.cause.message");
			advice.setTrapException(true);
			return advice;
		}

		private Message<?> successful;

		@Bean
		public IntegrationFlow success() {
			return f -> f
					.handle(m -> this.successful = m);
		}

		private Message<?> failed;

		@Bean
		public IntegrationFlow failure() {
			return f -> f
					.handle(m -> this.failed = m);
		}

	}

}
