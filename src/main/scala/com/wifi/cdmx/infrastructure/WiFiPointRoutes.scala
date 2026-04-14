package com.wifi.cdmx.infrastructure

import cats.effect.Async
import cats.implicits.*
import com.wifi.cdmx.application.WiFiPointService
import com.wifi.cdmx.domain.{PaginatedResponse, WiFiPoint}
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

class WiFiPointRoutes[F[_]: Async](service: WiFiPointService[F]) {

  // definitions
  private val baseEndpoint = endpoint.in("wifi")

  val getAllEndpoint = baseEndpoint
    .get
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .out(jsonBody[PaginatedResponse[WiFiPoint]])
    .summary("Get a paginated list of Access Points and the number of them")

  val getByIdEndpoint = baseEndpoint
    .get
    .in(path[String]("id"))
    .out(jsonBody[WiFiPoint])
    .errorOut(oneOf(oneOfVariant(StatusCode.NotFound, stringBody)))
    .summary("Lookup Access Points by id")

  val getByAlcaldiaEndpoint = baseEndpoint
    .get
    .in("alcaldia" / path[String]("alcaldia"))
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .out(jsonBody[PaginatedResponse[WiFiPoint]])
    .summary("Get a paginated list and total AP given an alcaldia")

  val getByProximityEndpoint = baseEndpoint
    .get
    .in("proximity")
    .in(query[Double]("latitud"))
    .in(query[Double]("longitud"))
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .out(jsonBody[PaginatedResponse[WiFiPoint]])
    .summary("Get a paginated list and total of WiFi points ordered by proximity")

  // logic
  val getAllRoutes = getAllEndpoint.serverLogic { (limit, offset) =>
    service.getAll(limit.getOrElse(10), offset.getOrElse(0)).map(_.asRight[Unit])
  }

  val getByIdRoutes = getByIdEndpoint.serverLogic { id =>
    service.getById(id).map {
      case Some(point) => point.asRight[String]
      case None => s"WiFi Point with ID $id not found".asLeft[WiFiPoint]
    }
  }

  val getByAlcaldiaRoutes = getByAlcaldiaEndpoint.serverLogic { (alcaldia, limit, offset) =>
    service.getByAlcaldia(alcaldia, limit.getOrElse(10), offset.getOrElse(0)).map(_.asRight[Unit])
  }

  val getByProximityRoutes = getByProximityEndpoint.serverLogic { (latitud, longitud, limit, offset) =>
    service.getByProximity(latitud, longitud, limit.getOrElse(10), offset.getOrElse(0)).map(_.asRight[Unit])
  }


  val allServerEndpoints = List(
    getAllRoutes,
    getByAlcaldiaRoutes,
    getByProximityRoutes,
    getByIdRoutes
  )

  val swaggerEndpoints = SwaggerInterpreter()
    .fromServerEndpoints[F](allServerEndpoints, "WiFi CDMX API", "0.1.0")

  val routes: HttpRoutes[F] = Http4sServerInterpreter[F]()
    .toRoutes(allServerEndpoints ++ swaggerEndpoints)
}
