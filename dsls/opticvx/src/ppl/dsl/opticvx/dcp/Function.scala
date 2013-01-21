package ppl.dsl.opticvx.dcp

import ppl.dsl.opticvx.common._
import ppl.dsl.opticvx.model._
import scala.collection.immutable.Seq

object Function {
  def param(idx: Int, argSize: Seq[IRPoly]): Function = {
    val irp0: IRPoly = IRPoly.const(0, argSize(0).arity)
    Function(
      argSize,
      SignumPoly.param(idx, argSize.length),
      for(i <- 0 until argSize.length) yield SignumPoly.const(if (i == idx) Signum.Positive else Signum.Zero, argSize.length),
      SignumPoly.const(Signum.Zero, argSize.length),
      irp0,
      for(i <- 0 until argSize.length) yield if (i == idx) AlmapIdentity(argSize(idx)) else AlmapZero(argSize(i), argSize(idx)),
      AlmapZero(irp0, argSize(idx)),
      AVectorZero(argSize(idx)),
      for(i <- 0 until argSize.length) yield AlmapZero(argSize(i), irp0),
      AlmapZero(irp0, irp0),
      AVectorZero(irp0),
      for(i <- 0 until argSize.length) yield AlmapZero(argSize(i), irp0),
      AlmapZero(irp0, irp0),
      AVectorZero(irp0),
      ConeZero(irp0.arity))
  }

  def const(c: AVector, argSize: Seq[IRPoly]): Function = {
    val irp0: IRPoly = IRPoly.const(0, argSize(0).arity)
    Function(
      argSize,
      SignumPoly.const(Signum.All, argSize.length),
      for(i <- 0 until argSize.length) yield SignumPoly.const(Signum.Zero, argSize.length),
      SignumPoly.const(Signum.Zero, argSize.length),
      irp0,
      for(i <- 0 until argSize.length) yield AlmapZero(argSize(i), c.size),
      AlmapZero(irp0, c.size),
      c,
      for(i <- 0 until argSize.length) yield AlmapZero(argSize(i), irp0),
      AlmapZero(irp0, irp0),
      AVectorZero(irp0),
      for(i <- 0 until argSize.length) yield AlmapZero(argSize(i), irp0),
      AlmapZero(irp0, irp0),
      AVectorZero(irp0),
      ConeZero(irp0.arity))    
  }

  def fromcone(cone: Cone): Function = {
    val irp0 = IRPoly.const(0, cone.arity)
    val irp1 = IRPoly.const(1, cone.arity)
    Function(
      Seq(cone.size),
      SignumPoly.const(Signum.Positive, 1),
      Seq(SignumPoly.const(Signum.All, 1)),
      SignumPoly.const(Signum.Positive, 1),
      irp0,
      Seq(AlmapZero(cone.size, irp1)),
      AlmapZero(irp0, irp1),
      AVectorZero(irp1),
      Seq(AlmapZero(cone.size, irp0)),
      AlmapZero(irp0, irp0),
      AVectorZero(irp0),
      Seq(AlmapIdentity(cone.size)),
      AlmapZero(irp0, cone.size),
      AVectorZero(cone.size),
      cone
    )
  }
}

