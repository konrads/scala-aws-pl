package aws_pl.controller

import java.util.Base64
import aws_pl.repo.UserRepo
import cats.data._
import cats.std.all._
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class AuthTokenCtrl(userRepo: UserRepo)(implicit ec: ExecutionContext) extends Controller {
  def get = Action.async { req =>
    val result =
      for {
        credentials <- {
          val c = for {
            authHeader <- req.headers.get("Authorization")
            encoded <- getUserAndPassword(authHeader)
            decoded <- base64Decode(encoded)
            creds <- toCredentials(decoded)
          } yield creds
          OptionT.fromOption[Future](c)
        }
        user <- OptionT(userRepo.getUser(credentials.user))
        if user.password == credentials.password
        token <- OptionT(userRepo.cacheUserId(user.uid))
      } yield {
        token
      }

    result.fold[Result](Results.Unauthorized) {
      token => Ok(Json.obj("token" -> token))
    }
  }

  private def getUserAndPassword(authHeader: String): Option[String] =
    authHeader.split( "\\s") match {
      case Array("Basic", encodedUserAndPass) => Some(encodedUserAndPass)
      case _ => None
    }

  private def base64Decode(encoded:String): Option[String] = try {
      val asArr = Base64.getDecoder.decode(encoded)
      Some(new String(asArr, "UTF-8"))
    } catch {
      case ex: IllegalArgumentException => None
    }

  private def toCredentials(decodedUserAndPass: String): Option[Credentials] = {
    decodedUserAndPass.split(":", 2) match {
      case Array(user, password) => Some(Credentials(user, password))
      case _ => None
    }
  }
}

case class Credentials(user: String, password: String)
