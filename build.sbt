import scala.jdk.CollectionConverters.asScalaBufferConverter
import com.typesafe.config.ConfigFactory

name := """back-office-service"""
organization := "com.nestra"
version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)
//, FlywayPlugin

resolvers += Resolver.mavenLocal

scalaVersion := "2.13.16"

javacOptions ++= Seq("--release", "21")
Compile / compile / scalacOptions ++= Seq("-target:jvm-21")

val conf = ConfigFactory.parseFile(new File("conf/application.conf"))
flywayLocations := Seq(conf.getStringList("flyway.locations").asScala: _*)

lazy val pac4jVersion = "6.2.1"

libraryDependencies ++= Seq(
  guice,
  javaJdbc,
  ws,
  javaWs,
  "org.postgresql" % "postgresql" % "42.7.7",
  "org.flywaydb" % "flyway-core" % "11.11.1",
  "org.flywaydb" % "flyway-database-postgresql" % "11.11.1" % Runtime,
  "com.netra" % "commons-netra" % "0.0.1-a"
    exclude("org.springframework.boot", "spring-boot-starter-validation")
    exclude("javax.validation", "validation-api")
    exclude("org.hibernate.validator", "hibernate-validator")
    exclude("org.springframework", "spring-context")
    exclude("org.springframework", "spring-core")
    exclude("org.springframework", "spring-beans")
    exclude("org.springframework", "spring-expression"),
    "software.amazon.awssdk" % "s3" % "2.32.26",
  "org.pac4j" %% "play-pac4j" % "13.0.0-PLAY3.0",
  "org.pac4j" % "pac4j-http" % pac4jVersion,
  "org.pac4j" % "pac4j-jwt"  % pac4jVersion,
  "com.typesafe.play" %% "play-ahc-ws" % "2.9.6",
  "net.coobird" % "thumbnailator" % "0.4.20",
  "com.auth0" % "java-jwt" % "4.5.0"


//  "org.hibernate.validator" % "hibernate-validator" % "6.2.5.Final",
//  "org.glassfish" % "jakarta.el" % "4.0.0",  "org.pac4j" % "pac4j-http" % "4.0.3" // For header-based auth
//  "javax.validation" % "validation-api" % "2.0.1.Final",
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % "2.14.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.3",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.14.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.3",
  "org.pac4j" % "pac4j-core" % pac4jVersion,
  "org.pac4j" % "pac4j-http" % pac4jVersion,
  "org.pac4j" % "pac4j-jwt"  % pac4jVersion
//
//  "javax.validation" % "validation-api" % "2.0.1.Final",
//  "org.hibernate.validator" % "hibernate-validator" % "6.2.5.Final"
)
