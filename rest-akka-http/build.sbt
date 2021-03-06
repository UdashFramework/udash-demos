import sbtcrossproject.{crossProject, CrossType}

name := "rest-akka-http"

inThisBuild(Seq(
  version := "0.7.0-SNAPSHOT",
  scalaVersion := "2.12.6",
  organization := "io.udash",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:dynamics",
    "-Xfuture",
    "-Xfatal-warnings",
    "-Xlint:_,-missing-interpolator,-adapted-args"
  ),
))

// Custom SBT tasks
val copyAssets = taskKey[Unit]("Copies all assets to the target directory.")
val compileStatics = taskKey[File](
  "Compiles JavaScript files and copies all assets to the target directory."
)
val compileAndOptimizeStatics = taskKey[File](
  "Compiles and optimizes JavaScript files and copies all assets to the target directory."
)

lazy val `rest-akka-http` = project.in(file("."))
  .aggregate(sharedJS, sharedJVM, frontend, backend)
  .dependsOn(backend)
  .settings(
    publishArtifact := false,
    Compile / mainClass := Some("io.udash.demos.rest.Launcher"),
  )

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure).in(file("shared"))
  .settings(
    libraryDependencies ++= Dependencies.crossDeps.value,
  )

lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js

lazy val backend = project.in(file("backend"))
  .dependsOn(sharedJVM)
  .settings(
    libraryDependencies ++= Dependencies.backendDeps.value,
    Compile / mainClass := Some("io.udash.demos.rest.Launcher"),
  )

val frontendWebContent = "UdashStatics/WebContent"
lazy val frontend = project.in(file("frontend")).enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    libraryDependencies ++= Dependencies.frontendDeps.value,

    // Make this module executable in JS
    Compile / mainClass := Some("io.udash.demos.rest.JSLauncher"),
    scalaJSUseMainModuleInitializer := true,

    // Implementation of custom tasks defined above
    copyAssets := {
      IO.copyDirectory(
        sourceDirectory.value / "main/assets",
        target.value / frontendWebContent / "assets"
      )
      IO.copyFile(
        sourceDirectory.value / "main/assets/index.html",
        target.value / frontendWebContent / "index.html"
      )
    },

    // Compiles JS files without full optimizations
    compileStatics := { (Compile / fastOptJS / target).value / "UdashStatics" },
    compileStatics := compileStatics.dependsOn(
      Compile / fastOptJS, Compile / copyAssets
    ).value,

    // Compiles JS files with full optimizations
    compileAndOptimizeStatics := { (Compile / fullOptJS / target).value / "UdashStatics" },
    compileAndOptimizeStatics := compileAndOptimizeStatics.dependsOn(
      Compile / fullOptJS, Compile / copyAssets
    ).value,

    // Target files for Scala.js plugin
    Compile / fastOptJS / artifactPath :=
      (Compile / fastOptJS / target).value /
        frontendWebContent / "scripts" / "frontend.js",
    Compile / fullOptJS / artifactPath :=
      (Compile / fullOptJS / target).value /
        frontendWebContent / "scripts" / "frontend.js",
    Compile / packageJSDependencies / artifactPath :=
      (Compile / packageJSDependencies / target).value /
        frontendWebContent / "scripts" / "frontend-deps.js",
    Compile / packageMinifiedJSDependencies / artifactPath :=
      (Compile / packageMinifiedJSDependencies / target).value /
        frontendWebContent / "scripts" / "frontend-deps.js"
  )