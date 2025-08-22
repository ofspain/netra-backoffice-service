package scalas.modules

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import scalas.services.FlywayInitializer
import services.S3Service
import services.db.JdbcWrapper


class ConfigModule extends Module {
  override def bindings(env: Environment, conf: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[FlywayInitializer].toSelf.eagerly(),  // ensures it runs at startup
      bind[JdbcWrapper].toSelf.eagerly(),
      bind[S3Service].toSelf.eagerly()
    )
  }
}