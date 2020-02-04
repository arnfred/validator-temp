package validator

import unfiltered.netty._
import util.Properties

object Main {
  def main(args : Array[String]): Unit= Validator.init.run
}



object Validator {

  def init = {
    val resourceDir = new java.io.File("resources/")
    val port = Properties.envOrElse("PORT", "7777").toInt
    println("starting on port: " + port)

    var srv = unfiltered.netty.Server.http(port).resources(resourceDir.toURI.toURL);
    srv.handler(validator.Plan)
  }

}