case class Function(
  // sizes of the input arguments to this function
  val argSize: Seq[IRPoly],
  // polynomial to determine the sign of this function
  val sign: SignumPoly,
  // polynomial to determine the tonicity of this function
  val tonicity: Seq[SignumPoly],
  // polynomial to determine the vexity of this function
  val vexity: SignumPoly,
  // size of the inner variable of this function
  var varSize: IRPoly,
  // value and constraints for inner problem (objective is return value)
  val valueArgAlmap: Seq[Almap],
  val valueVarAlmap: Almap,
  val valueOffset: AVector,
  val affineArgAlmap: Seq[Almap],
  val affineVarAlmap: Almap,
  val affineOffset: AVector,
  val conicArgAlmap: Seq[Almap],
  val conicVarAlmap: Almap,
  val conicOffset: AVector,
  val conicCone: Cone) extends HasArity[Function]
{
  val arity: Int = varSize.arity
  // first, make sure that the signum polynomials have the correct number of inputs
  // two inputs for each argument, one for sign and one for vexity
  if(vexity.arity != argSize.length) throw new IRValidationException()
  if(sign.arity != argSize.length) throw new IRValidationException()
  if(tonicity.length != argSize.length) throw new IRValidationException()
  for(t <- tonicity) {
    if(t.arity != argSize.length) throw new IRValidationException()
  }
  // next, verify that all the constraints have the appropriate size
  // this also implicitly verifies that all the arguments have the same arity
  if(valueArgAlmap.length != argSize.length) throw new IRValidationException()
  if(affineArgAlmap.length != argSize.length) throw new IRValidationException()
  if(conicArgAlmap.length != argSize.length) throw new IRValidationException()
  for(i <- 0 until argSize.length) {
    if(valueArgAlmap(i).domain != argSize(i)) throw new IRValidationException()
    if(affineArgAlmap(i).domain != argSize(i)) throw new IRValidationException()
    if(conicArgAlmap(i).domain != argSize(i)) throw new IRValidationException() 
  }
  if(valueVarAlmap.domain != varSize) throw new IRValidationException()
  if(affineVarAlmap.domain != varSize) throw new IRValidationException()
  if(conicVarAlmap.domain != varSize) throw new IRValidationException()
  // verify that all codomains agree
  if(valueVarAlmap.codomain != valueOffset.size) throw new IRValidationException()
  if(affineVarAlmap.codomain != affineOffset.size) throw new IRValidationException()
  if(conicVarAlmap.codomain != conicOffset.size) throw new IRValidationException()
  if(conicVarAlmap.codomain != conicCone.size) throw new IRValidationException()
  for(i <- 0 until argSize.length) {
    if(valueArgAlmap(i).codomain != valueOffset.size)
    if(affineArgAlmap(i).codomain != affineOffset.size) throw new IRValidationException()
    if(conicArgAlmap(i).codomain != conicOffset.size) throw new IRValidationException()
  }

  def arityOp(op: ArityOp): Function = Function(
    argSize map (x => x.arityOp(op)),
    sign,
    tonicity,
    vexity,
    varSize.arityOp(op),
    valueArgAlmap map (x => x.arityOp(op)),
    valueVarAlmap.arityOp(op),
    valueOffset.arityOp(op),
    affineArgAlmap map (x => x.arityOp(op)),
    affineVarAlmap.arityOp(op),
    affineOffset.arityOp(op),
    conicArgAlmap map (x => x.arityOp(op)),
    conicVarAlmap.arityOp(op),
    conicOffset.arityOp(op),
    conicCone.arityOp(op))

  // is this function input-invariant?
  def isPure: Boolean = 
    valueArgAlmap.foldLeft(true)((a,b) => a && b.isPure) &&
    valueVarAlmap.isPure &&
    valueOffset.isPure && 
    affineArgAlmap.foldLeft(true)((a,b) => a && b.isPure) &&
    affineVarAlmap.isPure &&
    affineOffset.isPure &&
    conicArgAlmap.foldLeft(true)((a,b) => a && b.isPure) &&
    conicVarAlmap.isPure &&
    conicOffset.isPure

  // is this an indicator function?
  def isIndicator: Boolean = 
    valueArgAlmap.foldLeft(true)((a,b) => a && b.is0) &&
    valueVarAlmap.is0 &&
    valueOffset.is0

  def +(y: Function): Function = {
    // the two functions to be added must take the same arguments
    if(argSize != y.argSize) throw new IRValidationException()
    // form the output function
    Function(
      argSize,
      sign + y.sign,
      for(i <- 0 until argSize.length) yield tonicity(i) + y.tonicity(i),
      vexity + y.vexity,
      varSize + y.varSize,
      for(i <- 0 until argSize.length) yield valueArgAlmap(i) + y.valueArgAlmap(i),
      AlmapHCat(valueVarAlmap, y.valueVarAlmap),
      valueOffset + y.valueOffset,
      for(i <- 0 until argSize.length) yield AlmapVCat(affineArgAlmap(i), y.affineArgAlmap(i)),
      Almap.diagCat(affineVarAlmap, y.affineVarAlmap),
      affineOffset ++ y.affineOffset,
      for(i <- 0 until argSize.length) yield AlmapVCat(conicArgAlmap(i), y.conicArgAlmap(i)),
      Almap.diagCat(conicVarAlmap, y.conicVarAlmap),
      conicOffset ++ y.conicOffset,
      ConeProduct(conicCone, y.conicCone))
  }

  def unary_-(): Function = Function(
    argSize,
    -sign,
    tonicity map (x => -x),
    -vexity,
    varSize,
    valueArgAlmap map (x => -x),
    -valueVarAlmap,
    -valueOffset,
    affineArgAlmap,
    affineVarAlmap,
    affineOffset,
    conicArgAlmap,
    conicVarAlmap,
    conicOffset,
    conicCone)

  def -(y: Function): Function = this + (-y)

  def scale(c: Double): Function = Function(
    argSize,
    sign * Signum.sgn(c),
    tonicity map (x => x * Signum.sgn(c)),
    vexity * Signum.sgn(c),
    varSize,
    valueArgAlmap map (x => AlmapScaleConstant(x, c)),
    AlmapScaleConstant(valueVarAlmap, c),
    AVectorScaleConstant(valueOffset, c),
    affineArgAlmap,
    affineVarAlmap,
    affineOffset,
    conicArgAlmap,
    conicVarAlmap,
    conicOffset,
    conicCone)

  def compose(ys: Seq[Function]): Function = {
    // verify that the same number of arguments are given for both functions
    if(ys.length != argSize.length) throw new IRValidationException()
    for(i <- 0 until ys.length) {
      if(ys(i).argSize.length != ys(0).argSize.length) throw new IRValidationException()
    }
    // form the output function
    val ysnumargs: Int = ys(0).argSize.length
    Function(
      //argSize
      ys(0).argSize,
      //sign
      sign.evalpoly(ysnumargs, ys map (x => x.sign)),
      //tonicity
      for(i <- 0 until ysnumargs) yield {
        var tacc: SignumPoly = SignumPoly.const(Signum.Zero, ysnumargs)
        for(j <- 0 until argSize.length) {
          tacc = tacc + tonicity(j).evalpoly(ysnumargs, ys map (x => x.sign)) * ys(j).tonicity(i)
        }
        tacc
      },
      //vexity
      {
        var vacc: SignumPoly = vexity.evalpoly(ysnumargs, ys map (x => x.sign))
        for(j <- 0 until argSize.length) {
          vacc = vacc + tonicity(j).evalpoly(ysnumargs, ys map (x => x.sign)) * ys(j).vexity
        }
        vacc
      },
      //varSize
      ys.foldLeft(varSize)((b,a) => b + a.varSize),
      //valueArgAlmap
      for(i <- 0 until ysnumargs) yield {
        var acc: Almap = valueArgAlmap(0) * ys(0).valueArgAlmap(i)
        for(j <- 1 until argSize.length) {
          acc = acc + valueArgAlmap(j) * ys(j).valueArgAlmap(i)
        }
        acc
      },
      //valueVarAlmap
      {
        var acc: Almap = valueVarAlmap
        for(j <- 0 until argSize.length) {
          acc = AlmapHCat(acc, valueArgAlmap(j) * ys(j).valueVarAlmap)
        }
        acc
      },
      //valueOffset
      {
        implicit val avlav = AVectorLikeAVector(arity)
        var acc: AVector = valueOffset
        for(j <- 0 until argSize.length) {
          acc = acc + valueArgAlmap(j) * ys(j).valueOffset
        }
        acc
      },
      //affineArgAlmap
      for(i <- 0 until ysnumargs) yield {
        var acc: Almap = affineArgAlmap(0) * ys(0).valueArgAlmap(i)
        for(j <- 1 until argSize.length) {
          acc = acc + affineArgAlmap(j) * ys(j).valueArgAlmap(i)
        }
        for(j <- 0 until argSize.length) {
          acc = AlmapVCat(acc, ys(j).affineArgAlmap(i))
        }
        acc
      },
      //affineVarAlmap
      {
        var acc: Almap = affineVarAlmap
        for(j <- 0 until argSize.length) {
          acc = Almap.diagCat(acc, ys(j).affineVarAlmap)
        }
        acc
      },
      //affineOffset
      {
        implicit val avlav = AVectorLikeAVector(arity)
        var acc: AVector = affineOffset
        for(j <- 0 until argSize.length) {
          acc = acc + affineArgAlmap(j) * ys(j).affineOffset
        }
        for(j <- 0 until argSize.length) {
          acc = acc ++ ys(j).affineOffset
        }
        acc
      },
      //conicArgAlmap
      for(i <- 0 until ysnumargs) yield {
        var acc: Almap = conicArgAlmap(0) * ys(0).valueArgAlmap(i)
        for(j <- 1 until argSize.length) {
          acc = acc + conicArgAlmap(j) * ys(j).valueArgAlmap(i)
        }
        for(j <- 0 until argSize.length) {
          acc = AlmapVCat(acc, ys(j).conicArgAlmap(i))
        }
        acc
      },
      //conicVarAlmap
      {
        var acc: Almap = conicVarAlmap
        for(j <- 0 until argSize.length) {
          acc = Almap.diagCat(acc, ys(j).conicVarAlmap)
        }
        acc
      },
      //conicOffset
      {
        implicit val avlav = AVectorLikeAVector(arity)
        var acc: AVector = conicOffset
        for(j <- 0 until argSize.length) {
          acc = acc + conicArgAlmap(j) * ys(j).conicOffset
        }
        for(j <- 0 until argSize.length) {
          acc = acc ++ ys(j).conicOffset
        }
        acc
      },
      //conicCone
      {
        var acc: Cone = conicCone
        for(j <- 0 until argSize.length) {
          acc = ConeProduct(acc, ys(j).conicCone)
        }
        acc
      }
    )
  }

  //transforms the function by minimizing over the last argument
  def minimize_over_lastarg: Function = {
    if(argSize.length < 1) throw new IRValidationException()
    val sgnvn = (for(i <- 0 until argSize.length - 1) yield SignumPoly.param(i, argSize.length - 1)) :+ SignumPoly.const(Signum.All, argSize.length - 1)
    Function(
      // argSize
      argSize.dropRight(1),
      // sign
      sign.evalpoly(sgnvn(0).arity, sgnvn),
      // tonicity (minimizing componentwise destroys tonicity information)
      for(i <- 0 until argSize.length-1) yield SignumPoly.const(Signum.All, argSize.length - 1), //tonicity(i).evalpoly(sgnvn),
      // vexity
      vexity.evalpoly(sgnvn(0).arity, sgnvn) + SignumPoly.const(Signum.Negative, argSize.length - 1),
      // varSize
      varSize + argSize.last,
      // valueArgAlmap
      valueArgAlmap.dropRight(1),
      // valueVarAlmap
      AlmapHCat(valueVarAlmap, valueArgAlmap.last),
      // valueOffset
      valueOffset,
      // affineArgAlmap
      affineArgAlmap.dropRight(1),
      // affineVarAlmap
      AlmapHCat(affineVarAlmap, affineArgAlmap.last),
      // affineOffset
      affineOffset,
      // conicArgAlmap
      conicArgAlmap.dropRight(1),
      // conicVarAlmap
      AlmapHCat(conicVarAlmap, conicArgAlmap.last),
      // conicOffset
      conicOffset,
      // conicCone
      conicCone)
  }

  def maximize_over_lastarg: Function = -((-this).minimize_over_lastarg)

  // change the DCP properties of this function
  def chdcp(new_sign: SignumPoly, new_tonicity: Seq[SignumPoly], new_vexity: SignumPoly): Function = Function(
    argSize,
    new_sign,
    new_tonicity,
    new_vexity,
    varSize,
    valueArgAlmap,
    valueVarAlmap,
    valueOffset,
    affineArgAlmap,
    affineVarAlmap,
    affineOffset,
    conicArgAlmap,
    conicVarAlmap,
    conicOffset,
    conicCone)
}

  /*
  case class FunDesc(val va: Signum, val sg: Signum, val name: String)
  case class ArgDesc(val t: Signum, val nt: Signum, val sh: Shape, val name: String)
  
  case class FunBuilder0(fd: FunDesc) {
    def arg(
      tonicity: Signum = Tonicity.none, 
      shape: Shape = ShapeScalar(), 
      niltonicity: Signum = Tonicity.none, 
      name: String = ""): FunBuilder1
    = FunBuilder1(fd, ArgDesc(tonicity, niltonicity, shape, name))
  }
  
  case class FunBuilder1(fd: FunDesc, ad0: ArgDesc) {
    def arg(
      tonicity: Signum = Tonicity.none, 
      shape: Shape = ShapeScalar(), 
      niltonicity: Signum = Tonicity.none, 
      name: String = ""): FunBuilder2
    = FunBuilder2(fd, ad0, ArgDesc(tonicity, niltonicity, shape, name))
    
    def body(fx: (Expr) => Expr) = DcpFun1(fd, ad0, fx)
  }
  
  case class FunBuilder2(fd: FunDesc, ad0: ArgDesc, ad1: ArgDesc) {
    def arg(
      tonicity: Signum = Tonicity.none, 
      shape: Shape = ShapeScalar(), 
      niltonicity: Signum = Tonicity.none,
      name: String = ""): FunBuilder3
    = FunBuilder3(fd, ad0, ad1, ArgDesc(tonicity, niltonicity, shape, name))
    
    def body(fx: (Expr,Expr) => Expr) = DcpFun2(fd, ad0, ad1, fx)
  }
  
  case class FunBuilder3(fd: FunDesc, ad0: ArgDesc, ad1: ArgDesc, ad2: ArgDesc) {
    def body(fx: (Expr,Expr,Expr) => Expr) = DcpFun3(fd, ad0, ad1, ad2, fx)
  }
  
  case class DcpFun1(fd: FunDesc, ad0: ArgDesc, fx: (Expr) => Expr) {
    def apply(a0: Expr): Expr = dcpfun1apply(fd, ad0, fx, a0)
  }
  
  case class DcpFun2(fd: FunDesc, ad0: ArgDesc, ad1: ArgDesc, fx: (Expr,Expr) => Expr) {
    def apply(a0: Expr, a1: Expr): Expr = dcpfun2apply(fd, ad0, ad1, fx, a0, a1)
  }
  
  case class DcpFun3(fd: FunDesc, ad0: ArgDesc, ad1: ArgDesc, ad2: ArgDesc, fx: (Expr,Expr,Expr) => Expr) {
    def apply(a0: Expr, a1: Expr, a2: Expr): Expr = dcpfun3apply(fd, ad0, ad1, ad2, fx, a0, a1, a2)
  }
  
  def dcpfun1apply(fd: FunDesc, ad0: ArgDesc, fx: (Expr) => Expr, a0: Expr): Expr
  def dcpfun2apply(fd: FunDesc, ad0: ArgDesc, ad1: ArgDesc, fx: (Expr,Expr) => Expr, a0: Expr, a1: Expr): Expr
  def dcpfun3apply(fd: FunDesc, ad0: ArgDesc, ad1: ArgDesc, ad2: ArgDesc, fx: (Expr,Expr,Expr) => Expr, a0: Expr, a1: Expr, a2: Expr): Expr
  */