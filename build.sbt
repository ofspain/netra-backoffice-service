import scala.jdk.CollectionConverters.asScalaBufferConverter
import com.typesafe.config.ConfigFactory

name := """back-office-service"""
organization := "com.nestra"

version := "1.0.0"
lazy val root = (project in file(".")).enablePlugins(PlayJava, FlywayPlugin)

resolvers += Resolver.mavenLocal


scalaVersion := "2.13.16"

javacOptions ++= Seq(
  "--release", "21"
)

Compile / compile / scalacOptions ++= Seq(
  "-target:jvm-21"
)

val conf = ConfigFactory.parseFile(new File("conf/application.conf"))
flywayLocations := Seq(conf.getStringList("flyway.locations").asScala:_*)

libraryDependencies ++= Seq(
  guice,
  "org.postgresql" % "postgresql" % "42.7.7" % "runtime",
  "org.flywaydb" % "flyway-core" % "11.11.1",
  "org.flywaydb" % "flyway-database-postgresql" % "11.11.1" % "runtime",
  "com.netra" % "commons-netra" % "0.0.1-a"


)


dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % "2.14.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.3"
)