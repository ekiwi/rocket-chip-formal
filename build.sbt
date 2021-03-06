name := "paso2"
version := "0.1"
scalaVersion := "2.12.15"

scalacOptions ++= Seq(
  "-language:reflectiveCalls",
  "-deprecation",
  "-feature",
  "-Xcheckinit",
)

// SNAPSHOT repositories
resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies += "edu.berkeley.cs" %% "rocketchip" % "1.5-SNAPSHOT"
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5-SNAPSHOT"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5-SNAPSHOT" % Test
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5-SNAPSHOT" cross CrossVersion.full)

Compile / scalaSource := baseDirectory.value / "src"
Test / scalaSource := baseDirectory.value / "test"

// LazyModules / Diplomacy have massive problems with multi-threading
Test / parallelExecution:= false