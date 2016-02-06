package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class Spot(ticker: String, timestamp: Long, currency: String, price: Double)

object Spot {
  implicit val jsonWrites = Json.writes[Spot]
  implicit val jsonReads  = Json.reads[Spot]

  implicit val redisFormatter = new ByteStringFormatter[Spot] {
    def serialize(spot: Spot): ByteString =
      ByteString(Json.toJson(spot).toString())

    def deserialize(bs: ByteString): Spot =
      Json.parse(bs.utf8String).as[Spot]
  }
}
