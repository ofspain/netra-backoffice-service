package scalas.modules

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import scalas.services.FlywayInitializer
import security.{Secured, SecurityConfig, UserContext}
import services.{FinancialInstitutionService, S3Service}
import services.db.JdbcWrapper
import org.pac4j.core.config.Config
import utilities.rest.{RestClientConfig, RestClientService}

import javax.inject.Provider

class ConfigModule extends Module {
  override def bindings(env: Environment, conf: Configuration): Seq[Binding[_]] = {
    println("✅ ConfigModule is being loaded - security bindings active")
    Seq(
      bind[FlywayInitializer].toSelf.eagerly(),
      bind[JdbcWrapper].toSelf.eagerly(),
      bind[S3Service].toSelf.eagerly(),
      bind[FinancialInstitutionService].toSelf,

      //rest utils
      bind[RestClientConfig].toSelf.eagerly(),
      bind[RestClientService].toSelf.eagerly(),

      // Security beans
      bind[SecurityConfig].toSelf.eagerly(),
      bind[UserContext].toSelf.eagerly(),
      bind[Secured].toSelf,

      // CRITICAL: Config binding - FIXED SYNTAX
      bind[Config].toProvider(classOf[security.ConfigProvider]).eagerly(),

      // CRITICAL: SecurityFilter binding - FIXED SYNTAX
      bind[org.pac4j.play.filters.SecurityFilter].toProvider(classOf[security.SecurityFilterProvider]).eagerly()
    )
  }
}