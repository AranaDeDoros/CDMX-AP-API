package com.wifi.cdmx.domain

import io.circe.Codec
import io.circe.generic.semiauto.*

case class WiFiPoint(
    id: String,
    programa: String,
    alcaldia: String,
    latitud: Double,
    longitud: Double
)

object WiFiPoint {
  given Codec[WiFiPoint] = deriveCodec[WiFiPoint]
}

case class PaginatedResponse[A](
    items: List[A],
    total: Long,
    limit: Int,
    offset: Int
)

object PaginatedResponse {
  given [A: Codec]: Codec[PaginatedResponse[A]] = deriveCodec[PaginatedResponse[A]]
}

trait WiFiPointRepository[F[_]] {
  def getAll(limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]]
  def getById(id: String): F[Option[WiFiPoint]]
  def getByAlcaldia(alcaldia: String, limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]]
  def getByProximity(lat: Double, long: Double, limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]]
  def upsert(points: List[WiFiPoint]): F[Unit]
}
