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

package org.springframework.integration.jpa.inbound;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.util.Assert;

/**
 * Polling message source that produces messages from the result of the provided:
 *
 * <ul>
 *     <li>entityClass</li>
 *     <li>JpQl Select Query</li>
 *     <li>Sql Native Query</li>
 *     <li>JpQl Named Query</li>
 *     <li>Sql Native Named Query</li>
 * </ul>
 *
 * After the objects have been polled, it also possibly to either:
 *
 * executes an update after the select possibly to updated the state of selected records
 *
 * <ul>
 *     <li>executes an update (per retrieved object or for the entire payload)</li>
 *     <li>delete the retrieved object</li>
 * </ul>
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class JpaPollingChannelAdapter extends AbstractMessageSource<Object> {

	private final JpaExecutor jpaExecutor;

	/**
	 * Constructor taking a {@link JpaExecutor} that provide all required JPA
	 * functionality.
	 *
	 * @param jpaExecutor Must not be null.
	 */
	public JpaPollingChannelAdapter(JpaExecutor jpaExecutor) {
		Assert.notNull(jpaExecutor, "jpaExecutor must not be null.");
		this.jpaExecutor = jpaExecutor;
	}

	/**
	 * Check for mandatory attributes
	 */
	@Override
	protected void onInit() {
		this.jpaExecutor.setBeanFactory(getBeanFactory());
	}

	/**
	 * Use {@link JpaExecutor#poll()} to executes the JPA operation.
	 * If {@link JpaExecutor#poll()} returns null, this method will return
	 * <code>null</code>. Otherwise, a new {@link org.springframework.messaging.Message}
	 * is constructed and returned.
	 */
	@Override
	protected Object doReceive() {
		return this.jpaExecutor.poll();
	}

	@Override
	public String getComponentType() {
		return "jpa:inbound-channel-adapter";
	}

}
