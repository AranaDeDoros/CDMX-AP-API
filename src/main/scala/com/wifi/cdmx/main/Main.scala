package com.wifi.cdmx.main

import cats.effect.*
import com.comcast.ip4s.*
import com.wifi.cdmx.application.{IngestService, WiFiPointService}
import com.wifi.cdmx.domain.AppConfig
import com.wifi.cdmx.infrastructure.{DoobieWiFiPointRepository, WiFiPointRoutes}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import pureconfig.ConfigSource
import scala.concurrent.duration.*

object Main extends IOApp.Simple {

  override def run: IO[Unit] = {
    val config = ConfigSource.default.loadOrThrow[AppConfig]

    //environment setup
    val resources = for {
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        config.db.driver,
        config.db.url,
        config.db.user,
        config.db.pass,
        ec
      )
    } yield xa

    //we handle resources safely
    resources.use { xa =>
      val repo = new DoobieWiFiPointRepository[IO](xa)
      val service = new WiFiPointService[IO](repo)
      val wifiRoutes = new WiFiPointRoutes[IO](service)
      val httpApp = Logger.httpApp(true, true)(wifiRoutes.routes.orNotFound)

      //run ingestion (csv load)
      //on start up and then every 8 hours
      val ingestService = new IngestService(repo)
      val ingestionProcess =
        (IO.sleep(10.seconds) *>
          // had no time to finish this properly
          // the idea is to ingest/load data from the csv
          // on startup, the csv may need some cleaning up tho
          IO.println("STARTING INGEST") *>
          ingestService.ingest(config.db.csv)
            .handleErrorWith(e => IO.println(s"ERROR: ${e.getMessage}")) *>
          IO.println("ENDING INGEST") *>
          IO.sleep(8.hours)
          ).foreverM

      for {
        fiber  <- ingestionProcess.start
        _ <- EmberServerBuilder.default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(httpApp)
          .build
          .use(_ => IO.never)
          .guarantee(fiber.cancel)
      } yield ()
    }
  }
}
