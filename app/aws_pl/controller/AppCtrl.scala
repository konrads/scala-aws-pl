package aws_pl.controller

import aws_pl.repo.{FxRepo, PortfolioRepo, SpotRepo, UserRepo}
import aws_pl.util.AuthenticatedAction
import aws_pl.util.Cats._
import play.api.libs.json.{Json, JsArray}

import scala.concurrent.ExecutionContext
import cats.std.all._

class AppCtrl(userRepo: UserRepo, portfolioRepo: PortfolioRepo, spotRepo: SpotRepo, fxRepo: FxRepo, auth: AuthenticatedAction)
             (implicit ec: ExecutionContext) extends ResultMapper {

  def getUser = auth.async { req =>
    userRepo.getUser(req.user.uid).map(mapResult(_))
  }

  def getMySpots(currency: String) = auth.async { req =>
    val res = for {
      tickers <- f2Xort(portfolioRepo.getPortfolio(req.user.uid))
      spots   <- fs2Xort(spotRepo.getLatestSpots(tickers), NoData)
      fxMap   <- fm2Xort(fxRepo.getFxMap(currency), NoDependency)
    } yield {
      val fxedSpots = for {
        spot <- spots
      } yield {
        spot.copy(
          currency = currency,
          price = spot.price * fxMap(spot.currency)
        )
      }
      val asJs = for { spot <- fxedSpots } yield { Json.toJson(spot) }
      JsArray(asJs)
    }
    res.value.map(mapResult(_))
  }

  def getSpots(tickers: Seq[String]) = auth.async { req =>
    val res = for {
      tickers <- f2Xort(portfolioRepo.getPortfolio(req.user.uid))
      spots   <- fs2Xort(spotRepo.getLatestSpots(tickers), NoData)
    } yield {
      val asJs = for {spot <- spots} yield { Json.toJson(spot) }
      JsArray(asJs)
    }
    res.value.map(mapResult(_))
  }

  def getFx(currency: String) = auth.async { req =>
    fm2Xort(fxRepo.getFxMap(currency), NoData).value.map(mapResult(_))
  }
}
