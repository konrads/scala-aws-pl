package aws_pl.repo

import aws_pl.aws.S3
import play.api.libs.json.JsArray
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class PortfolioRepo(s3: S3, bucket: String, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getPortfolio(uid: String): Future[Seq[String]] = {
    val cacheKey = s"portfolio:$uid"
    val s3Key = s"portfolio/$uid"
    for {
      cached <- redis.smembers[String](cacheKey)
      cachedOrFromDynamo <- {
        if (cached.nonEmpty)
          Future.successful(cached)
        else
          s3.getJson(bucket, s3Key).map {
            case None => Nil
            case Some(tickers) => tickers.as[JsArray].value.map(_.toString())
          }
      }
    } yield {
      cachedOrFromDynamo
    }
  }
}
