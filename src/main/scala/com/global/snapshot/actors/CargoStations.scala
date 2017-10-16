package com.global.snapshot.actors

import akka.actor.{ActorRef, Props}
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.{StartScheduler, StopScheduler}
import com.global.snapshot.actors.CargoStation._
import com.global.snapshot.actors.CargoStations._

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
      berlin ! Connect(Set(stockholm, copenhagen, helsinki, oslo), IncomingChannel)
      berlin ! Connect(Set(stockholm, copenhagen, helsinki, oslo), OutgoingChannel)

      stockholm ! Connect(Set(berlin), IncomingChannel)
      stockholm ! Connect(Set(berlin), OutgoingChannel)

      copenhagen ! Connect(Set(berlin), IncomingChannel)
      copenhagen ! Connect(Set(berlin), OutgoingChannel)

      helsinki ! Connect(Set(berlin), IncomingChannel)
      helsinki ! Connect(Set(berlin), OutgoingChannel)

      oslo ! Connect(Set(berlin), IncomingChannel)
      oslo ! Connect(Set(berlin), OutgoingChannel)

      berlin ! StartScheduler

    case Leave =>
      berlin ! Disconnect(Set(stockholm, copenhagen, helsinki, oslo), IncomingChannel)
      berlin ! Disconnect(Set(stockholm, copenhagen, helsinki, oslo), OutgoingChannel)

      stockholm ! Disconnect(Set(berlin), IncomingChannel)
      stockholm ! Disconnect(Set(berlin), OutgoingChannel)

      copenhagen ! Disconnect(Set(berlin), IncomingChannel)
      copenhagen ! Disconnect(Set(berlin), OutgoingChannel)

      helsinki ! Disconnect(Set(berlin), IncomingChannel)
      helsinki ! Disconnect(Set(berlin), OutgoingChannel)

      oslo ! Disconnect(Set(berlin), IncomingChannel)
      oslo ! Disconnect(Set(berlin), OutgoingChannel)

      berlin ! StopScheduler

    case Stop =>
      berlin ! StopScheduler
      stockholm ! StopScheduler
      copenhagen ! StopScheduler
      helsinki ! StopScheduler
      oslo ! StopScheduler

      context stop self

    case StartMarker =>
      (new scala.util.Random).nextInt(4) match {
        case 0 =>
          log.info(s"Marker sent out from $stockholm")
          stockholm ! InitMarker
        case 1 =>
          log.info(s"Marker sent out from $copenhagen")
          copenhagen ! InitMarker
        case 2 =>
          log.info(s"Marker sent out from $helsinki")
          helsinki ! InitMarker
        case 3 =>
          log.info(s"Marker sent out from $oslo")
          oslo ! InitMarker
      }

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

  case object StartMarker extends CargoStationsOperations
}
