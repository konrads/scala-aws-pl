package aws_pl.util

import cats.Functor
import cats.data.{OptionT, Xor, XorT}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.language.higherKinds

object Cats {
  def liftF[F[_], T](f: F[T])(implicit F: Functor[F]): OptionT[F, T] =
    OptionT(F.map(f)(Some(_)))

  def xort[T1, T2](f: Future[Option[T2]], default: T1): XorT[Future, T1, T2] =
    XorT(f.map(Xor.fromOption(_, default)))
}
