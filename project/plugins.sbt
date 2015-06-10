// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// The Play plugins
addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.8")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
