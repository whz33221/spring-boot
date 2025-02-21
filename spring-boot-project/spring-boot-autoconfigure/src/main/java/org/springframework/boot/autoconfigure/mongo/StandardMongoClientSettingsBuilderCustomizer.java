/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SslSettings;
import org.bson.UuidRepresentation;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * A {@link MongoClientSettingsBuilderCustomizer} that applies standard settings to a
 * {@link MongoClientSettings}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class StandardMongoClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer, Ordered {

	private final ConnectionString connectionString;

	private final UuidRepresentation uuidRepresentation;

	private final MongoConnectionDetails connectionDetails;

	private final MongoProperties.Ssl ssl;

	private final SslBundles sslBundles;

	private int order = 0;

	/**
	 * Create a new instance.
	 * @param connectionString the connection string
	 * @param uuidRepresentation the uuid representation
	 * @param ssl the ssl properties
	 * @param sslBundles the ssl bundles
	 * @deprecated since 3.5.0 for removal in 3.7.0 in favor of
	 * {@link #StandardMongoClientSettingsBuilderCustomizer(MongoConnectionDetails, UuidRepresentation)}
	 */
	@Deprecated(forRemoval = true, since = "3.5.0")
	public StandardMongoClientSettingsBuilderCustomizer(ConnectionString connectionString,
			UuidRepresentation uuidRepresentation, MongoProperties.Ssl ssl, SslBundles sslBundles) {
		this.connectionDetails = null;
		this.connectionString = connectionString;
		this.uuidRepresentation = uuidRepresentation;
		this.ssl = ssl;
		this.sslBundles = sslBundles;
	}

	public StandardMongoClientSettingsBuilderCustomizer(MongoConnectionDetails connectionDetails,
			UuidRepresentation uuidRepresentation) {
		this.connectionString = null;
		this.ssl = null;
		this.sslBundles = null;
		this.connectionDetails = connectionDetails;
		this.uuidRepresentation = uuidRepresentation;
	}

	@Override
	public void customize(MongoClientSettings.Builder settingsBuilder) {
		settingsBuilder.uuidRepresentation(this.uuidRepresentation);
		if (this.connectionDetails != null) {
			settingsBuilder.applyConnectionString(this.connectionDetails.getConnectionString());
			settingsBuilder.applyToSslSettings(this::configureSslIfNeeded);
		}
		else {
			settingsBuilder.uuidRepresentation(this.uuidRepresentation);
			settingsBuilder.applyConnectionString(this.connectionString);
			if (this.ssl.isEnabled()) {
				settingsBuilder.applyToSslSettings(this::configureSsl);
			}
		}
	}

	private void configureSsl(SslSettings.Builder settings) {
		settings.enabled(true);
		if (this.ssl.getBundle() != null) {
			SslBundle sslBundle = this.sslBundles.getBundle(this.ssl.getBundle());
			Assert.state(!sslBundle.getOptions().isSpecified(), "SSL options cannot be specified with MongoDB");
			settings.context(sslBundle.createSslContext());
		}
	}

	private void configureSslIfNeeded(SslSettings.Builder settings) {
		SslBundle sslBundle = this.connectionDetails.getSslBundle();
		if (sslBundle != null) {
			settings.enabled(true);
			Assert.state(!sslBundle.getOptions().isSpecified(), "SSL options cannot be specified with MongoDB");
			settings.context(sslBundle.createSslContext());
		}
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order value of this object.
	 * @param order the new order value
	 * @see #getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
