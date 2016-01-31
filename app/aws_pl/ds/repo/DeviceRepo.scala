package aws_pl.ds.repo

import aws_pl.aws.DynamoDB
import aws_pl.model.Device
import cats.data.OptionT
import cats.std.future._
import redis.RedisClient

import scala.concurrent.{ExecutionContext, Future}

class DeviceRepo(dynamoDB: DynamoDB, redis: RedisClient)(implicit ec: ExecutionContext) {
  def getDevices(uid: String): Future[Seq[Device]] =
    dynamoDB.device.getByUid(uid)

  def getDevice(devId: String): Future[Option[Device]] = {
    val s3Key = s"device:$devId"
    val cached = redis.get[Device](s3Key)
    OptionT(cached).orElseF(dynamoDB.device.getByDevId(devId)).value
  }
}
