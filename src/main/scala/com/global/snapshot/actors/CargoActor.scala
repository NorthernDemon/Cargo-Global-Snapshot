package com.global.snapshot.actors

import akka.actor.{Actor, ActorLogging, ActorRef}

abstract class CargoActor
  extends Actor with ActorLogging {

  override def receive = {

    case event =>
      log.error(s"${name(self)} received an unknown event $event")
  }

  protected def name(actor: ActorRef): String =
    actor.path.name.toUpperCase
}
