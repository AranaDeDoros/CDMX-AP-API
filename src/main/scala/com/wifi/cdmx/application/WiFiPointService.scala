package com.wifi.cdmx.application

import com.wifi.cdmx.domain.{PaginatedResponse, WiFiPoint, WiFiPointRepository}

class WiFiPointService[F[_]](repo: WiFiPointRepository[F]) {
  def getAll(limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]] =
    repo.getAll(limit, offset)

  def getById(id: String): F[Option[WiFiPoint]] =
    repo.getById(id)

  def getByAlcaldia(alcaldia: String, limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]] =
    repo.getByAlcaldia(alcaldia, limit, offset)

  def getByProximity(lat: Double, long: Double, limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]] =
    repo.getByProximity(lat, long, limit, offset)
}
