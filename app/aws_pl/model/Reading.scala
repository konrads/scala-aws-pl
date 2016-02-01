package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class Reading(devId: String, timestamp: Int, `type`: String, reading: Double)

object Reading {
  implicit val jsonWrites = Json.writes[Reading]
  implicit val jsonReads  = Json.reads[Reading]

  implicit val redisFormatter = new ByteStringFormatter[Reading] {
    def serialize(reading: Reading): ByteString =
      ByteString(Json.toJson(reading).toString())

    def deserialize(bs: ByteString): Reading =
      Json.parse(bs.utf8String).as[Reading]
  }
}
