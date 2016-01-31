package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class User (uid: String, role: String, password: String)

object User {
  implicit val jsonWrites = Json.writes[User]
  implicit val jsonReads  = Json.reads[User]

  implicit val redisFormatter = new ByteStringFormatter[User] {
    def serialize(user: User): ByteString =
      ByteString(Json.toJson(user).toString())

    def deserialize(bs: ByteString): User =
      Json.parse(bs.utf8String).as[User]
  }
}
