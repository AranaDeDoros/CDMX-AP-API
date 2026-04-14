package com.wifi.cdmx.application

import cats.effect.IO
import com.wifi.cdmx.domain.{WiFiPoint, WiFiPointRepository}

class IngestService(repo: WiFiPointRepository[IO]) {

  /**
   * Updates the current database with info from a csv
   * @return
   */
  def ingest(path: String): IO[Unit] = {
    val csvPath = if (path.isEmpty) "00-2025-wifi_cdmx.csv" else path
    IO.blocking(readCsv(csvPath))
      .flatMap(aps => repo.upsert(aps.toList))
  }

  /**
   * loads a CSV from a file
   * this method does not scale well
   * and a more robust option should be preferred
   * maybe something like fs2 if the file is too large
   * @param path csv filepath
   * @return
   */
  private def readCsv(path : String = "00-2025-wifi_cdmx.csv"): Iterator[WiFiPoint] = {
    val stream = Option(getClass.getClassLoader.getResourceAsStream(path))
      .getOrElse(throw new RuntimeException(s"File not found: $path"))

    val source = scala.io.Source.fromInputStream(stream)

    val iter = source
      .getLines()
      .drop(1)
      .filter(_.nonEmpty)
      .flatMap { line =>
        val cols = line.split(",").map(_.trim)

        try {
          Some(WiFiPoint(
            cols(0),
            cols(1),
            cols(4),
            cols(2).toDouble,
            cols(3).toDouble
          ))
        } catch {
          case _: Exception => None
        }
      }

    new Iterator[WiFiPoint] {
      def hasNext: Boolean = {
        val hn = iter.hasNext
        if (!hn) source.close()
        hn
      }
      def next(): WiFiPoint = iter.next()
    }
  }

}
