import uk.gov.hmrc.DefaultBuildSettings

val appName = "bank-account-reputation-third-party-cache"

ThisBuild / scalaVersion        := "2.13.16"
ThisBuild / majorVersion        := 0

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin, BuildInfoPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(
    scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s"
  )
  .settings( // https://github.com/sbt/sbt-buildinfo
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "buildinfo"
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(PlayKeys.playDefaultPort := 9899)
  .settings(resolvers += Resolver.jcenterRepo)

lazy val it = project.in(file("it"))
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(Test / fork := true)
