package utilities.rest;

import com.typesafe.config.Config;
import play.api.Configuration;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RestClientConfig {

    private final int defaultTimeout;
    private final int maxConnections;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final boolean enableRetry;
    private final int maxRetries;

    @Inject
    public RestClientConfig(Configuration configuration) {

        // Get the underlying Typesafe Config object
        Config underlyingConfig = configuration.underlying();

        // Check if rest.client section exists
        boolean hasRestConfig = underlyingConfig.hasPath("rest.client");
        Config restConfig = hasRestConfig ? underlyingConfig.getConfig("rest.client") : com.typesafe.config.ConfigFactory.empty();

        // Use Typesafe Config methods
        this.defaultTimeout = restConfig.hasPath("timeout") ? restConfig.getInt("timeout") : 30000;
        this.maxConnections = restConfig.hasPath("maxConnections") ? restConfig.getInt("maxConnections") : 100;
        this.connectionTimeout = restConfig.hasPath("connectionTimeout") ? restConfig.getInt("connectionTimeout") : 5000;
        this.socketTimeout = restConfig.hasPath("socketTimeout") ? restConfig.getInt("socketTimeout") : 30000;
        this.enableRetry = restConfig.hasPath("enableRetry") ? restConfig.getBoolean("enableRetry") : true;
        this.maxRetries = restConfig.hasPath("maxRetries") ? restConfig.getInt("maxRetries") : 3;
    }

    // Getters
    public int getDefaultTimeout() { return defaultTimeout; }
    public int getMaxConnections() { return maxConnections; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getSocketTimeout() { return socketTimeout; }
    public boolean isEnableRetry() { return enableRetry; }
    public int getMaxRetries() { return maxRetries; }
}
