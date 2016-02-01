package aws_pl.aws

import aws_pl.model.{Device, Reading, User}
import com.amazonaws.regions.Region
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, QueryRequest}
import com.github.dwhjames.awswrap.dynamodb._
import play.Logger

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


class DynamoDB(awsRegion: Region, endpoint: Option[String], userTable: String, deviceTable: String, readingTable: String)
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
    object Attrs extends Enumeration { val uid, role, password = Value }

    implicit object serializer extends DynamoDBSerializer[User] {
      override def tableName = userTable
      override def hashAttributeName = Attrs.uid.toString
      override def primaryKeyOf(user: User) = Map(Attrs.uid.toString -> user.uid)

      override def toAttributeMap(user: User) = Map(
        Attrs.uid.toString -> user.uid,
        Attrs.role.toString -> user.role)

      override def fromAttributeMap(item: mutable.Map[String, AttributeValue]) = User(
        uid = item(Attrs.uid.toString),
        role = item(Attrs.role.toString),
        password = item(Attrs.password.toString))
    }

    def getByUid(uid: String): Future[Option[User]] =
      mapper.loadByKey[User](uid)

    def getAll: Future[Seq[User]] =
      mapper.scan[User]()
  }

  object device {
    object Attrs extends Enumeration { val devId, uid, `type`  = Value }

    implicit object serializer extends DynamoDBSerializer[Device] {
      override def tableName = deviceTable
      override def hashAttributeName = Attrs.devId.toString
      override def primaryKeyOf(device: Device) = Map(Attrs.devId.toString -> device.uid)

      override def toAttributeMap(device: Device) = Map(
        Attrs.devId.toString -> device.devId,
        Attrs.uid.toString -> device.uid,
        Attrs.`type`.toString -> device.`type`)

      override def fromAttributeMap(item: mutable.Map[String, AttributeValue]) = Device(
        devId = item(Attrs.devId.toString),
        uid = item(Attrs.uid.toString),
        `type` = item(Attrs.`type`.toString))
    }

    def getByDevId(devId: String): Future[Option[Device]] =
      mapper.loadByKey[Device](devId)

    def getByUid(uid: String): Future[Seq[Device]] = {
      val req = new QueryRequest()
        .withKeyConditions(Map(Attrs.uid.toString -> QueryCondition.equalTo(uid)))
      mapper.queryOnce[Device](req)
    }
  }

  object reading {
    object Attrs extends Enumeration { val devId, timestamp, `type`, reading  = Value }

    implicit object serializer extends DynamoDBSerializer[Reading] {
      override def tableName = readingTable
      override def hashAttributeName = Attrs.devId.toString
      override def primaryKeyOf(reading: Reading) = Map(Attrs.devId.toString -> reading.devId, Attrs.timestamp.toString -> reading.timestamp)

      override def toAttributeMap(reading: Reading) = Map(
        Attrs.devId.toString -> reading.devId,
        Attrs.timestamp.toString -> reading.timestamp,
        Attrs.`type`.toString -> reading.`type`,
        Attrs.reading.toString -> reading.reading)

      override def fromAttributeMap(item: mutable.Map[String, AttributeValue]) = Reading(
        devId = item(Attrs.devId.toString),
        timestamp = item(Attrs.timestamp.toString).getN.toInt,
        `type` = item(Attrs.`type`.toString),
        reading = item(Attrs.reading.toString).getN.toDouble)
    }

    def getByReadingId(readingId: String): Future[Option[Reading]] =
      mapper.loadByKey[Reading](readingId)

    def getForPeriod(devId: String, limit: Int, startTs: Int, endTs: Int): Future[Seq[Reading]] = {
      val req = new QueryRequest()
        .withKeyConditions(Map(
          Attrs.devId.toString -> QueryCondition.equalTo(devId),
          Attrs.timestamp.toString -> QueryCondition.between(startTs, endTs)))
        .withScanIndexForward(false)
        .withLimit(limit)
      mapper.queryOnce[Reading](req)
    }
  }
}
