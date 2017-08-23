package com.global.snapshot

import com.typesafe.config.ConfigFactory

object Config {

  val config = ConfigFactory.load()

  val cargoInitialCount = config.getLong("cargo.initial.count")

  val cargoUnloadMin = config.getInt("cargo.unload.min")
  val cargoUnloadMax = config.getInt("cargo.unload.max")
}
