import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  override val appName = "api-gatekeeper-frontend"

  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion

  private val slf4jVersion = "1.7.22"
  private val logbackVersion = "1.1.9"
  private val log4j2AdapterVersion = "2.7"

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "7.10.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "6.2.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc" %% "play-config" % "3.0.0",
    "uk.gov.hmrc" %% "logback-json-logger" % "3.1.0",
    "uk.gov.hmrc" %% "json-encryption" % "3.1.0",
    "uk.gov.hmrc" %% "play-health" % "2.0.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.0.0",
    "uk.gov.hmrc" %% "play-ui" % "5.3.0",
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
    "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "ch.qos.logback" % "logback-core" % logbackVersion,
    "org.apache.logging.log4j" % "log4j-to-slf4j" % log4j2AdapterVersion,
    "org.apache.logging.log4j" % "log4j-1.2-api" % log4j2AdapterVersion,
    "org.apache.logging.log4j" % "log4j-api" % log4j2AdapterVersion
  ).map(
    _.excludeAll(
      ExclusionRule(organization = "commons-logging"),
      ExclusionRule(organization = "log4j"),
      ExclusionRule("org.apache.logging.log4j", "log4j-core"),
      ExclusionRule("org.slf4j", "slf4j-log412"),
      ExclusionRule("org.slf4j", "slf4j-jdk14")
    ))

  abstract class TestDependencies(scope: String) {
    lazy val test: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % "2.2.6" % scope,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
      "org.pegdown" % "pegdown" % "1.6.0" % scope,
      "org.jsoup" % "jsoup" % "1.10.1" % scope,
      "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
      "uk.gov.hmrc" %% "hmrctest" % "2.2.0" % scope,
      "com.github.tomakehurst" % "wiremock" % "1.58" % scope,
      "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % scope,
      "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % scope,
      "org.mockito" % "mockito-all" % "1.10.19" % scope,
      "org.scalacheck" %% "scalacheck" % "1.12.6" % scope
    ).map(
      _.excludeAll(
        ExclusionRule(organization = "commons-logging"),
        ExclusionRule(organization = "log4j"),
        ExclusionRule("org.apache.logging.log4j", "log4j-core"),
        ExclusionRule("org.slf4j", "slf4j-log412"),
        ExclusionRule("org.slf4j", "slf4j-jdk14")
      ))
  }

  object Test extends TestDependencies("test")

  object AcceptanceTest extends TestDependencies("acceptance")

  def apply() = compile ++ Test.test ++ AcceptanceTest.test
}





