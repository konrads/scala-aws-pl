package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class Device(devId: String, uid: String, `type`: String, model: String)

object Device {
  implicit val jsonWrites = Json.writes[Device]
  implicit val jsonReads  = Json.reads[Device]

  implicit val redisFormatter = new ByteStringFormatter[Device] {
    def serialize(device: Device): ByteString =
      ByteString(Json.toJson(device).toString())

    def deserialize(bs: ByteString): Device =
      Json.parse(bs.utf8String).as[Device]
  }
}
