/**
 * Copyright Indra Soluciones Tecnologías de la Información, S.L.U.
 * 2013-2019 SPAIN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*******************************************************************************
 * Â© Indra Sistemas, S.A.
 * 2013 - 2018  SPAIN
 *
 * All rights reserved
 ******************************************************************************/
package com.minsait.onesait.platform.commons.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.ToString;

@ToString
public class ContextData implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	private String deviceTemplate;
	@Getter
	private String device;
	@Getter
	private String clientConnection;
	@Getter
	private String clientSession;
	@Getter
	final private String user;
	@Getter
	final private String timezoneId;
	@Getter
	final private String timestamp;
	@Getter
	final private long timestampMillis;
	@Getter
	final private String source;

	public ContextData(JsonNode node) {

		final JsonNode deviceTemplate = node.findValue("deviceTemplate");
		if (deviceTemplate != null) {
			this.deviceTemplate = deviceTemplate.asText();
		} else {
			this.deviceTemplate = "";
		}

		final JsonNode device = node.findValue("device");
		if (device != null) {
			this.device = device.asText();
		} else {
			this.device = "";
		}

		final JsonNode clientConnection = node.findValue("clientConnection");
		if (clientConnection != null) {
			this.clientConnection = clientConnection.asText();
		} else {
			this.clientConnection = "";
		}

		final JsonNode clientSession = node.findValue("clientSession");
		if (clientSession != null) {
			this.clientSession = clientSession.asText();
		} else {
			this.clientSession = "";
		}

		final JsonNode user = node.findValue("user");
		if (user != null) {
			this.user = user.asText();
		} else {
			this.user = "";
		}

		final JsonNode timezoneId = node.findValue("timezoneId");
		if (timezoneId != null) {
			this.timezoneId = timezoneId.asText();
		} else {
			this.timezoneId = CalendarAdapter.getServerTimezoneId();
		}

		final JsonNode timestamp = node.findValue("timestamp");
		if (timestamp != null) {
			this.timestamp = timestamp.asText();
		} else {
			this.timestamp = Calendar.getInstance(TimeZone.getTimeZone(this.timezoneId)).getTime().toString();
		}
		final JsonNode timestampMillis = node.findValue("timestampMillis");
		if (timestampMillis != null) {
			this.timestampMillis = timestampMillis.asLong();
		} else {
			this.timestampMillis = System.currentTimeMillis();
		}
		final JsonNode source = node.findValue("source");
		if (source != null) {
			this.source = source.toString();
		} else {
			this.source = "";
		}
	}

	public ContextData(ContextData other) {
		user = other.user;
		deviceTemplate = other.deviceTemplate;
		device = other.device;
		clientConnection = other.clientConnection;
		clientSession = other.clientSession;
		timezoneId = other.timezoneId;
		timestamp = other.timestamp;
		timestampMillis = other.timestampMillis;
		source = other.source;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof ContextData))
			return false;
		final ContextData that = (ContextData) other;
		return Objects.equals(user, that.user) && Objects.equals(device, that.device)
				&& Objects.equals(deviceTemplate, that.deviceTemplate)
				&& Objects.equals(clientConnection, that.clientConnection)
				&& Objects.equals(clientSession, that.clientSession) && Objects.equals(timezoneId, that.timezoneId)
				&& Objects.equals(timestamp, that.timestamp) && Objects.equals(timestampMillis, that.timestampMillis)
				&& Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(user, deviceTemplate, device, clientConnection, clientSession, timezoneId, timestamp,
				source);
	}

	private ContextData(Builder build) {
		user = build.user;
		timezoneId = build.timezoneId;
		timestamp = build.timestamp;
		clientConnection = build.clientConnection;
		deviceTemplate = build.deviceTemplate;
		device = build.device;
		clientSession = build.clientSession;
		timestampMillis = build.timestampMillis;
		source = build.source;
	}

	public static Builder builder(String user, String timezoneId, String timestamp, long timestampMillis,
			String source) {
		return new Builder(user, timezoneId, timestamp, timestampMillis, source);
	}

	public static class Builder {
		private String deviceTemplate;
		private String device;
		private String clientConnection;
		private String clientSession;
		private final String user;
		private final String timezoneId;
		private final String timestamp;
		private final long timestampMillis;
		private final String source;

		public Builder(String user, String timezoneId, String timestamp, long timestampMillis, String source) {
			this.user = user;
			this.timezoneId = timezoneId;
			this.timestamp = timestamp;
			this.timestampMillis = timestampMillis;
			this.source = source;
		}

		public ContextData build() {
			return new ContextData(this);
		}

		public Builder clientSession(String clientSession) {
			this.clientSession = clientSession;
			return this;
		}

		public Builder clientConnection(String clientConnection) {
			this.clientConnection = clientConnection;
			return this;
		}

		public Builder device(String device) {
			this.device = device;
			return this;
		}

		public Builder deviceTemplate(String deviceTemplate) {
			this.deviceTemplate = deviceTemplate;
			return this;
		}
	}
}
