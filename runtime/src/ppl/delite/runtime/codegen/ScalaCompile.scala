package ppl.delite.runtime.codegen

import scala.tools.nsc._
import scala.tools.nsc.io._
import scala.tools.nsc.util._
import collection.mutable.ArrayBuffer
import java.io.ByteArrayOutputStream
import ppl.delite.runtime.Config

object ScalaCompile extends CodeCache {

  private val classCacheHome = cacheHome + "classes" + File.separator

  private val sourceBuffer = new ArrayBuffer[String]

  def target = "scala"

  def addSource(source: String) {
    if (!sourceBuffer.contains(source)) //avoid duplicate kernels //TODO: there must be a better way
      sourceBuffer += source
  }

  def compile: ClassLoader = {
    cacheRuntimeSources(sourceBuffer.toArray)
    sourceBuffer.clear()

    for (module <- modules if (module.needsCompile)) {
      val sources = Directory(Path(sourceCacheHome + module.name)).deepFiles.filter(_.extension == target).map(_.path).toArray
      val classes = module.deps.map(d => Path(classCacheHome + d.name).path).toArray
      compile(classCacheHome + module.name, sources, classes)
    }

    ScalaClassLoader.fromURLs(modules.map(m => Path(classCacheHome + m.name).toURL), this.getClass.getClassLoader)
  }

  def compile(destination: String, sources: Array[String], classPaths: Array[String]) {
    Directory(Path(destination)).createDirectory()

    val currentCp = this.getClass.getClassLoader match {
      case ctx: java.net.URLClassLoader => ctx.getURLs.map(_.getPath).mkString(File.pathSeparator)
      case _ => System.getProperty("java.class.path")
    }
    val cp = currentCp + File.pathSeparator + classPaths.mkString(File.pathSeparator)

    val bcp = Predef.getClass.getClassLoader match {
      case ctx: java.net.URLClassLoader => ctx.getURLs.map(_.getPath).mkString(File.pathSeparator)
      case _ => System.getProperty("sun.boot.class.path")
    }

    val args = Array("-nowarn", "-d", destination, "-classpath", cp, "-bootclasspath", bcp) ++ sources
    def compile() = { Main.process(args); Main.reporter.hasErrors } //scalac
    //def compile() = CompileClient.main0(args) != 0 //fsc
    val dummyStream = new ByteArrayOutputStream

    //this suppresses strange spurious compiler errors generated by fsc
    //if (Console.withOut(dummyStream)(compile())) { //try twice, suppress output on first
      if (compile()) {
        Directory(Path(Config.codeCacheHome)).deleteRecursively() //something's wrong, clear the cache
        error("Compilation failed")
      }
    //}
  }

  def printSources() {
    for (i <- 0 until sourceBuffer.length) {
      print(sourceBuffer(i))
      print("\n /*********/ \n \n")
    }
  }

}
