package com.global.snapshot

import akka.actor.ActorSystem
import com.global.snapshot.actors.CargoStations
import com.global.snapshot.actors.CargoStations.{Start, Stop}

import scala.io.StdIn

object Main extends App {

  val system = ActorSystem("cargo")

  try {
    val stations = system.actorOf(CargoStations.props, "stations")
    stations ! Start

    println(">>> Press ENTER to exit <<<")
    StdIn.readLine()

    stations ! Stop
  } finally {
    system.terminate()
  }
}
