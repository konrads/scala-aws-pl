package aws_pl.model

import akka.util.ByteString
import play.api.libs.json.Json
import redis.ByteStringFormatter

case class User (uid: String, `type`: String, password: String) {
  def asPublic = UserPublic(this.uid, this.`type`)
}

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

case class UserPublic (uid: String, `type`: String)

object UserPublic {
  implicit val jsonWrites = Json.writes[UserPublic]
  implicit val jsonReads  = Json.reads[UserPublic]
}
