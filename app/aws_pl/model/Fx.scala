package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class Fx(sourceCurrency: String, targetCurrency: String, multiplier: Double)

object Fx {
  implicit val jsonWrites = Json.writes[Fx]
  implicit val jsonReads  = Json.reads[Fx]

  implicit val redisFormatter = new ByteStringFormatter[Fx] {
    def serialize(fx: Fx): ByteString =
      ByteString(Json.toJson(fx).toString())

    def deserialize(bs: ByteString): Fx =
      Json.parse(bs.utf8String).as[Fx]
  }
}
