package com.global.snapshot

import akka.actor.ActorSystem
import com.global.snapshot.CargoStation.Connect

import scala.io.StdIn

object Main extends App {

  val system = ActorSystem("cargo-network")

  try {
    val stockholm = system.actorOf(CargoStation.props("Stockholm", Config.cargoStationsCount), "stockholm")
    val copenhagen = system.actorOf(CargoStation.props("Copenhagen", Config.cargoStationsCount), "copenhagen")
    val helsinki = system.actorOf(CargoStation.props("Helsinki", Config.cargoStationsCount), "helsinki")
    val oslo = system.actorOf(CargoStation.props("Oslo", Config.cargoStationsCount), "oslo")

    stockholm ! Connect(
      incomingChannels = Set(copenhagen, helsinki),
      outgoingChannels = Set(oslo)
    )
    copenhagen ! Connect(
      incomingChannels = Set(oslo),
      outgoingChannels = Set(stockholm, helsinki)
    )
    helsinki ! Connect(
      incomingChannels = Set(copenhagen),
      outgoingChannels = Set(stockholm, oslo)
    )
    oslo ! Connect(
      incomingChannels = Set(stockholm, helsinki),
      outgoingChannels = Set(copenhagen)
    )

    println(">>> Press ENTER to exit <<<")
    StdIn.readLine()
  } finally {
    system.terminate()
  }
}
