package aws_pl.controller

import cats.data.Xor
import play.api.mvc._
import play.api.libs.json._

trait ResultMapper extends Controller {
  def mapResult[C](resO: Option[C])(implicit w: Writes[C]): Result =
    resO.fold(NotFound.as(JSON)) { res =>
      Ok(Json.toJson(res))
    }

  def mapResult[C](resOrError: Err Xor C)(implicit w: Writes[C]): Result =
    resOrError.fold({
      case BadParams => BadRequest.as(JSON)
      case NoData => NotFound.as(JSON)
      case NoDependency => FailedDependency.as(JSON)
      case _ => InternalServerError.as(JSON)
    },
      success => Ok(Json.toJson(success))
    )
}

sealed trait Err
case object BadParams extends Err
case object NoData extends Err
case object NoDependency extends Err
