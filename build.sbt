import play.sbt.routes.RoutesKeys
import sbt.Keys.scalacOptions

val appName: String = "eps-hods-adapter"

Global / majorVersion := 2
Global / scalaVersion := "3.6.4"

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
val excludedPackages: Seq[String] = Seq(
  "<empty>",
  "Reverse.*",
  ".*Routes.*",
  ".*\\$anon.*",
  "uk.gov.hmrc.BuildInfo",
  ".*models.*",
  ".*javascript*",
  ".*JobScheduler*",
  "testOnly.*",
  "testOnlyDoNotUseInAppConf.*"
)

lazy val scoverageSettings = {
  import scoverage.*
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(","),
    ScoverageKeys.coverageMinimumStmtTotal := 86.0,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins *)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    PlayKeys.playDefaultPort := 9412,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Test / parallelExecution := false,
    Test / fork := false,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator
  )
  .settings(
    scalacOptions ++= List(
      // Silence "Flag -XXX set repeatedly"
      "-Wconf:msg=Flag.*repeatedly:s",
      // Silence unused warnings on Play `routes` files
      "-Wconf:src=routes/.*:s"
    )
  )
  .settings(RoutesKeys.routesImport ++= Seq("uk.gov.hmrc.hods.binders._", "uk.gov.hmrc.domain._"))
  .settings(scoverageSettings.settings *)

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions ++= List(
      // Silence "Flag -XXX set repeatedly"
      "-Wconf:msg=Flag.*repeatedly:s"
    )
  )

Test / test := (Test / test)
  .dependsOn(scalafmtCheckAll)
  .value

it / test := (it / Test / test)
  .dependsOn(scalafmtCheckAll, it / scalafmtCheckAll)
  .value
