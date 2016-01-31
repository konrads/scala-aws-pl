package aws_pl.controller

import aws_pl.ds.repo.{DeviceRepo, ReadingRepo, UserRepo}
import aws_pl.model.Device
import aws_pl.util.{Cats, TokenValidated}
import cats.data.{Xor, XorT}
import cats.std.all._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AppCtrl(userRepo: UserRepo, deviceRepo: DeviceRepo, readingRepo: ReadingRepo, tokenVal: TokenValidated)
             (implicit ec: ExecutionContext) extends ResultMapper {

  def getUser = tokenVal.async { req =>
    userRepo.getUser(req.user.uid).map(mapResult(_))
  }

  def getDevice(deviceIdO: Option[String]) = tokenVal.async { req =>
    val res = deviceIdO.map { devId =>
      for {
        device <- Cats.xort[Err, Device](deviceRepo.getDevice(devId), NoData)
        // ensure deviceId belongs to current user
        ownedDevice <- {
          val asXor = if (device.uid == req.user.uid)
            Xor.right[Err, Device](device)
          else
            Xor.left[Err, Device](NoDependency)
          XorT.fromXor[Future](asXor)
        }
      } yield ownedDevice
    }.getOrElse {
      val devFO = for {
        devices <- deviceRepo.getDevices(req.user.uid)
        device <- Future.successful(Try(devices.head).toOption)
      } yield {
        device
      }
      Cats.xort[Err, Device](devFO, NoData)
    }
    res.value.map(mapResult(_))
  }

  def getDevices = tokenVal.async { req =>
    deviceRepo.getDevices(req.user.uid).map { devs =>
      val devs2 = devs.map(Json.toJson(_))
      Ok(Json.arr(devs2))
    }
  }

  // match
  def getReading(readingId: Option[String]) = tokenVal.async { req =>
    // ensure deviceId belongs to current user
    ???
  }

  def getReadings = tokenVal.async { req =>
    ???
  }
}