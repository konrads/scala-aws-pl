package aws_pl.util

import aws_pl.controller.Err
import cats.Functor
import cats.data.{OptionT, Xor, XorT}
import cats.std.all._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.language.higherKinds

object Cats {
  def f2Optt[F[_], T](f: F[T])(implicit F: Functor[F]): OptionT[F, T] =
    OptionT(F.map(f)(Some(_)))

  def fs2Xort[R](f: Future[Seq[R]], default: Err): XorT[Future, Err, Seq[R]] = {
    val fo = f.map {
      case Nil => None
      case seq => Some(seq)
    }
    fo2Xort(fo, default)
  }

  def fm2Xort[K, V](f: Future[Map[K, V]], default: Err): XorT[Future, Err, Map[K, V]] = {
    val fo = f.map {
      case map if map.isEmpty => None
      case map => Some(map)
    }
    fo2Xort(fo, default)
  }

  def f2Xort[R](f: Future[R]): XorT[Future, Err, R] =
    XorT.right[Future, Err, R](f)

  def fo2Xort[R](f: Future[Option[R]], default: Err): XorT[Future, Err, R] =
    XorT(f.map(Xor.fromOption(_, default)))

  def xor2Xort[R](xor: Err Xor R): XorT[Future, Err, R] =
    XorT.fromXor[Future](xor)

  def right2Xort[R](pure: R): XorT[Future, Err, R] =
    XorT.pure[Future, Err, R](pure)
}
