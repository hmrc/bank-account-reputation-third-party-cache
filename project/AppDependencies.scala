import play.core.PlayVersion.current
import sbt._

object AppDependencies {
  private val bootstrapPlayVersion = "8.1.0"

  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % "1.8.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30" % bootstrapPlayVersion
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % "test"
  )
}
