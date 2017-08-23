package com.global.snapshot.actors

import akka.actor.Props
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.ScheduleUnload
import com.global.snapshot.actors.CargoStation.Initialize
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
        initialCargoCount = Config.cargoInitialCount,
        incomingChannels = Set(copenhagen, helsinki),
        outgoingChannels = Set(oslo)
      )
      copenhagen ! Initialize(
        initialCargoCount = Config.cargoInitialCount,
        incomingChannels = Set(oslo),
        outgoingChannels = Set(stockholm, helsinki)
      )
      helsinki ! Initialize(
        initialCargoCount = Config.cargoInitialCount,
        incomingChannels = Set(copenhagen),
        outgoingChannels = Set(stockholm, oslo)
      )
      oslo ! Initialize(
        initialCargoCount = Config.cargoInitialCount,
        incomingChannels = Set(stockholm, helsinki),
        outgoingChannels = Set(copenhagen)
      )

      stockholm ! ScheduleUnload
      copenhagen ! ScheduleUnload
      helsinki ! ScheduleUnload
      oslo ! ScheduleUnload

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
