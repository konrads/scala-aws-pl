package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class Portfolio(uid: String, tickers: Seq[String])

object Portfolio {
  implicit val jsonWrites = Json.writes[Portfolio]
  implicit val jsonReads  = Json.reads[Portfolio]

  implicit val redisFormatter = new ByteStringFormatter[Portfolio] {
    def serialize(portfolio: Portfolio): ByteString =
      ByteString(Json.toJson(portfolio).toString())

    def deserialize(bs: ByteString): Portfolio =
      Json.parse(bs.utf8String).as[Portfolio]
  }
}
