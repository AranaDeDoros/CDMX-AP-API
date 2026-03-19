package com.wifi.cdmx.infrastructure

import cats.effect.IO
import com.wifi.cdmx.application.WiFiPointService
import com.wifi.cdmx.domain.{PaginatedResponse, WiFiPoint, WiFiPointRepository}
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityDecoder.*

class WiFiPointRoutesSpec extends CatsEffectSuite {

  val mockRepo = new WiFiPointRepository[IO] {
    val samplePoint = WiFiPoint("1", "Prog", "Alcaldia", 19.0, -99.0)

    override def getAll(limit: Int, offset: Int): IO[PaginatedResponse[WiFiPoint]] =
      IO.pure(PaginatedResponse(List(samplePoint), 1, limit, offset))

    override def getById(id: String): IO[Option[WiFiPoint]] =
      if (id == "1") IO.pure(Some(samplePoint)) else IO.pure(None)

    override def getByAlcaldia(alcaldia: String, limit: Int, offset: Int): IO[PaginatedResponse[WiFiPoint]] =
      IO.pure(PaginatedResponse(List(samplePoint), 1, limit, offset))

    override def getByProximity(lat: Double, long: Double, limit: Int, offset: Int): IO[PaginatedResponse[WiFiPoint]] =
      IO.pure(PaginatedResponse(List(samplePoint), 1, limit, offset))

    override def upsert(points: List[WiFiPoint]): IO[Unit] = ???
  }

  val service = new WiFiPointService[IO](mockRepo)
  val routes = new WiFiPointRoutes[IO](service).routes.orNotFound

  test("GET /wifi returns 200 and paginated list") {
    val request = Request[IO](Method.GET, uri"/wifi")
    routes.run(request).flatMap { response =>
      assertEquals(response.status, Status.Ok)
      response.as[PaginatedResponse[WiFiPoint]].map { body =>
        assertEquals(body.items.size, 1)
        assertEquals(body.items.head.id, "1")
      }
    }
  }

  test("GET /wifi/1 returns 200 and the point") {
    val request = Request[IO](Method.GET, uri"/wifi/1")
    routes.run(request).flatMap { response =>
      assertEquals(response.status, Status.Ok)
      response.as[WiFiPoint].map { point =>
        assertEquals(point.id, "1")
      }
    }
  }

  test("GET /wifi/999 returns 404") {
    val request = Request[IO](Method.GET, uri"/wifi/999")
    routes.run(request).map { response =>
      assertEquals(response.status, Status.NotFound)
    }
  }

  test("GET /wifi/alcaldia/test returns 200") {
    val request = Request[IO](Method.GET, uri"/wifi/alcaldia/test")
    routes.run(request).map { response =>
      assertEquals(response.status, Status.Ok)
    }
  }

  test("GET /wifi/proximity?latitud=19&longitud=-99 returns 200") {
    val request = Request[IO](Method.GET, uri"/wifi/proximity?latitud=19&longitud=-99")
    routes.run(request).map { response =>
      assertEquals(response.status, Status.Ok)
    }
  }
}
