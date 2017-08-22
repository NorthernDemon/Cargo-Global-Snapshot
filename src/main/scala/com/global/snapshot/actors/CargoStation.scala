package com.global.snapshot.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.global.snapshot.actors.CargoScheduler.StartScheduling
import com.global.snapshot.actors.CargoStation.{Connect, Load, Unload}

class CargoStation(initialCargoCount: Long)
  extends Actor with ActorLogging {

  require(initialCargoCount >= 0)

  val stationName = this.self.path.name

  var cargoSchedulerActor: ActorRef = _

  var cargoCount: Long = 0
  var incomingChannels: Set[ActorRef] = _
  var outgoingChannels: Set[ActorRef] = _

  override def preStart() = {
    this.cargoCount = initialCargoCount
    log.info(s"$stationName station with $cargoCount cargo is up and running")
  }

  override def postStop() = {
    log.info(s"$stationName station with $cargoCount cargo is shutting down...")
  }

  def receive = {

    case StartScheduling =>
      cargoSchedulerActor = context.actorOf(CargoScheduler.props(self, outgoingChannels), s"${stationName}Scheduler")
      cargoSchedulerActor ! StartScheduling

    case Connect(incomingChannels: Set[ActorRef], outgoingChannels: Set[ActorRef]) =>
      log.info(s"Connecting $stationName station to " +
        s"incoming channels $incomingChannels and outgoing channels $outgoingChannels")
      this.incomingChannels = incomingChannels
      this.outgoingChannels = outgoingChannels

    case Unload(outgoingCargo: Long, outgoingChannel: ActorRef) =>
      if (outgoingChannels.contains(outgoingChannel)) {
        if (cargoCount - outgoingCargo < 0) {
          log.warning(s"Cargo station $stationName cannot unload more" +
            s" than it has: cargoCount=$cargoCount, outgoingCargo=$outgoingCargo")
        } else {
          log.info(s"Unloading $outgoingCargo cargo from $stationName station to the $outgoingChannel")
          cargoCount -= outgoingCargo
          outgoingChannel ! Load(outgoingCargo, self)
        }
      } else {
        log.error(s"Station $stationName cannot unload the cargo to an unconnected $outgoingChannel")
      }

    case Load(incomingCargo: Long, incomingChannel: ActorRef) =>
      if (incomingChannels.contains(incomingChannel)) {
        log.info(s"Loading $incomingCargo cargo to $stationName station from the $incomingChannel")
        cargoCount += incomingCargo
      } else {
        log.error(s"Station $stationName cannot accept the cargo from an unconnected $incomingChannel")
      }

    case event =>
      log.error(s"Cargo station $stationName received an unknown event $event")
  }
}

object CargoStation {
  def props(initialCargoCount: Long): Props =
    Props(new CargoStation(initialCargoCount))

  sealed trait CargoStationOperations
  case class Connect(incomingChannels: Set[ActorRef], outgoingChannels: Set[ActorRef]) extends CargoStationOperations
  case class Unload(cargoCount: Long, outgoingChannel: ActorRef) extends CargoStationOperations
  case class Load(cargoCount: Long, incomingChannel: ActorRef) extends CargoStationOperations
}
