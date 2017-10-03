package com.global.snapshot

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging.LogLevel
import com.global.snapshot.actors.CargoStations
import com.global.snapshot.actors.CargoStations.{Join, Leave, Start, Stop}

import scala.annotation.tailrec
import scala.io.StdIn

object Main extends App {

  val system = ActorSystem("cargo")

  try {
    val stations = system.actorOf(CargoStations.props, "stations")
    stations ! Start

    println("> Press ENTER to switch to the CLI")
    readCommand(stations)

    stations ! Stop
  } finally {
    system.terminate()
  }

  @tailrec
  private def readCommand(stations: ActorRef): Unit = {
    val command = StdIn.readLine()
    val commands = "Type in one of the commands: log, join, leave, marker, exit"
    command match {
      case "log" =>
        println("Started logging the actors")
        system.eventStream.setLogLevel(LogLevel(4))
      case "join" =>
        println("Berlin station is joining")
        stations ! Join
      case "leave" =>
        println("Berlin station is leaving")
        stations ! Leave
      case "marker" =>
        println("Sending out the marker")
      case "exit" =>
        println("Shutting down the stations")
      case "" =>
        system.eventStream.setLogLevel(LogLevel(1))
        println(">Logs are off. " + commands)
      case unknown => println(s">Don't know how to $unknown something. $commands")
    }
    if (command == "exit") () else readCommand(stations)
  }
}
