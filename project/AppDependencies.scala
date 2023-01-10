import play.core.PlayVersion.current
import sbt._

object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "0.74.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28" % "7.12.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                 % "3.2.12"               % "test",
    "com.typesafe.play"       %% "play-test"                 % current                % "test",
    "org.pegdown"             %  "pegdown"                   % "1.6.0"                % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"        % "5.1.0"                % "test, it",
    "org.scalatestplus"       % "mockito-4-6_2.13"           % "3.2.14.0"             % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"              % "0.62.2"               % "test, it"
  )
}
