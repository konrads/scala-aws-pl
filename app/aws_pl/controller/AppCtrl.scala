package aws_pl.controller

import aws_pl.model.{Spot, SpotPublic}
import aws_pl.repo.{FxRepo, PortfolioRepo, SpotRepo, UserRepo}
import aws_pl.util.AuthenticatedAction
import aws_pl.util.Cats._
import cats.data.OptionT
import cats.implicits._
import org.joda.time.DateTime
import play.api.libs.json.{Json, JsArray}
import play.api.mvc.{Result, BodyParsers}

import scala.concurrent.{ExecutionContext, Future}

class AppCtrl(userRepo: UserRepo, portfolioRepo: PortfolioRepo, spotRepo: SpotRepo, fxRepo: FxRepo,
              auth: AuthenticatedAction, userTypes: Map[String, String])
             (implicit ec: ExecutionContext) extends ResultMapper {

  def getUser = auth.asyncOptt { req =>
    OptionT(userRepo.getUser(req.user.uid)).map { user =>
      val publicUser = user.copy(`type` = userTypes(user.`type`)).asPublic
      Ok(Json.toJson(publicUser))
    }
  }

  def getMySpots(targetCurrency: String) = auth.asyncXort { req =>
    def canBeConverted(spots: Seq[Spot], availableCurrencies: Set[String]) = {
      val needConversion = (for { s <- spots } yield { s.currency }).toSet - targetCurrency
      needConversion.subsetOf(availableCurrencies)
    }

    for {
      tickers <- f2Xort(portfolioRepo.getPortfolio(req.user.uid))
      spots   <- fs2Xort(spotRepo.getLatestSpots(tickers), NoData)
      fxMap   <- fm2Xort(fxRepo.getFxMap(targetCurrency), NoDependency)
      _       <- {
        // ensure targetCurrency is within fxMap
        if (canBeConverted(spots, fxMap.keySet)) right2Xort(true)
        else left2Xort(NoDependency)
      }
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
      Ok(JsArray(asJs))
    }
  }

  def getSpots(tickers: Option[String]) = auth.asyncXort { req =>
    for {
      definedOrMyTickers <- {
        if (tickers.nonEmpty)
          right2Xort(tickers.get.split(",").toSeq)
        else
          f2Xort(portfolioRepo.getPortfolio(req.user.uid))
      }
      spots <- fs2Xort(spotRepo.getLatestSpots(definedOrMyTickers), NoData)
    } yield {
      val asJs = for {spot <- spots} yield { Json.toJson(spot) }
      Ok(JsArray(asJs))
    }
  }

  def getFx(currency: String) = auth.asyncXort { req =>
    fm2Xort(fxRepo.getFxMap(currency), NoData).map { fx =>
      Ok(Json.toJson(fx))
    }
  }

  def putSpot = auth.asyncOptt(BodyParsers.parse.json) { req =>
    // FIXME: json schema validation?
    val res: Future[Option[Result]] = req.body.validate[SpotPublic].asOpt match {
      case Some(spot) =>
        val now = DateTime.now.getMillis/1000
        val asPrivate = spot.toPrivate(now)
        spotRepo.saveSpot(asPrivate).map(_ => Some(Ok(JsArray())))
      case None =>
        Future.successful(None)
    }
    OptionT(res)
  }
}
