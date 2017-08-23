package com.global.snapshot.actors

import akka.actor.{ActorRef, Cancellable, Props}
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.{ScheduleRandomUnloader, ScheduleUnload}
import com.global.snapshot.actors.CargoStation.Unload

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class CargoScheduler(cargoStation: ActorRef,
                     initialOutgoingChannels: Seq[ActorRef])
  extends CargoActor {

  require(initialOutgoingChannels.nonEmpty)

  var cargoScheduler: Option[Cancellable] = None

  val random = new scala.util.Random
  var outgoingChannels: Seq[ActorRef] = Seq.empty[ActorRef]

  override def preStart() = {
    this.outgoingChannels = initialOutgoingChannels
  }

  override def receive = {

    case ScheduleUnload =>
      cargoScheduler match {
        case Some(scheduler) => if (!scheduler.isCancelled) scheduler.cancel()
        case None =>
      }
      cargoScheduler = Some(
        context.system.scheduler.schedule(
          initialDelay = 1 second,
          interval = 10 seconds,
          receiver = self,
          message = ScheduleRandomUnloader
        )
      )

    case ScheduleRandomUnloader =>
      cargoStation ! Unload(getRandomCargo, getRandomOutgoingChannel)

    case event =>
      super.receive(event)
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
  case object ScheduleRandomUnloader extends CargoSchedulerOperations
}
