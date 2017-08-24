package com.global.snapshot.actors

import akka.actor.{ActorRef, Props}
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.{StartScheduler, StopScheduler}
import com.global.snapshot.actors.CargoStation._
import com.global.snapshot.actors.CargoStations.{Join, Leave, Start, Stop}

class CargoStations
  extends CargoActor {

  var berlin: ActorRef = _
  var stockholm: ActorRef = _
  var copenhagen: ActorRef = _
  var helsinki: ActorRef = _
  var oslo: ActorRef = _

  override def receive = {

    case Start =>
      berlin = context.actorOf(CargoStation.props, "berlin")
      stockholm = context.actorOf(CargoStation.props, "stockholm")
      copenhagen = context.actorOf(CargoStation.props, "copenhagen")
      helsinki = context.actorOf(CargoStation.props, "helsinki")
      oslo = context.actorOf(CargoStation.props, "oslo")

      berlin ! Initialize(
        cargoCount = Config.cargoInitialCount * 2,
        incomingChannels = Set.empty[ActorRef],
        outgoingChannels = Set.empty[ActorRef]
      )
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

    case Join =>
      berlin ! Connect(stockholm, IncomingChannel)
      berlin ! Connect(stockholm, OutgoingChannel)
      berlin ! Connect(helsinki, IncomingChannel)
      berlin ! Connect(helsinki, OutgoingChannel)

      helsinki ! Connect(berlin, IncomingChannel)
      helsinki ! Connect(berlin, OutgoingChannel)

      stockholm ! Connect(berlin, IncomingChannel)
      stockholm ! Connect(berlin, OutgoingChannel)

      berlin ! StartScheduler

    case Leave =>
      berlin ! StopScheduler

      berlin ! Disconnect(stockholm, IncomingChannel)
      berlin ! Disconnect(stockholm, OutgoingChannel)
      berlin ! Disconnect(helsinki, IncomingChannel)
      berlin ! Disconnect(helsinki, OutgoingChannel)

      helsinki ! Disconnect(berlin, IncomingChannel)
      helsinki ! Disconnect(berlin, OutgoingChannel)

      stockholm ! Disconnect(berlin, IncomingChannel)
      stockholm ! Disconnect(berlin, OutgoingChannel)

    case Stop =>
      stockholm ! StopScheduler
      copenhagen ! StopScheduler
      helsinki ! StopScheduler
      oslo ! StopScheduler

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
  case object Join extends CargoStationsOperations
  case object Leave extends CargoStationsOperations
  case object Stop extends CargoStationsOperations
}
