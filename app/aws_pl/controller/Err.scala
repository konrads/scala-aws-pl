package aws_pl.controller

sealed trait Err
case object BadReq extends Err
case object NoData extends Err
case object NoDependency extends Err
