package security;

import org.pac4j.play.filters.SecurityFilter;
import org.pac4j.core.config.Config;
import play.api.Configuration;
import org.apache.pekko.stream.Materializer;
import scala.concurrent.ExecutionContext;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class SecurityFilterProvider implements Provider<SecurityFilter> {

    private final SecurityConfig securityConfig;
    private final Configuration configuration;
    private final ExecutionContext executionContext;
    private final Materializer materializer;

    @Inject
    public SecurityFilterProvider(
            SecurityConfig securityConfig,
            Configuration configuration,
            ExecutionContext executionContext,
            Materializer materializer
    ) {
        this.securityConfig = securityConfig;
        this.configuration = configuration;
        this.executionContext = executionContext;
        this.materializer = materializer;
    }

    @Override
    public SecurityFilter get() {

        System.out.println("🎯 CREATING SecurityFilter with CUSTOM configuration");
        System.out.println("🎯 SecurityConfig: " + securityConfig.getClass().getName());

        Config config = securityConfig.getConfig();
        System.out.println("🎯 Pac4j Clients: " + config.getClients().getClients());

        return new SecurityFilter(
                configuration,
                securityConfig.getConfig(),
                executionContext,
                materializer
        );
    }
}