package com.global.snapshot.actors

import akka.actor.{ActorRef, Cancellable, Props}
import akka.pattern.ask
import com.global.snapshot.Config
import com.global.snapshot.actors.CargoScheduler.{StartScheduler, StopScheduler}
import com.global.snapshot.actors.CargoStation.{GetOutgoingChannels, Unload}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class CargoScheduler
  extends CargoActor {

  val parent = context.parent
  val random = new scala.util.Random

  var cancellable: Option[Cancellable] = None

  override def receive = {

    case StartScheduler =>
      cancellable match {
        case Some(_) =>
          log.warning(s"Scheduler for $name is already started")
        case None =>
          cancellable = Some(context.system.scheduler.schedule(1 second, 5 seconds) {
            (parent ? GetOutgoingChannels) (1 second).mapTo[Set[ActorRef]].map { outgoingChannels =>
              parent ! Unload(getRandomCargo, getRandomOutgoingChannel(outgoingChannels.toSeq))
            }
          })
      }

    case StopScheduler =>
      cancellable match {
        case Some(scheduler) =>
          scheduler.cancel()
          cancellable = None
        case None =>
          log.warning(s"Scheduler for $name is not yet started")
      }

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
  case object StartScheduler extends CargoSchedulerOperations
  case object StopScheduler extends CargoSchedulerOperations
}
