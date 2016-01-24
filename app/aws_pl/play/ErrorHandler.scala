package aws_pl.play

import java.io.{PrintWriter, StringWriter}

import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    val errMsg = s"A client error occurred: $message"
    Logger.error(errMsg)
    Future.successful(Status(statusCode)(errMsg))
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error("A server error occurred", exception)
    val errMsg = s"A server error occurred: ${exception.getMessage}, stackTrace:\n${stackTraceStr(exception)}"
    Future.successful(InternalServerError(errMsg))
  }

  def stackTraceStr(t: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    t.printStackTrace(pw)
    sw.toString
  }
}
