package com.wifi.cdmx.application

import cats.effect.IO
import com.wifi.cdmx.domain.{WiFiPoint, WiFiPointRepository}

class IngestService(repo: WiFiPointRepository[IO]) {

  /**
   * Updates the current database with info from a csv
   * @return
   */
  def ingest(path: String): IO[Unit] =
    IO.blocking(readCsv(path))
      .flatMap(aps => repo.upsert(aps.toList))

  /**
   * loads a CSV from a file
   * this method does not scale well
   * and a more robust option should be preferred
   * maybe something like fs2 if the file is too large
   * @param path
   * @return
   */
  private def readCsv(path : String ): Iterator[WiFiPoint] = {
    val stream = getClass.getClassLoader
      .getResourceAsStream("00-2025-wifi_cdmx.csv")

    val source = scala.io.Source.fromInputStream(stream)

    source
      .getLines()
      .drop(1)
      .map(_.split("\t"))
      .map(cols => WiFiPoint(
        cols(0),
        cols(1),
        cols(4),
        cols(2).toDouble,
        cols(3).toDouble
      ))
  }

}
