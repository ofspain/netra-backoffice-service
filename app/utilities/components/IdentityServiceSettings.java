package utilities.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import play.api.Configuration;


@Singleton
@Getter
public class IdentityServiceSettings {

    private final String authrexBaseUrl;
    private final String customerUserRegistrationPath;
    private final String loginPath;

    @Inject
    public IdentityServiceSettings(Configuration configuration){
        com.typesafe.config.Config underlyingConfig = configuration.underlying();

        boolean authrexPath = underlyingConfig.hasPath("services.authrex-path");
        com.typesafe.config.Config authrexPathConfig = authrexPath ? underlyingConfig.getConfig("services.authrex-path") : com.typesafe.config.ConfigFactory.empty();

        authrexBaseUrl = authrexPathConfig.hasPath("baseUrl") ? authrexPathConfig.getString("baseUrl") : "http://localhost:8080";
        customerUserRegistrationPath = authrexPathConfig.hasPath("customerUserRegistrationPath") ? authrexPathConfig.getString("customerUserRegistrationPath") :"/api/identities/registration";
        loginPath = authrexPathConfig.hasPath("loginPath") ? authrexPathConfig.getString("loginPath") :"/api/auth/login";

    }
}
