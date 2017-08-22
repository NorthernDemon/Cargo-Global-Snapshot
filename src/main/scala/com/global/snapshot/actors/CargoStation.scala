package com.global.snapshot.actors

import akka.actor.{ActorRef, Props}
import com.global.snapshot.actors.CargoScheduler.StartScheduling
import com.global.snapshot.actors.CargoStation.{Connect, Load, Unload}

class CargoStation(initialCargoCount: Long)
  extends CargoActor {

  require(initialCargoCount >= 0)

  val stationName = name(this.self)

  var cargoSchedulerActor: Option[ActorRef] = None

  var cargoCount: Long = 0
  var incomingChannels: Set[ActorRef] = _
  var outgoingChannels: Set[ActorRef] = _

  override def preStart() = {
    this.cargoCount = initialCargoCount
    log.info(s"Ramping up $stationName with $cargoCount cargo")
  }

  override def postStop() = {
    log.info(s"Shutting down $stationName with $cargoCount cargo left")
  }

  override def receive = {

    case StartScheduling =>
      cargoSchedulerActor match {
        case Some(scheduler) =>
          scheduler ! StartScheduling
        case None =>
          val scheduler = context.actorOf(CargoScheduler.props(self, outgoingChannels), s"${stationName}Scheduler")
          cargoSchedulerActor = Some(scheduler)
          scheduler ! StartScheduling
      }

    case Connect(incomingChannels: Set[ActorRef], outgoingChannels: Set[ActorRef]) =>
      this.incomingChannels = incomingChannels
      this.outgoingChannels = outgoingChannels
      log.info(s"Connecting $stationName to " +
        s"incoming channels ${incomingChannels.map(name)} and " +
        s"outgoing channels ${outgoingChannels.map(name)}")

    case Unload(outgoingCargo: Long, outgoingChannel: ActorRef) =>
      if (outgoingChannels.contains(outgoingChannel)) {
        if (cargoCount - outgoingCargo < 0) {
          log.warning(s"Cannot unload $outgoingCargo cargo because $stationName only has $cargoCount cargo left")
        } else {
          log.info(s"Unloading $outgoingCargo cargo from $stationName to ${name(outgoingChannel)}")
          cargoCount -= outgoingCargo
          outgoingChannel ! Load(outgoingCargo, self)
        }
      } else {
        log.error(s"Cannot unload cargo from $stationName to an unconnected ${name(outgoingChannel)}")
      }

    case Load(incomingCargo: Long, incomingChannel: ActorRef) =>
      if (incomingChannels.contains(incomingChannel)) {
        log.info(s"Loading $incomingCargo cargo from ${name(incomingChannel)} to $stationName")
        cargoCount += incomingCargo
      } else {
        log.error(s"Cannot accept cargo from an unconnected ${name(incomingChannel)} to $stationName")
      }

    case event =>
      super.receive(event)
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
