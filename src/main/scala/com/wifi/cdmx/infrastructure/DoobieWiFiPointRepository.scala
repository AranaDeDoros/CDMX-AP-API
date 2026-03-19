package com.wifi.cdmx.infrastructure

import cats.effect.MonadCancelThrow
import cats.implicits.*
import com.wifi.cdmx.domain.{PaginatedResponse, WiFiPoint, WiFiPointRepository}
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class DoobieWiFiPointRepository[F[_]: MonadCancelThrow](xa: Transactor[F]) extends WiFiPointRepository[F] {

  private object Queries {
    def getAll(limit: Int, offset: Int): Query0[WiFiPoint] =
      sql"""
        SELECT id, programa, alcaldia, latitud, longitud
        FROM wifi.access_points
        ORDER BY id
        LIMIT $limit OFFSET $offset
      """.query[WiFiPoint]

    def countAll: Query0[Long] =
      sql"SELECT count(*) FROM wifi.access_points".query[Long]

    def getById(id: String): Query0[WiFiPoint] =
      sql"""
        SELECT id, programa, alcaldia, latitud, longitud
        FROM wifi.access_points
        WHERE id = $id
      """.query[WiFiPoint]

    def getByAlcaldia(alcaldia: String, limit: Int, offset: Int): Query0[WiFiPoint] =
      sql"""
        SELECT id, programa, alcaldia, latitud, longitud
        FROM wifi.access_points
        WHERE alcaldia = $alcaldia
        ORDER BY id
        LIMIT $limit OFFSET $offset
      """.query[WiFiPoint]

    def countByAlcaldia(alcaldia: String): Query0[Long] =
      sql"SELECT count(*) FROM wifi.access_points WHERE alcaldia = $alcaldia".query[Long]

    def getByProximity(lat: Double, long: Double, limit: Int, offset: Int): Query0[WiFiPoint] =
      sql"""
        SELECT id, programa, alcaldia, latitud, longitud
        FROM wifi.access_points
        ORDER BY geom <-> ST_SetSRID(ST_MakePoint($long, $lat), 4326)::geography
        LIMIT $limit OFFSET $offset
      """.query[WiFiPoint]
  }

  override def getAll(limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]] =
    for {
      items <- Queries.getAll(limit, offset).to[List].transact(xa)
      total <- Queries.countAll.unique.transact(xa)
    } yield PaginatedResponse(items, total, limit, offset)

  override def getById(id: String): F[Option[WiFiPoint]] =
    Queries.getById(id).option.transact(xa)

  override def getByAlcaldia(alcaldia: String, limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]] =
    for {
      items <- Queries.getByAlcaldia(alcaldia, limit, offset).to[List].transact(xa)
      total <- Queries.countByAlcaldia(alcaldia).unique.transact(xa)
    } yield PaginatedResponse(items, total, limit, offset)

  override def getByProximity(lat: Double, long: Double, limit: Int, offset: Int): F[PaginatedResponse[WiFiPoint]] =
    for {
      items <- Queries.getByProximity(lat, long, limit, offset).to[List].transact(xa)
      total <- Queries.countAll.unique.transact(xa)
    } yield PaginatedResponse(items, total, limit, offset)

  override def upsert(points: List[WiFiPoint]): F[Unit] = {
    val sql = """
        INSERT INTO wifi.access_points (id, programa, alcaldia, latitud, longitud, geom)
        VALUES (?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326))
        ON CONFLICT (id) DO UPDATE SET
          programa = EXCLUDED.programa,
          alcaldia = EXCLUDED.alcaldia,
          latitud = EXCLUDED.latitud,
          longitud = EXCLUDED.longitud,
          geom = EXCLUDED.geom
      """
    Update[(String, String, String, Double, Double, Double, Double)](sql)
      .updateMany(points.map(p => (p.id, p.programa, p.alcaldia, p.latitud, p.longitud, p.longitud, p.latitud)))
      .void
      .transact(xa)
  }
}
