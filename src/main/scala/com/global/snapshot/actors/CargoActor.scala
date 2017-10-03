package com.global.snapshot.actors

import akka.actor.{Actor, ActorLogging, ActorRef}

abstract class CargoActor
  extends Actor with ActorLogging {

  val name = getName(self)

  override def receive = {

    case event =>
      log.error(s"$name received an unknown event $event")
  }

  protected def getName(actor: ActorRef): String =
    actor.path.name.toUpperCase
}
