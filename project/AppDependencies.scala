import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc"             %% "simple-reactivemongo"      % "7.30.0-play-27",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27" % "2.23.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                 % "3.0.8"                 % "test",
    "com.typesafe.play"       %% "play-test"                 % current                 % "test",
    "org.pegdown"             %  "pegdown"                   % "1.6.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"        % "4.0.3"                 % "test, it",
    "org.mockito" % "mockito-all"                            % "1.10.19"               % "test, it",
    "uk.gov.hmrc"             %% "reactivemongo-test"        % "4.21.0-play-27"        % "test, it"
  )
}
