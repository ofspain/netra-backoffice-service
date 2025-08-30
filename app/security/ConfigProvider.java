package security;

import org.pac4j.core.config.Config;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ConfigProvider implements Provider<Config> {

    private final SecurityConfig securityConfig;

    @Inject
    public ConfigProvider(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    @Override
    public Config get() {
        return securityConfig.getConfig();
    }
}