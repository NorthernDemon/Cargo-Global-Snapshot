package com.global.snapshot.actors

import akka.actor.{ActorRef, Cancellable, Props}
import akka.pattern.ask
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.{ScheduleUnload, UnscheduleUnload}
import com.global.snapshot.actors.CargoStation.{GetOutgoingChannels, Unload}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class CargoScheduler
  extends CargoActor {

  val cargoStation = context.parent
  var cargoScheduler: Cancellable = _

  val random = new scala.util.Random

  override def receive = {

    case ScheduleUnload =>
      cargoScheduler = context.system.scheduler.schedule(1 second, 5 seconds) {
        (cargoStation ? GetOutgoingChannels) (1 second).mapTo[Set[ActorRef]].flatMap { outgoingChannels =>
          cargoStation ! Unload(getRandomCargo, getRandomOutgoingChannel(outgoingChannels.toSeq))
          Future.successful()
        }
      }

    case UnscheduleUnload =>
      if (!cargoScheduler.isCancelled) cargoScheduler.cancel()

    case event =>
      super.receive(event)
  }

  private def getRandomCargo: Long =
    getRandom(Config.cargoUnloadMin, Config.cargoUnloadMax)

  private def getRandomOutgoingChannel(outgoingChannels: Seq[ActorRef]): ActorRef =
    outgoingChannels(getRandom(0, outgoingChannels.size))

  private def getRandom(lowInclusive: Int, highInclusive: Int): Int =
    lowInclusive + random.nextInt(highInclusive)
}

object CargoScheduler {
  def props: Props =
    Props(new CargoScheduler)

  sealed trait CargoSchedulerOperations
  case object ScheduleUnload extends CargoSchedulerOperations
  case object UnscheduleUnload extends CargoSchedulerOperations
}
