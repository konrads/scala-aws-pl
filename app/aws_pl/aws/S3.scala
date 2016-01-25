package aws_pl.aws

import java.io.InputStream

import com.amazonaws.regions.Region
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.{AmazonS3Exception, ListObjectsRequest}
import com.github.dwhjames.awswrap.s3.AmazonS3ScalaClient
import play.Logger
import play.api.libs.json.{JsValue, Json}

import scala.collection.JavaConverters._
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

  def list[T](bucket: String, prefix: String, filterPred: (String)=>Boolean, transform: (String)=>T): Future[Stream[T]] = {
    val res = list(bucket, prefix)
    res.map { stream =>
      for {k <- stream if filterPred(k)} yield transform(k)
    }
  }

  def list(bucket: String, prefix: String): Future[Stream[String]] = {
    // iterates pagination
    def listAllKeys(req: ListObjectsRequest, keysSoFar: Stream[String]): Future[Stream[String]] = {
      val contentF = client.listObjects(req)
      contentF.flatMap { ol =>
        val keys = ol.getObjectSummaries.asScala.map(_.getKey)
        val keysSoFar2 = keysSoFar #::: keys.toStream
        if (ol.isTruncated) {
          listAllKeys(req.withMarker(req.getMarker), keysSoFar2)
        } else {
          Future.successful(keysSoFar2)
        }
      }
    }
    val firstReq = new ListObjectsRequest().withBucketName(bucket).withPrefix(prefix)
    listAllKeys(firstReq, Stream[String]())
  }

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
