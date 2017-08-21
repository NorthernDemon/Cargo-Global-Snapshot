package com.global.snapshot.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.{ScheduleUnload, StartScheduling}
import com.global.snapshot.actors.CargoStation.Unload

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class CargoScheduler(cargoStation: ActorRef,
                     initialOutgoingChannels: Seq[ActorRef])
  extends Actor with ActorLogging {

  require(initialOutgoingChannels.nonEmpty)

  var cargoScheduler: Option[Cancellable] = _

  val random = new scala.util.Random
  var outgoingChannels: Seq[ActorRef] = _

  override def preStart() = {
    this.outgoingChannels = initialOutgoingChannels
  }

  override def receive = {

    case StartScheduling =>
      cargoScheduler match {
        case Some(scheduler) =>
          if (!scheduler.isCancelled) {
            scheduler.cancel()
          }
        case _ =>
          cargoScheduler = Some(context.system.scheduler.schedule(1 second, 10 seconds, self, ScheduleUnload))
      }

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
  case object StartScheduling extends CargoSchedulerOperations
  case object ScheduleUnload extends CargoSchedulerOperations
}
