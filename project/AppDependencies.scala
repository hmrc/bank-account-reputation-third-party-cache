import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "10.5.0"
  private val playSuffix           = "-play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo$playSuffix"         % "2.12.0",
    "uk.gov.hmrc"       %% s"bootstrap-backend$playSuffix"  % bootstrapPlayVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test$playSuffix"     % bootstrapPlayVersion
  ).map(_ % Test)
}
