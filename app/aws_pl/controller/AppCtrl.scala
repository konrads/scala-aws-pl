package aws_pl.controller

import aws_pl.model.Spot
import aws_pl.repo.{FxRepo, PortfolioRepo, SpotRepo, UserRepo}
import aws_pl.util.AuthenticatedAction
import aws_pl.util.Cats._
import cats.data.OptionT
import play.api.libs.json.{JsObject, Json, JsArray}
import play.api.mvc.BodyParsers

import scala.concurrent.{Future, ExecutionContext}
import cats.std.all._

class AppCtrl(userRepo: UserRepo, portfolioRepo: PortfolioRepo, spotRepo: SpotRepo, fxRepo: FxRepo,
              auth: AuthenticatedAction, userTypes: Map[String, String])
             (implicit ec: ExecutionContext) extends ResultMapper {

  def getUser = auth.async { req =>
    val userOptt = OptionT(userRepo.getUser(req.user.uid)).map { user =>
      val withFixedType = user.copy(`type` = userTypes(user.`type`))
      withFixedType.asPublic
    }
    userOptt.value.map(mapResult(_))
  }

  def getMySpots(targetCurrency: String) = auth.async { req =>
    val res = for {
      tickers <- f2Xort(portfolioRepo.getPortfolio(req.user.uid))
      spots   <- fs2Xort(spotRepo.getLatestSpots(tickers), NoData)
      fxMap   <- fm2Xort(fxRepo.getFxMap(targetCurrency), NoDependency)
    } yield {
      val fxedSpots = for {
        spot <- spots
      } yield {
        if (spot.currency == targetCurrency)
          spot
        else
          spot.copy(
            currency = targetCurrency,
            price = spot.price * fxMap(spot.currency)
          )
      }
      val asJs = for { spot <- fxedSpots } yield { Json.toJson(spot) }
      JsArray(asJs)
    }
    res.value.map(mapResult(_))
  }

  def getSpots(tickers: Option[String]) = auth.async { req =>
    val res = for {
      definedOrMyTickers <- {
        if (tickers.nonEmpty)
          right2Xort(tickers.get.split(",").toSeq)
        else
          f2Xort(portfolioRepo.getPortfolio(req.user.uid))
      }
      spots <- fs2Xort(spotRepo.getLatestSpots(definedOrMyTickers), NoData)
    } yield {
      val asJs = for {spot <- spots} yield { Json.toJson(spot) }
      JsArray(asJs)
    }
    res.value.map(mapResult(_))
  }

  def getFx(currency: String) = auth.async { req =>
    fm2Xort(fxRepo.getFxMap(currency), NoData).value.map(mapResult(_))
  }

  def putSpot = auth.async(BodyParsers.parse.json) { req =>
    // FIXME: json schema validation?
    val spotO = req.body.validate[Spot].asOpt
    val res = if (spotO.nonEmpty)
      spotRepo.saveSpot(spotO.get)map(o => Some(JsObject(Seq())))
    else
      Future.successful(None)
    res.map(mapResult(_))
  }
}
