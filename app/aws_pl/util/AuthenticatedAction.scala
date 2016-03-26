package aws_pl.util

import aws_pl.controller.{NoDependency, NoData, BadReq, Err}
import aws_pl.model.User
import aws_pl.repo.UserRepo
import aws_pl.util.Cats._
import cats.data.{Xor, XorT, OptionT}
import cats.std.future._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.ContentTypes

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

  def syncXor(block: WithUserReq[AnyContent] => Err Xor Result): Action[AnyContent] =
    syncXor(BodyParsers.parse.default)(block)

  def syncXor[T](bp: BodyParser[T])(block: WithUserReq[T] => Err Xor Result): Action[T] =
    apply(bp)(block.andThen(mapResult))

  def syncOpt(block: WithUserReq[AnyContent] => Option[Result]): Action[AnyContent] =
    syncOpt(BodyParsers.parse.default)(block)

  def syncOpt[T](bp: BodyParser[T])(block: WithUserReq[T] => Option[Result]): Action[T] =
    apply(bp)(block.andThen(mapResult))

  def asyncXort(block: WithUserReq[AnyContent] => XorT[Future, Err, Result]): Action[AnyContent] =
    asyncXort(BodyParsers.parse.default)(block)

  def asyncXort[T](bp: BodyParser[T])(block: WithUserReq[T] => XorT[Future, Err, Result]): Action[T] =
    async(bp)(block.andThen(s => s.value.map(mapResult)))

  def asyncOptt(block: WithUserReq[AnyContent] => OptionT[Future, Result]): Action[AnyContent] =
    asyncOptt(BodyParsers.parse.default)(block)

  def asyncOptt[T](bp: BodyParser[T])(block: WithUserReq[T] => OptionT[Future, Result]): Action[T] =
    async(bp)(block.andThen(s => s.value.map(mapResult)))

  private def mapResult(resO: Option[Result]): Result =
    resO.getOrElse(NotFound.as(ContentTypes.JSON))

  private def mapResult(resOrError: Err Xor Result): Result =
    resOrError.fold({
      case BadReq       => BadRequest.as(ContentTypes.JSON)
      case NoData       => NotFound.as(ContentTypes.JSON)
      case NoDependency => FailedDependency.as(ContentTypes.JSON)
      case _            => InternalServerError.as(ContentTypes.JSON)
    },
      success => success
    )
}

case class WithUserReq[T](req: Request[T], user: User) extends WrappedRequest[T](req)
