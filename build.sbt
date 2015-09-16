import SiteKeys.siteSourceDirectory

lazy val commonSettings = Seq(
  organization := "io.swagger",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-Xfatal-warnings", "-target:jvm-1.7"),
  conflictManager := ConflictManager.strict,
  dependencyOverrides <++= scalaVersion { v => Set(
    "org.scala-lang" % "scala-library" % v,
    "org.scala-lang" % "scala-compiler" % v,
    "org.scala-lang" % "scala-reflect" % v
  )}
)

val projectOrg = "io.swagger"
val scalaVsn = "2.11.7"

site.settings
siteSourceDirectory := file("site")
site.includeScaladoc()
ghpages.settings
git.remoteRepo := "git@github.com:2rs2ts/swagger-gdd.git"

lazy val `swagger-gdd` = (project in file(".")).
  aggregate(`swagger-gdd-models`, `swagger-gdd-converters`).
  settings(commonSettings: _*).
  settings(
    name := "swagger-gdd"
  )

lazy val `swagger-gdd-models` = (project in file("models")).
  settings(commonSettings: _*).
  settings(
    name := "swagger-gdd-models"
  )

lazy val `swagger-gdd-converters` = (project in file("converters")).
  dependsOn(`swagger-gdd-models`).
  settings(commonSettings: _*).
  settings(
    name := "swagger-gdd-converters",
    libraryDependencies ++= Seq(
      "io.swagger" % "swagger-models" % "1.5.3" exclude("com.fasterxml.jackson.core", "jackson-annotations"),
      "io.swagger" % "swagger-parser" % "1.0.10" exclude("com.fasterxml.jackson.core", "jackson-core") exclude("com.fasterxml.jackson.core", "jackson-databind") exclude("com.fasterxml.jackson.core", "jackson-annotations") exclude("io.swagger", "swagger-models"),
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.0",
      "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
      "org.specs2" %% "specs2-core" % "3.6.4" % "test",
      "org.specs2" %% "specs2-scalacheck" % "3.6.4" % "test",
      "com.paypal" %% "cascade-common" % "0.5.0" % "test" classifier "tests" exclude("org.slf4j", "slf4j-api") exclude("com.fasterxml.jackson.datatype", "jackson-datatype-joda")
    )
  )
