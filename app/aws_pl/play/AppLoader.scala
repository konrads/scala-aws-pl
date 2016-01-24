package aws_pl.play

import _root_.redis.RedisClient
import _root_.router.Routes
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import aws_pl.ds.aws._
import aws_pl.ds.repo._
import aws_pl.controller._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.libs.ws.ning.NingWSComponents
import redis.RedisClient
import router.Routes
import scala.collection.JavaConverters._

class AppLoader extends ApplicationLoader {
  def load(context: Context) = {
    Logger.configure(context.environment)
    new Components(context).application
  }
}

class Components(context: Context)
  extends BuiltInComponentsFromContext(context)
  with NingWSComponents {
  val awsRegion        = Region.getRegion(Regions.fromName(configuration.getString("aws.region").get))
  val s3Endpoint       = configuration.getString("aws.s3.endpoint")
  val dynamodbEndpoint = configuration.getString("aws.dynamodb.endpoint")
  val s3Bucket         = configuration.getString("aws.s3.bucket")
  val redisHost        = configuration.getString("aws.redis.host").get
  val s3DevicePath     = configuration.getString("aws.s3.path.device").get
  val userTable        = configuration.getString("aws.dynamodb.table.user").get
  val deviceTable      = configuration.getString("aws.dynamodb.table.device").get
  val readingTable     = configuration.getString("aws.dynamodb.table.reading").get
  val deviceNames      = configuration.getObject("reference_data.device_names").map(_.asScala.map { case (k,v) => (k,v.unwrapped().asInstanceOf[String]) }).get

  import actorSystem.dispatcher

  // data stores
  lazy val dynamoDB    = new DynamoDB(awsRegion, dynamodbEndpoint, userTable, deviceTable, readingTable)
  lazy val s3          = new S3(awsRegion, s3Endpoint)
  lazy val redis       = RedisClient(host=redisHost)(actorSystem)

  // repos
  lazy val userRepo    = new UserRepo(dynamoDB, redis)
  lazy val deviceRepo  = new DeviceRepo(dynamoDB, redis)
  lazy val readingRepo = new ReadingRepo(s3, redis)

  // controller buiders
  lazy val tokenVal    = new TokenValidated(userRepo)

  // controllers
  lazy val tokenCtrl   = new TokenCtrl(userRepo)
  lazy val userCtrl    = new UserCtrl(tokenVal, userRepo)
  lazy val deviceCtrl  = new DeviceCtrl(tokenVal, deviceRepo)
  lazy val readingCtrl = new ReadingCtrl(tokenVal, readingRepo, deviceRepo)

  override lazy val router = new Routes(httpErrorHandler, tokenCtrl, userCtrl, deviceCtrl)
}
