import play.Project._

name := """hello-play-java"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.2", 
  "org.webjars" % "bootstrap" % "2.3.1",
  "org.pac4j" % "play-pac4j_java" % "1.2.2",
  "org.pac4j" % "pac4j-oauth" % "1.6.0",
  "org.pac4j" % "pac4j-openid" % "1.6.0",
  "org.pac4j" % "pac4j-gae" % "1.6.0")

resolvers ++= Seq(
  "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

playJavaSettings
