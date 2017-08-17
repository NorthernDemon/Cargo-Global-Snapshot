package com.global.snapshot

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.global.snapshot.CargoScheduler.ScheduleUnload
import com.global.snapshot.CargoStation.Unload

class CargoScheduler(cargoStation: ActorRef,
                     outgoingChannels: Seq[ActorRef])
  extends Actor with ActorLogging {

  require(outgoingChannels.nonEmpty)

  val random = new scala.util.Random

  override def receive = {

    case ScheduleUnload =>
      cargoStation ! Unload(getRandomCargo, getRandomOutgoingChannel)

    case event =>
      log.error(s"Cargo scheduler for $cargoStation received an unknown event $event")
  }

  private def getRandomCargo: Long =
    getRandom(Config.cargoUnloadMin, Config.cargoUnloadMax)

  private def getRandomOutgoingChannel: ActorRef =
    outgoingChannels(getRandom(0, outgoingChannels.size))

  private def getRandom(lowInclusive: Int, highInclusive: Int): Int =
    lowInclusive + random.nextInt(highInclusive)
}

object CargoScheduler {
  def props(cargoStation: ActorRef, outgoingChannels: Set[ActorRef]): Props =
    Props(new CargoScheduler(cargoStation, outgoingChannels.toSeq))

  sealed trait CargoSchedulerOperations
  case object ScheduleUnload extends CargoSchedulerOperations
}
