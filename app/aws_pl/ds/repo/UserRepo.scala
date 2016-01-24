package aws_pl.ds.repo

import aws_pl.aws.DynamoDB
import aws_pl.model.User
import cats.data.OptionT
import cats.std.future._
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class UserRepo(dynamoDB: DynamoDB, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getUser(uid: String): Future[Option[User]] = {
    val s3Key = s"user:$uid"
    val cached = redis.get[User](s3Key)
    OptionT(cached).orElseF(dynamoDB.user.getByUid(uid)).value
  }
}
