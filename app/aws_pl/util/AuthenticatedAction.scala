package aws_pl.util

import aws_pl.model.User
import aws_pl.repo.UserRepo
import aws_pl.util.Cats._
import cats.data.OptionT
import cats.std.future._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import scala.concurrent._

class AuthenticatedAction(userRepo: UserRepo) extends ActionBuilder[WithUserReq] {
  val authTokenHeader = "Authentication-Token"

  override def invokeBlock[A](req: Request[A], block: (WithUserReq[A]) => Future[Result]): Future[Result] = {
    val result = for {
      token <- OptionT.fromOption(req.headers.get(authTokenHeader))
      uid   <- OptionT(userRepo.getCachedUserId(token))
      user  <- OptionT(userRepo.getUser(uid))
      res   <- f2Optt(block(WithUserReq(req, user)))
    } yield {
      res
    }
    result.getOrElse(Results.Unauthorized(s"Non-existent/invalid/expired $authTokenHeader"))
  }
}

case class WithUserReq[A](req: Request[A], user: User) extends WrappedRequest[A](req)
