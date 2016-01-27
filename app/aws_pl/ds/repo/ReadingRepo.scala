package aws_pl.ds.repo

import aws_pl.aws.S3
import aws_pl.model.Reading
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class ReadingRepo(s3: S3, redis: RedisClient, bucket: String)(implicit ec: ExecutionContext) {
  def getReadings(devId: String): Future[Seq[Reading]] = {
    for {
      cached <- getCached(devId)
      cachedOrFromS3 <- {
        if (cached.nonEmpty)
          Future.successful(cached)
        else
          getFromS3(devId)
      }
    } yield cachedOrFromS3
  }

  private def getCached(devId: String): Future[Seq[Reading]] = {
    val cacheKey = s"readings:last10:$devId"
    for {
      readingIds <- redis.smembers[String](cacheKey)
      readingOs  <- redis.mget[Reading](readingIds:_*)
    } yield {
      readingOs.collect{ case Some(x) => x }
    }
  }

  private def getFromS3(devId: String): Future[Seq[Reading]] = {
    val s3Key = s"readings/last10/$devId"
    for {
      readingIds <- s3.list(bucket, s3Key)
      readingOs  <- {
        val readingFs = for {
          readingId <- readingIds
        } yield {
          s3.getJson(bucket, s"$s3Key/$readingId")
        }
        Future.sequence(readingFs)
      }
    } yield {
      val readings = for {
        readingO <- readingOs
        if readingO.isDefined
      } yield {
        readingO.get.as[Reading]
      }
      readings.toSeq
    }
  }
}
