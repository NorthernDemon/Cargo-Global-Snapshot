package com.global.snapshot.actors

import akka.actor.{ActorRef, Props}
import com.global.snapshot.actors.CargoScheduler.ScheduleUnload
import com.global.snapshot.actors.CargoStation.{Initialize, Load, Unload}

class CargoStation
  extends CargoActor {

  var cargoSchedulerActor: Option[ActorRef] = None

  var cargoCount: Long = 0
  var incomingChannels: Set[ActorRef] = Set.empty[ActorRef]
  var outgoingChannels: Set[ActorRef] = Set.empty[ActorRef]

  override def postStop() = {
    log.info(s"Shutting down $name with $cargoCount cargo left")
  }

  override def receive = {

    case ScheduleUnload =>
      cargoSchedulerActor match {
        case Some(scheduler) =>
          scheduler ! ScheduleUnload
        case None =>
          val scheduler = context.actorOf(CargoScheduler.props(self, outgoingChannels), s"${name}Scheduler")
          cargoSchedulerActor = Some(scheduler)
          scheduler ! ScheduleUnload
      }

    case Initialize(initialCargoCount: Long, incomingChannels: Set[ActorRef], outgoingChannels: Set[ActorRef]) =>
      this.cargoCount = initialCargoCount
      this.incomingChannels = incomingChannels
      this.outgoingChannels = outgoingChannels
      log.info(s"Initializing $name with " +
        s"cargoCount=$cargoCount, " +
        s"incomingChannels=${incomingChannels.map(getName)}, " +
        s"outgoingChannels=${outgoingChannels.map(getName)}")

    case Unload(outgoingCargo: Long, outgoingChannel: ActorRef) =>
      if (outgoingChannels.contains(outgoingChannel)) {
        if (cargoCount - outgoingCargo < 0) {
          log.warning(s"Cannot unload $outgoingCargo cargo because $name only has $cargoCount cargo left")
        } else {
          log.info(s"Unloading $outgoingCargo cargo from $name to ${getName(outgoingChannel)}")
          cargoCount -= outgoingCargo
          outgoingChannel ! Load(outgoingCargo, self)
        }
      } else {
        log.error(s"Cannot unload cargo from $name to an unconnected ${getName(outgoingChannel)}")
      }

    case Load(incomingCargo: Long, incomingChannel: ActorRef) =>
      if (incomingChannels.contains(incomingChannel)) {
        log.info(s"Loading $incomingCargo cargo from ${getName(incomingChannel)} to $name")
        cargoCount += incomingCargo
      } else {
        log.error(s"Cannot accept cargo from an unconnected ${getName(incomingChannel)} to $name")
      }

    case event =>
      super.receive(event)
  }
}

object CargoStation {
  def props: Props =
    Props(new CargoStation)

  sealed trait CargoStationOperations
  case class Initialize(initialCargoCount: Long,
                        incomingChannels: Set[ActorRef],
                        outgoingChannels: Set[ActorRef]) extends CargoStationOperations
  case class Unload(cargoCount: Long,
                    outgoingChannel: ActorRef) extends CargoStationOperations
  case class Load(cargoCount: Long,
                  incomingChannel: ActorRef) extends CargoStationOperations
}
