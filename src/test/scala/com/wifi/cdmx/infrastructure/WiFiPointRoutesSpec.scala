package com.wifi.cdmx.infrastructure

import cats.effect.{IO, Ref}
import com.wifi.cdmx.application.WiFiPointService
import com.wifi.cdmx.domain.{PaginatedResponse, WiFiPoint, WiFiPointRepository}
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityDecoder.*

class WiFiPointRoutesSpec extends CatsEffectSuite {

  def createMockRepo(initialData: Map[String, WiFiPoint] = Map.empty): IO[WiFiPointRepository[IO]] = {
    Ref.of[IO, Map[String, WiFiPoint]](initialData).map { state =>
      new WiFiPointRepository[IO] {
        override def getAll(limit: Int, offset: Int): IO[PaginatedResponse[WiFiPoint]] =
          state.get.map { data =>
            val items = data.values.toList.sortBy(_.id).slice(offset, offset + limit)
            PaginatedResponse(items, data.size, limit, offset)
          }

        override def getById(id: String): IO[Option[WiFiPoint]] =
          state.get.map(_.get(id))

        override def getByAlcaldia(alcaldia: String, limit: Int, offset: Int): IO[PaginatedResponse[WiFiPoint]] =
          state.get.map { data =>
            val filtered = data.values.filter(_.alcaldia == alcaldia).toList.sortBy(_.id)
            val items = filtered.slice(offset, offset + limit)
            PaginatedResponse(items, filtered.size, limit, offset)
          }

        override def getByProximity(lat: Double, long: Double, limit: Int, offset: Int): IO[PaginatedResponse[WiFiPoint]] =
          state.get.map { data =>
            // Simple Euclidean distance for mock
            val items = data.values.toList
              .sortBy(p => math.sqrt(math.pow(p.latitud - lat, 2) + math.pow(p.longitud - long, 2)))
              .slice(offset, offset + limit)
            PaginatedResponse(items, data.size, limit, offset)
          }

        override def upsert(points: List[WiFiPoint]): IO[Unit] =
          state.update(_ ++ points.map(p => p.id -> p).toMap)
      }
    }
  }

  val samplePoint = WiFiPoint("SomeId1", "Prog", "Alcaldia1", 19.0, -99.0)
  val samplePoint2 = WiFiPoint("SomeId2", "Prog2", "Alcaldia2", 20.0, -100.0)

  test("GET /wifi returns 200 and paginated list from dynamic state") {
    for {
      repo <- createMockRepo(Map("1" -> samplePoint, "2" -> samplePoint2))
      service = new WiFiPointService[IO](repo)
      routes = new WiFiPointRoutes[IO](service).routes.orNotFound
      request = Request[IO](Method.GET, uri"/wifi?limit=1")
      response <- routes.run(request)
      _ = assertEquals(response.status, Status.Ok)
      body <- response.as[PaginatedResponse[WiFiPoint]]
      _ = assertEquals(body.items.size, 1)
      _ = assertEquals(body.total, 2L)
      _ = assertEquals(body.items.head.id, "SomeId1")
    } yield ()
  }

  test("GET /wifi/1 returns 200 and the point from dynamic state") {
    for {
      repo <- createMockRepo(Map("SomeId1" -> samplePoint))
      service = new WiFiPointService[IO](repo)
      routes = new WiFiPointRoutes[IO](service).routes.orNotFound
      request = Request[IO](Method.GET, uri"/wifi/SomeId1")
      response <- routes.run(request)
      _ = assertEquals(response.status, Status.Ok)
      point <- response.as[WiFiPoint]
      _ = assertEquals(point.id, "SomeId1")
    } yield ()
  }

  test("GET /wifi/999 returns 404 when not in state") {
    for {
      repo <- createMockRepo(Map("1" -> samplePoint))
      service = new WiFiPointService[IO](repo)
      routes = new WiFiPointRoutes[IO](service).routes.orNotFound
      request = Request[IO](Method.GET, uri"/wifi/999")
      response <- routes.run(request)
      _ = assertEquals(response.status, Status.NotFound)
    } yield ()
  }

  test("GET /wifi/alcaldia/{alcaldia} filters correctly from dynamic state") {
    for {
      repo <- createMockRepo(Map("1" -> samplePoint, "2" -> samplePoint2))
      service = new WiFiPointService[IO](repo)
      routes = new WiFiPointRoutes[IO](service).routes.orNotFound
      request = Request[IO](Method.GET, uri"/wifi/alcaldia/Alcaldia2")
      response <- routes.run(request)
      _ = assertEquals(response.status, Status.Ok)
      body <- response.as[PaginatedResponse[WiFiPoint]]
      _ = assertEquals(body.items.size, 1)
      _ = assertEquals(body.items.head.alcaldia, "Alcaldia2")
      _ = assertEquals(body.total, 1L)
    } yield ()
  }


}
