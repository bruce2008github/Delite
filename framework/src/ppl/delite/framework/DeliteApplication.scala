package ppl.delite.framework

import codegen.c.TargetC
import codegen.scala.TargetScala
import codegen.Target
import java.io.PrintWriter
import collection.mutable.{HashMap, ListBuffer}
import scala.virtualization.lms.common.{EffectExp, BaseExp}
import scala.virtualization.lms.common.embedded.scala.ScalaOpsPkgExp

trait DeliteApplication extends ScalaOpsPkgExp {

  type DeliteApplicationTarget = Target{val intermediate: DeliteApplication.this.type}

  var args: Rep[Array[String]] = _
  private var _targets: HashMap[String, DeliteApplicationTarget] = _

  // DeliteApplication should have a list of targets -- each target contains a single generator object that mixes in
  // all of the DSL generator classes

  final def main(args: Array[String]) {
    println("Delite Application Being Staged:[" + this.getClass.getSimpleName + "]")
    val main_m = {x: Rep[Array[String]] => liftedMain()}


    println("******Generating the program*********")
    for(e <- targets) {
      val tgt = e._2
      globalDefs = List()
      tgt.generator.emitSource(main_m, "Application", new PrintWriter(System.out))
    }

  }

  // temporary, for code gen: we need one copy of these globals shared between code generators and targets
  //var shallow = false

  var scope: List[TP[_]] = Nil


  def addTarget(tgt: DeliteApplicationTarget) = {
    targets += tgt.name -> tgt
    tgt
  }

  def targets : HashMap[String, DeliteApplicationTarget] = {
    if (_targets == null) {
      _targets = new HashMap[String, DeliteApplicationTarget]

      addTarget(new TargetScala{val intermediate: DeliteApplication.this.type = DeliteApplication.this})
      //addTarget(new TargetC{val intermediate: DeliteApplication.this.type = DeliteApplication.this})
    }
    _targets
  }

  def registerDSLType(name: String): DSLTypeRepresentation = nop

  /**
   * this is the entry method for our applications, user implement this method. Note, that it is missing the
   * args parameter, args are now accessed via the args field. This basically hides the notion of Reps from
   * user code
   */
  def main(): Unit

  def liftedMain(): Rep[Unit] = main


  //so that our main doesn't itself get lifted
  private def println(s:String) = System.out.println(s)

  private def nop = throw new RuntimeException("not implemented yet")
}
