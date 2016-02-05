package aws_pl.aws

import aws_pl.model.{Spot, User}
import com.amazonaws.regions.Region
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import com.github.dwhjames.awswrap.dynamodb._
import play.Logger

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


class DynamoDB(awsRegion: Region, endpoint: Option[String], userTable: String, spotTable: String)
              (implicit ec: ExecutionContext) {
  private val asyncClient: AmazonDynamoDBAsyncClient = {
    val c = new AmazonDynamoDBAsyncClient()
    Logger.info("Running dynamoDB with region: " + awsRegion)
    c.setRegion(awsRegion)
    endpoint.foreach { opt =>
      Logger.info("Running dynamoDB with endpoint: " + opt)
      c.setEndpoint(opt)
    }
    c
  }
  private val client = new AmazonDynamoDBScalaClient(asyncClient)
  private val mapper = AmazonDynamoDBScalaMapper(client)

  object user {
    object Attrs extends Enumeration { val uid, `type`, password = Value }

    implicit object serializer extends DynamoDBSerializer[User] {
      override def tableName = userTable
      override def hashAttributeName = Attrs.uid.toString
      override def primaryKeyOf(user: User) = Map(Attrs.uid.toString -> user.uid)

      override def toAttributeMap(user: User) = Map(
        Attrs.uid.toString -> user.uid,
        Attrs.`type`.toString -> user.`type`)

      override def fromAttributeMap(item: mutable.Map[String, AttributeValue]) = User(
        uid = item(Attrs.uid.toString),
        `type` = item(Attrs.`type`.toString),
        password = item(Attrs.password.toString))
    }

    def getByUid(uid: String): Future[Option[User]] =
      mapper.loadByKey[User](uid)

    def getAll: Future[Seq[User]] =
      mapper.scan[User]()
  }

  object spot {
    object Attrs extends Enumeration { val ticker, timestamp, currency, price  = Value }

    implicit object serializer extends DynamoDBSerializer[Spot] {
      override def tableName = spotTable
      override def hashAttributeName = Attrs.ticker.toString
      override def primaryKeyOf(spot: Spot) = Map(Attrs.ticker.toString -> spot.ticker, Attrs.timestamp.toString -> spot.timestamp)

      override def toAttributeMap(spot: Spot) = Map(
        Attrs.ticker.toString -> spot.ticker,
        Attrs.timestamp.toString -> spot.timestamp,
        Attrs.currency.toString -> spot.currency,
        Attrs.price.toString -> spot.price)

      override def fromAttributeMap(item: mutable.Map[String, AttributeValue]) = Spot(
        ticker = item(Attrs.ticker.toString),
        timestamp = item(Attrs.timestamp.toString).getN.toInt,
        currency = item(Attrs.currency.toString),
        price = item(Attrs.price.toString).getN.toDouble)
    }

    def getByTicker(ticker: String): Future[Option[Spot]] =
      mapper.loadByKey[Spot](ticker)

    def getForPeriod(ticker: String, limit: Int, startTs: Long, endTs: Long): Future[Seq[Spot]] = {
      val req = new QueryRequest()
        .withKeyConditions(Map(
          Attrs.ticker.toString -> QueryCondition.equalTo(ticker),
          Attrs.timestamp.toString -> QueryCondition.between(startTs, endTs)))
        .withScanIndexForward(false)
        .withLimit(limit)
      mapper.queryOnce[Spot](req)
    }
  }
}
