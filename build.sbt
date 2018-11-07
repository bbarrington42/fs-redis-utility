import sbtassembly.AssemblyPlugin.defaultShellScript

assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript))

assemblyJarName in assembly := "fs-user-migration"


name := "fs-redis-utility"


version := "0.1"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-feature",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)


libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.play" %% "play-json" % "2.6.10",
  "com.typesafe.config" %% "config" % "0.1.7",
  "redis.clients" % "jedis" % "2.9.0",
  "org.scalaz" %% "scalaz-core" % "7.2.26"
)
