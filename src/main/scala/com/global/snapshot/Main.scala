package com.global.snapshot

import akka.actor.ActorSystem
import com.global.snapshot.actos.CargoManager
import com.global.snapshot.actos.CargoManager.{Start, Stop}

import scala.io.StdIn

object Main extends App {

  val system = ActorSystem("cargo-network")

  try {
    val cargoManager = system.actorOf(CargoManager.props, "cargoManager")
    cargoManager ! Start

    println(">>> Press ENTER to exit <<<")
    StdIn.readLine()

    cargoManager ! Stop
  } finally {
    system.terminate()
  }
}
