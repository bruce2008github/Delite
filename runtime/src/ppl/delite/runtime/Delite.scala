package ppl.delite.runtime

import codegen._
import executor._
import graph.ops.{EOP, Arguments}
import graph.{TestGraph, DeliteTaskGraph}
import profiler.PerformanceTimer
import scheduler._
import tools.nsc.io._

/**
 * Author: Kevin J. Brown
 * Date: Oct 11, 2010
 * Time: 5:02:38 PM
 *
 * Pervasive Parallelism Laboratory (PPL)
 * Stanford University
 */

object Delite {

  private val mainThread = Thread.currentThread

  private def printArgs(args: Array[String]) {
    if(args.length == 0) {
      println("Not enough arguments.\nUsage: [Launch Runtime Command] filename.deg arguments*")
      exit(-1)
    }
    println("Delite Runtime executing with the following arguments:")
    println(args.mkString(","))
  }

  private def printConfig() {
    println("Delite Runtime executing with " + Config.numThreads + " CPU thread(s) and " + Config.numGPUs + " GPU(s)")
  }

  def main(args: Array[String]) {
    printArgs(args)

    printConfig()

    //extract application arguments
    Arguments.args = args.drop(1)

    val scheduler = Config.scheduler match {
      case "SMPStaticScheduler" => new SMPStaticScheduler
      case "GPUOnlyStaticScheduler" => new GPUOnlyStaticScheduler
      case "default" => {
        if (Config.numGPUs == 0) new SMPStaticScheduler
        else if (Config.numThreads == 1 && Config.numGPUs == 1) new GPUOnlyStaticScheduler
        else error("No scheduler currently exists that can handle requested resources")
      }
      case _ => throw new IllegalArgumentException("Requested scheduler is not recognized")
    }

    val executor = Config.executor match {
      case "SMPExecutor" => new SMPExecutor
      case "SMP+GPUExecutor" => new SMP_GPU_Executor
      case "default" => {
        if (Config.numGPUs == 0) new SMPExecutor
        else new SMP_GPU_Executor
      }
      case _ => throw new IllegalArgumentException("Requested executor type is not recognized")
    }

    try {

      executor.init() //call this first because could take a while and can be done in parallel

      //load task graph
      val graph = loadDeliteDEG(args(0))
      //val graph = new TestGraph

      //load kernels & data structures
      loadSources(graph)

      //schedule
      scheduler.schedule(graph)

      //compile
      val executable = Compilers.compileSchedule(graph)

      //execute
      val numTimes = Config.numRuns
      for (i <- 1 to numTimes) {
        println("Beginning Execution Run " + i)
        PerformanceTimer.start("all", false)
        executor.run(executable)
        EOP.await //await the end of the application program
        PerformanceTimer.stop("all", false)
        PerformanceTimer.print("all")
        // check if we are timing another component
        if(Config.dumpStatsComponent != "all")
          PerformanceTimer.print(Config.dumpStatsComponent)
      }

      if(Config.dumpStats)
        PerformanceTimer.dumpStats()

      executor.shutdown()
    }
    catch { case e => {
      executor.abnormalShutdown()
      //clear code cache in this case
      Directory(Path(Config.codeCacheHome)).deleteRecursively
      throw e
    } }
  }

  def loadDeliteDEG(filename: String) = {
    val deg = Path(filename)
    if (!deg.isFile)
      error(filename + " does not exist")
    DeliteTaskGraph(deg.jfile)
  }

  def loadSources(graph: DeliteTaskGraph) {
    ScalaCompile.cacheDegSources(Directory(Path(graph.kernelPath + File.separator + ScalaCompile.target + File.separator).toAbsolute))
    //required files may not be present if no gpu enabled
    if(Config.numGPUs > 0)
      CudaCompile.cacheDegSources(Directory(Path(graph.kernelPath + File.separator + CudaCompile.target + File.separator).toAbsolute))
  }

  //abnormal shutdown
  def shutdown() {
    mainThread.interrupt()
  }

}
