package aws_pl.util

import aws_pl.ds.repo.UserRepo
import aws_pl.model.User
import cats.data.OptionT
import cats.std.future._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import scala.concurrent._

class TokenValidated(userRepo: UserRepo) extends ActionBuilder[WithUserReq] {
  override def invokeBlock[A](req: Request[A], block: (WithUserReq[A]) => Future[Result]): Future[Result] = {
    val result = for {
      token <- OptionT.fromOption(req.headers.get("Authentication-Token"))
      uid <- OptionT(userRepo.getCachedUserId(token))
      user <- OptionT(userRepo.getUser(uid))
      res <- Cats.liftF(block(WithUserReq(req, user)))
    } yield {
      res
    }
    result.getOrElse(Results.Unauthorized("Non-existent/invalid/expired Authentication-Token"))
  }
}

case class WithUserReq[A](req: Request[A], user: User) extends WrappedRequest[A](req)
