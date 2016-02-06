package aws_pl.repo

import aws_pl.aws.S3
import aws_pl.model.Fx
import play.api.libs.json.JsArray
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class FxRepo(s3: S3, s3Bucket: String, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getLatestFx: Future[Seq[Fx]] = {
    for {
      cached <- redis.smembers[Fx](s"fx:mapping")
      cachedOrFromS3 <- {
        if (cached.nonEmpty)
          Future.successful(cached)
        else
          s3.getJson(s3Bucket, "fx/mapping.json").map(o => o.map(_.as[JsArray].value.map(_.as[Fx])).getOrElse(Nil))
      }
    } yield cachedOrFromS3
  }

  def getFxMap(targetCurrency: String): Future[Map[String, Double]] =
    getLatestFx.map { fxs =>
      val mappings = fxs.collect {
        case fx: Fx if fx.sourceCurrency == targetCurrency => fx.targetCurrency -> fx.multiplier
      }
      Map(mappings:_*)
    }
}
