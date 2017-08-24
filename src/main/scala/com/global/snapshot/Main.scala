package com.global.snapshot

import akka.actor.ActorSystem
import com.global.snapshot.actors.CargoStations
import com.global.snapshot.actors.CargoStations.{Join, Leave, Start, Stop}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {

  val system = ActorSystem("cargo")

  try {
    val stations = system.actorOf(CargoStations.props, "stations")
    stations ! Start

    system.scheduler.schedule(10 second, 1 minute) {
      stations ! Join
    }
    system.scheduler.schedule(40 seconds, 1 minute) {
      stations ! Leave
    }

    println(">>> Press ENTER to exit <<<")
    StdIn.readLine()

    stations ! Stop
  } finally {
    system.terminate()
  }
}
