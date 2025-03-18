import sbt.*

object AppDependencies {
  private val bootstrapPlayVersion = "9.11.0"

  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % "2.5.0",
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapPlayVersion
  )

  val test = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapPlayVersion % "test"
  )
}
