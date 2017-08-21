package com.global.snapshot

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.global.snapshot.actos.CargoStation
import com.global.snapshot.actos.CargoStation.Load
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class MainSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("global-snapshot"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "cargo actor" should "pass on a load message" in {
    val testProbe = TestProbe()
    val mikkeli = system.actorOf(CargoStation.props("Mikkeli", 0))
    val pushkin = system.actorOf(CargoStation.props("Pushkin", 1))
    mikkeli ! Load(1, pushkin)
    testProbe.expectMsg(1 second, Load(1, pushkin))
  }
}
