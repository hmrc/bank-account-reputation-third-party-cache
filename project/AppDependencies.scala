import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(

    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.27.0-play-26",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.9.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"         % "1.9.0" % Test classifier "tests",
    "org.scalatest"           %% "scalatest"                 % "3.0.8"                 % "test",
    "com.typesafe.play"       %% "play-test"                 % current                 % "test",
    "org.pegdown"             %  "pegdown"                   % "1.6.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"        % "3.1.3"                 % "test, it",
//    "org.scalatestplus"       %% "mockito-3-3"               % "3.0.8.0"               % "test, it",
    "org.mockito" % "mockito-all" % "1.10.19" % "test, it",
    "uk.gov.hmrc"             %% "reactivemongo-test"        % "4.19.0-play-26"        % "test, it"
  )
}
