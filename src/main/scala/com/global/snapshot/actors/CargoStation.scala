package com.global.snapshot.actors

import akka.actor.{ActorRef, Props}
import com.global.snapshot.actors.CargoScheduler.{StartScheduler, StopScheduler}
import com.global.snapshot.actors.CargoStation._

class CargoStation
  extends CargoActor {

  var cargoCount = 0L
  var incomingChannels = Set.empty[ActorRef]
  var outgoingChannels = Set.empty[ActorRef]

  val scheduler = context.actorOf(CargoScheduler.props, "scheduler")

  override def postStop() = {
    log.info(s"Shutting down $name with $cargoCount cargo left")
  }

  override def receive = {

    case StartScheduler =>
      scheduler forward StartScheduler

    case StopScheduler =>
      scheduler forward StopScheduler

    case GetOutgoingChannels =>
      sender ! outgoingChannels

    case Initialize(cargoCount: Long, incomingChannels: Set[ActorRef], outgoingChannels: Set[ActorRef]) =>
      this.cargoCount = cargoCount
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

    case Connect(channels: Set[ActorRef], channelType: ChannelType) =>
      log.info(s"Connecting $channelType ${channels.map(getName)} to $name")
      channelType match {
        case IncomingChannel => incomingChannels ++= channels
        case OutgoingChannel => outgoingChannels ++= channels
      }

    case Disconnect(channels: Set[ActorRef], channelType: ChannelType) =>
      log.info(s"Disconnecting $channelType ${channels.map(getName)} from $name")
      channelType match {
        case IncomingChannel => incomingChannels --= channels
        case OutgoingChannel => outgoingChannels --= channels
      }

    case event =>
      super.receive(event)
  }
}

object CargoStation {
  def props: Props =
    Props(new CargoStation)

  sealed trait ChannelType
  case object IncomingChannel extends ChannelType
  case object OutgoingChannel extends ChannelType

  sealed trait CargoStationOperations

  case object GetOutgoingChannels
  case class Initialize(cargoCount: Long,
                        incomingChannels: Set[ActorRef],
                        outgoingChannels: Set[ActorRef]) extends CargoStationOperations

  case class Unload(cargoCount: Long,
                    outgoingChannel: ActorRef) extends CargoStationOperations
  case class Load(cargoCount: Long,
                  incomingChannel: ActorRef) extends CargoStationOperations

  case class Connect(channels: Set[ActorRef],
                     channelType: ChannelType) extends CargoStationOperations
  case class Disconnect(channels: Set[ActorRef],
                        channelType: ChannelType) extends CargoStationOperations
}
