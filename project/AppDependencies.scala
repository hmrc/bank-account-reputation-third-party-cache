import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"        % "0.51.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28" % "5.10.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                 % "3.1.4"                 % "test",
    "com.typesafe.play"       %% "play-test"                 % current                 % "test",
    "org.pegdown"             %  "pegdown"                   % "1.6.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"        % "5.1.0"                 % "test, it",
    "org.scalatestplus"       %% "mockito-3-4"               % "3.1.4.0"               % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"              % "0.35.10"               % "test, it"
  )
}