package com.global.snapshot.actors

import akka.actor.Props
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.StartScheduler
import com.global.snapshot.actors.CargoStation._
import com.global.snapshot.actors.CargoStations.{Start, Stop}

class CargoStations
  extends CargoActor {

  override def receive = {

    case Start =>
      val stockholm = context.actorOf(CargoStation.props, "stockholm")
      val copenhagen = context.actorOf(CargoStation.props, "copenhagen")
      val helsinki = context.actorOf(CargoStation.props, "helsinki")
      val oslo = context.actorOf(CargoStation.props, "oslo")

      stockholm ! Initialize(
        cargoCount = Config.cargoInitialCount,
        incomingChannels = Set(copenhagen, helsinki),
        outgoingChannels = Set(oslo)
      )
      copenhagen ! Initialize(
        cargoCount = Config.cargoInitialCount,
        incomingChannels = Set(oslo),
        outgoingChannels = Set(stockholm, helsinki)
      )
      helsinki ! Initialize(
        cargoCount = Config.cargoInitialCount,
        incomingChannels = Set(copenhagen),
        outgoingChannels = Set(stockholm, oslo)
      )
      oslo ! Initialize(
        cargoCount = Config.cargoInitialCount,
        incomingChannels = Set(stockholm, helsinki),
        outgoingChannels = Set(copenhagen)
      )

      stockholm ! StartScheduler
      copenhagen ! StartScheduler
      helsinki ! StartScheduler
      oslo ! StartScheduler

    case Stop =>
      context stop self

    case event =>
      super.receive(event)
  }
}

object CargoStations {
  def props: Props =
    Props(new CargoStations)

  sealed trait CargoStationsOperations
  case object Start extends CargoStationsOperations
  case object Stop extends CargoStationsOperations
}
