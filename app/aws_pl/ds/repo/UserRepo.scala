package aws_pl.ds.repo

import aws_pl.ds.aws.S3
import aws_pl.model.User
import cats.data.OptionT
import redis.RedisClient

import scala.concurrent.{Future, ExecutionContext}

class UserRepo(s3: S3, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getUser(uid: String): Future[Option[User]] = {
    val s3Key = "user:$uid"
    for {
      user <- OptionT(s3.getJson(uid))
    } yield {
      user
    }
    ???
  }

}
