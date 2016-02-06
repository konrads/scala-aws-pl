package aws_pl.play

import aws_pl.repo.{FxRepo, SpotRepo, PortfolioRepo, UserRepo}
import aws_pl.util.AuthenticatedAction
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import aws_pl.aws._
import aws_pl.controller._
import com.typesafe.config.ConfigObject
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

  def asStrMap(cObj: ConfigObject) =
    cObj.asScala.map { case (k,v) => (k,v.unwrapped().asInstanceOf[String]) }.toMap


  // required config
  val awsRegion          = Region.getRegion(Regions.fromName(configuration.getString("aws.region").get))
  val s3Bucket           = configuration.getString("aws.s3.bucket").get
  val redisHost          = configuration.getString("aws.redis.host").get
  val userTable          = configuration.getString("aws.dynamodb.table.user").get
  val spotTable          = configuration.getString("aws.dynamodb.table.spot").get
  val userTypes          = configuration.getObject("reference_data.user_type").map(asStrMap).get

  // optional config
  val s3Endpoint         = configuration.getString("aws.s3.endpoint")
  val dynamodbEndpoint   = configuration.getString("aws.dynamodb.endpoint")

  Logger.info(
    s"""Config:
      |  awsRegion:        $awsRegion
      |  s3Bucket:         $s3Bucket
      |  redisHost:        $redisHost
      |  userTable:        $userTable
      |  spotTable:        $spotTable
      |  userTypes:        $userTypes
      |  s3Endpoint:       $s3Endpoint
      |  dynamodbEndpoint: $dynamodbEndpoint
    """.stripMargin)

  import actorSystem.dispatcher

  // data stores
  lazy val dynamoDB      = new DynamoDB(awsRegion, dynamodbEndpoint, userTable, spotTable)
  lazy val s3            = new S3(awsRegion, s3Endpoint)
  lazy val redis         = RedisClient(host=redisHost)(actorSystem)

  // repos
  lazy val userRepo      = new UserRepo(dynamoDB, redis, userTypes)
  lazy val portfolioRepo = new PortfolioRepo(s3, s3Bucket, redis)
  lazy val spotRepo      = new SpotRepo(dynamoDB, redis)
  lazy val fxRepo        = new FxRepo(s3, s3Bucket, redis)

  // controller builders
  lazy val auth          = new AuthenticatedAction(userRepo)

  // controllers
  lazy val authTokenCtrl = new AuthTokenCtrl(userRepo)
  lazy val appCtrl       = new AppCtrl(userRepo, portfolioRepo, spotRepo, fxRepo, auth)

  override lazy val router = new Routes(httpErrorHandler, authTokenCtrl, appCtrl)
}
