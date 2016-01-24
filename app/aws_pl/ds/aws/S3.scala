package aws_pl.ds.aws

import java.io.InputStream

import com.amazonaws.regions.Region
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.github.dwhjames.awswrap.s3.AmazonS3ScalaClient
import play.Logger
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

class S3(awsRegion: Region, endpoint: Option[String])(implicit ec: ExecutionContext) {
  val client = {
    val c = new AmazonS3ScalaClient()
    Logger.info(s"Running s3 with region: $awsRegion")
    c.client.setRegion(awsRegion)
    c.client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))
    endpoint.foreach { opt =>
      Logger.info(s"Running s3 with endpoint: $opt")
      c.client.setEndpoint(opt)
    }
    c
  }

  def getTxt(bucket: String, key: String): Future[Option[String]] =
    streamFromS3(bucket, key)(in => scala.io.Source.fromInputStream(in).mkString)

  def getJson(bucket: String, key: String): Future[Option[JsValue]] =
    streamFromS3(bucket, key)(in => Json.parse(in))

  private def streamFromS3[T](bucket: String, key: String)(f: InputStream => T)(implicit ec: ExecutionContext): Future[Option[T]] =
    client.getObject(bucket, key)
      .map {
        s3Obj =>
          try {
            Some(f(s3Obj.getObjectContent))
          } finally {
            s3Obj.close()
          }
      }
      .recover {
        case e:AmazonS3Exception if e.getStatusCode == 404 => None
      }
}
