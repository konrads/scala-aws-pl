package aws_pl.ds.repo

import java.util.UUID

import aws_pl.aws.DynamoDB
import aws_pl.model.User
import aws_pl.util.Cats
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

  def getCachedUserId(token: String) =
    redis.get[String](s"$authTokenPrefix$token")

  def cacheUserId(userId: String): Future[Option[String]] = {
    val token = UUID.randomUUID.toString
    val res = for {
      res <- Cats.liftF(redis.set(s"$authTokenPrefix$token", userId))
      res2 <- OptionT.pure(token)
    } yield res2
    res.value
  }

  private val authTokenPrefix = "authtoken:"
}
