package com.global.snapshot.actors

import akka.actor.Props
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.StartScheduling
import com.global.snapshot.actors.CargoStation.Connect
import com.global.snapshot.actors.CargoStations.{Start, Stop}

class CargoStations
  extends CargoActor {

  override def receive = {

    case Start =>
      val stockholm = context.actorOf(CargoStation.props(Config.cargoStationsCount), "stockholm")
      val copenhagen = context.actorOf(CargoStation.props(Config.cargoStationsCount), "copenhagen")
      val helsinki = context.actorOf(CargoStation.props(Config.cargoStationsCount), "helsinki")
      val oslo = context.actorOf(CargoStation.props(Config.cargoStationsCount), "oslo")

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

      stockholm ! StartScheduling
      copenhagen ! StartScheduling
      helsinki ! StartScheduling
      oslo ! StartScheduling

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
