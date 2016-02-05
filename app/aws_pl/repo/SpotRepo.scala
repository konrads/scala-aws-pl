package aws_pl.repo

import aws_pl.aws.DynamoDB
import aws_pl.model.Spot
import org.joda.time.DateTime
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class SpotRepo(dynamoDB: DynamoDB, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getLatestSpots(tickers: Seq[String]): Future[Seq[Spot]] = {
    val spotFOs = for { ticker <- tickers } yield getLatestSpot(ticker)
    val res = Future.sequence(spotFOs).map { spotOs =>
      for { spotO <- spotOs if spotO.isDefined } yield spotO.get
    }
    res
  }

  def getLatestSpot(ticker: String): Future[Option[Spot]] =
    for {
      cached <- redis.get[Spot](s"latest-ticker:$ticker")
      cachedOrFromDb <- {
        if (cached.nonEmpty)
          Future.successful(cached)
        else
          dynamoDB.spot.getByTicker(ticker)
      }
    } yield cachedOrFromDb

  def getSpots(ticker: String, limit: Int, sinceTs: Long): Future[Seq[Spot]] =
    dynamoDB.spot.getForPeriod(ticker, limit, sinceTs, DateTime.now.getMillis)
}
