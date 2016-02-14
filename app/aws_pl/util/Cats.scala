package aws_pl.util

import aws_pl.controller.Err
import cats.Functor
import cats.data.{OptionT, Xor, XorT}
import cats.implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Transform helpers that encode correct types, implicits.
  */
object Cats {
  def f2Optt[F[_], T](f: F[T])(implicit F: Functor[F]): OptionT[F, T] =
    OptionT(F.map(f)(Some(_)))

  def fs2Xort[R](f: Future[Seq[R]], default: Err): XorT[Future, Err, Seq[R]] =
    XorT(f.map {
      case Nil => Xor.Left(default)
      case seq => Xor.Right(seq)
    })

  def fm2Xort[K, V](f: Future[Map[K, V]], default: Err): XorT[Future, Err, Map[K, V]] =
    XorT(f.map {
      case map if map.isEmpty => Xor.Left(default)
      case map => Xor.Right(map)
    })

  def f2Xort[R](f: Future[R]): XorT[Future, Err, R] =
    XorT.right[Future, Err, R](f)

  def fo2Xort[R](f: Future[Option[R]], default: Err): XorT[Future, Err, R] =
    XorT(f.map(Xor.fromOption(_, default)))

  def xor2Xort[R](xor: Err Xor R): XorT[Future, Err, R] =
    XorT.fromXor[Future](xor)

  def right2Xort[R](r: R): XorT[Future, Err, R] =
    XorT.pure[Future, Err, R](r)

  def left2Xort[R](err: Err): XorT[Future, Err, R] =
    XorT.left[Future, Err, R](Future.successful(err))
}
