package aws_pl.ds.repo

import aws_pl.aws.S3
import aws_pl.model.Reading
import aws_pl.util.Cats
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class ReadingRepo(s3: S3, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getReadings(devId: String, fromTs: Int): Future[Seq[Reading]] = {
    val cachedKey = s"readings:last10:$devId"
    val readingIds = redis.smembers[String](cachedKey)
    val cached = for {
      readingIds <- Cats.liftF(redis.smembers[String](cachedKey))
      readings <- Cats.liftF(redis.mget[Reading](readingIds:_*))
    } yield {
      for { i <- readings if i.isDefined } yield i.get
    }
    cached.getOrElseF {
      // fetch from s3
      
      ???
    }
  }

}
