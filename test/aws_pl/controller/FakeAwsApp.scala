package aws_pl.controller

import java.util.Base64

import aws_pl.play.Components
import play.api.ApplicationLoader.Context
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplicationLoader}
import play.api.{ApplicationLoader, Configuration, Logger}

class FakeAwsApp(reconfig: Configuration => Configuration = x => x) extends WithComponents(new AppLoader(reconfig)) {
  def getToken(user: String, pwd: String): Option[String] = {
    val userPassword = Base64.getEncoder.encodeToString(s"$user:$pwd".getBytes())
    val req = FakeRequest(GET, "/authtkn").withHeaders("Authorization" -> s"Basic $userPassword")
    val respF = route(req).get
    val statusCode = status(respF)
    if (statusCode != OK)
      None
    else {
      for {
        cntTypeO <- contentType(respF)
        token <- {
          val content = contentAsString(respF)
          (Json.parse(content) \ "Authentication-Token").asOpt[String]
        }
      } yield {
        token
      }
    }
  }

  def get(url: String, user: String, pwd: String) = {
    val token = getToken(user, pwd).get
    val req = FakeRequest(GET, url).withHeaders("Authentication-Token" -> token)
    val respF = route(req).get
    (status(respF), contentAsString(respF))
  }

  def put(url: String, user: String, pwd: String, body: String) = {
    val token = getToken(user, pwd).get
    val req = FakeRequest(PUT, url).withHeaders("Authentication-Token" -> token).withBody(Json.parse(body))
    val respF = route(req).get
    status(respF)
  }

  def sanitize(str: String) =
    str.replaceAll("(\"timestamp\":)[\\d]*", "$1xxx")
}

// wrapper class to expose components
class WithComponents(appLoader: AppLoader) extends WithApplicationLoader(appLoader) {
  def components: Components = appLoader.components
}

class AppLoader(reconfig: Configuration => Configuration) extends ApplicationLoader {
  var components: Components = _
  def load(ctx: Context) = {
    val ctx2 = ctx.copy(initialConfiguration = reconfig(ctx.initialConfiguration))
    Logger.configure(ctx2.environment)
    components = new Components(ctx2)
    components.application
  }
}