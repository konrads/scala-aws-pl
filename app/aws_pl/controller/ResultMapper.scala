package aws_pl.controller

import cats.data.Xor
import play.api.http.{ContentTypes, Writeable}
import play.api.mvc._

trait ResultMapper extends Results with ContentTypes {
  def map[C](resOrError: Err Xor C)(implicit writeable: Writeable[C]): Result =
    resOrError.fold({
      case BadParams => BadRequest.as(JSON)
      case NoContent => NotFound.as(JSON)
      case NoDependency => FailedDependency.as(JSON)
      case _ => InternalServerError.as(JSON)
    },
      success => Ok(success)
    )
}

sealed trait Err
case object BadParams extends Err
case object NoContent extends Err
case object NoDependency extends Err
