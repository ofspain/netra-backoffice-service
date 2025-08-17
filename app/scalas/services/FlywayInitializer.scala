package scalas.services

import org.flywaydb.core.Flyway
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class FlywayInitializer @Inject()(config: Configuration,lifecycle: ApplicationLifecycle) {
  private val logger = Logger(this.getClass)

  private val url       = config.get[String]("flyway.url")
  private val user      = config.get[String]("flyway.user")
  private val password  = config.get[String]("flyway.password")
  private val locations = config.getOptional[Seq[String]]("flyway.locations")
    .getOrElse(Seq("filesystem:conf/db/migration"))
  private val table     = config.getOptional[String]("flyway.table")
    .getOrElse("flyway_schema_backoffice_history")   // <-- separate table

  private val flyway = Flyway.configure()
    .dataSource(url, user, password)
    .locations(locations: _*)
    .table(table)
    .baselineOnMigrate(true)   // <-- important: accept existing schema
    .validateOnMigrate(true)
    .sqlMigrationPrefix("V")
    .load()

  try {
    logger.info(s">>> Starting Flyway migration (tracking table: $table)...")
    flyway.migrate()
    logger.info(">>> Flyway migration finished successfully.")
  } catch {
    case e: Exception =>
      logger.error(">>> Flyway migration failed!", e)
      throw new RuntimeException("Flyway migration failed", e)
  }

  lifecycle.addStopHook(() => Future.successful(()))
}
