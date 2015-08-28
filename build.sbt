val projectOrg = "io.swagger"
val projectName = "swagger-gdd"
val scalaVsn = "2.11.7"

libraryDependencies ++= Seq(
  "io.swagger" % "swagger-models" % "1.5.3" exclude("com.fasterxml.jackson.core", "jackson-annotations"),
  "io.swagger" % "swagger-parser" % "1.0.10" exclude("com.fasterxml.jackson.core", "jackson-core") exclude("com.fasterxml.jackson.core", "jackson-databind") exclude("com.fasterxml.jackson.core", "jackson-annotations") exclude("io.swagger", "swagger-models"),
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.0",
  "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  "org.specs2" %% "specs2-core" % "3.6.4" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.6.4" % "test"
)

lazy val `swagger-gdd` = (project in file(".")).
  settings(
    organization := projectOrg,
    name := projectName,
    scalaVersion := scalaVsn,
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xlint", "-Xfatal-warnings", "-target:jvm-1.7"),
    conflictManager := ConflictManager.strict,
    dependencyOverrides <++= scalaVersion { v => Set(
      "org.scala-lang" % "scala-library" % v,
      "org.scala-lang" % "scala-compiler" % v,
      "org.scala-lang" % "scala-reflect" % v
    )}
  )
