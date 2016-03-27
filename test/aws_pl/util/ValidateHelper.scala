package aws_pl.util

import java.util.Base64

import com.fasterxml.jackson.core.JsonParseException
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsObject, JsArray, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
  * Provides validations for GETs and PUTs.
  */
trait ValidateHelper extends SpecificationLike {
  def getToken(user: String, pwd: String): Option[String] = {
    val userPassword = Base64.getEncoder.encodeToString(s"$user:$pwd".getBytes())
    val req = FakeRequest(GET, "/authtkn").withHeaders("Authorization" -> s"Basic $userPassword")
    val respF = route(req).get
    val statusCode = status(respF)
    if (statusCode != OK)
      None
    else
      for {
        cntTypeO <- contentType(respF)
        token <- {
          val content = contentAsString(respF)
          (Json.parse(content) \ "Authentication-Token").asOpt[String]
        }
      } yield token
  }

  def validateGet(url: String, user: String, pwd: String, expRespStatus: Int, expRespBody: Option[String]) = {
    val token = getToken(user, pwd).get
    val req = FakeRequest(GET, url).withHeaders("Authentication-Token" -> token)
    val respF = route(req).get
    status(respF) mustEqual expRespStatus
    expRespBody.map(sanitize(contentAsString(respF)) mustEqual _)
  }

  def validatePut(url: String, user: String, pwd: String, body: String, expRespStatus: Int) = {
    val token = getToken(user, pwd).get
    val req = FakeRequest(PUT, url).withHeaders("Authentication-Token" -> token).withBody(Json.parse(body))
    val respF = route(req).get
    status(respF) mustEqual expRespStatus
  }

  def validateGets(testInfo : (String, (String, String, String, Int, Option[String]))*) = {
    for {
      (desc, (url, user, pwd, expRespStatus, expRespBody)) <- testInfo
    } yield
      desc in new FakeAwsApp {
        validateGet(url, user, pwd, expRespStatus, expRespBody)
      }
  }

  def validatePuts(testInfo : (String, (String, String, String, String, Int))*) = {
    for {
      (desc, (url, user, pwd, body, expRespStatus)) <- testInfo
    } yield
      desc in new FakeAwsApp {
        validatePut(url, user, pwd, body, expRespStatus)
      }
  }

  private def sanitize(str: String): String = {
    def sortJs(js: JsValue): JsValue =
      js match {
        case arr: JsArray  => JsArray(arr.value.map(sortJs).sortBy(_.toString))
        case obj: JsObject => JsObject(obj.value.toSeq.map{ case (k,v) => k -> sortJs(v) }.sortBy{ case (k,_) => k })
        case other => other
      }

    val str2 = str.replaceAll("(\"timestamp\":)[\\d]*", "$1xxx")
    try {
      val js = Json.parse(str2)
      sortJs(js).toString
    } catch {
      case _:JsonParseException => str2
    }
  }
}
