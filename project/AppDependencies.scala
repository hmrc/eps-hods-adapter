import sbt.*
import play.sbt.PlayImport.*

object AppDependencies {

  private val playVersion = "play-30"
  private val hmrcMongoVersion = "2.12.0"
  private val bootstrapVersion = "10.7.0"
  private val pekkoVersion: String = "1.0.3"
  private val domainVersion = "13.0.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    ws,
    "org.typelevel"     %% "cats-core"                               % "2.13.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-work-item-repo-$playVersion" % hmrcMongoVersion,
    "uk.gov.hmrc"       %% s"bootstrap-backend-$playVersion"         % bootstrapVersion,
    "uk.gov.hmrc"       %% s"domain-$playVersion"                    % domainVersion
  )
  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % Test,
    "org.scalatestplus" %% "mockito-3-4"                   % "3.2.10.0"       % Test,
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"  % bootstrapVersion % Test,
    "uk.gov.hmrc"       %% s"domain-test-$playVersion"     % domainVersion    % Test,

    // Core Pekko Stream testkit
    "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
    // Classic actor testkit (for TestKit base class)
    "org.apache.pekko" %% "pekko-testkit"             % pekkoVersion % Test,
    "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test
  )
}
