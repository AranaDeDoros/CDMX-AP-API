package com.wifi.cdmx.domain

import pureconfig.*
import pureconfig.generic.derivation.default.*

case class DbConfig(
    url: String,
    user: String,
    pass: String,
    driver: String,
    csv: String               
) derives ConfigReader

case class AppConfig(
    db: DbConfig
) derives ConfigReader
