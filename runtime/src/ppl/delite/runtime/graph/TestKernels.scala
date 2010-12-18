package ppl.delite.runtime.graph

/**
 * Author: Kevin J. Brown
 * Date: Nov 9, 2010
 * Time: 3:17:02 AM
 * 
 * Pervasive Parallelism Laboratory (PPL)
 * Stanford University
 */

object TestKernel1a {
  def apply() = println("op1a")
}

object TestKernel1b {
  def apply() = println("op1b")
}

object TestKernel1c {
  def apply() = println("op1c")
}

object TestKernel1d {
  def apply() = println("op1d")
}

object TestKernel2a {
  def apply() = println("op2a")
}

object TestKernel2b {
  def apply() = println("op2b")
}

object TestKernel2c {
  def apply() = println("op2c")
}

object TestKernel2d {
  def apply() = println("op2d")
}

object TestKernel3 {
  def apply() = println("op3")
}

object TestKernelBegin {
  def apply() = {
    val res = new ArrayColl[Int](10)
    for (i <- 0 until 10) res.dcUpdate(i, i)
    res
  }
}

object TestKernelPrint {
  def apply(result: Int) { println(result) }
}

object TestKernelEnd {
  def apply(out: ArrayColl[Int]) = {
    print("[ ")
    for (e <- out) print(e + " ")
    print("]\n")
  }
}

abstract class DeliteCollection[T] {
  def size: Int
  def dcApply(idx: Int): T
  def dcUpdate(idx: Int, x: T)
}

class ArrayColl[T: Manifest](val length: Int) extends DeliteCollection[T] {
  val _data = new Array[T](length)
  def foreach[U](f: T => U) = _data.foreach[U](f)
  def size = length
  def dcApply(idx: Int): T = _data(idx)
  def dcUpdate(idx: Int, x: T) { _data(idx) = x }
}

abstract class DeliteOPMap[A,B, CR <: DeliteCollection[B]] {
  def in: DeliteCollection[A]
  def out: CR
  def map(a: A): B
}

object TestKernelMap {
  def apply(in0: ArrayColl[Int], in1: ArrayColl[Int]): DeliteOPMap[Int,Int, ArrayColl[Int]] = {
    new DeliteOPMap[Int,Int, ArrayColl[Int]] {
      def in = in1
      def out = in0
      def map(a: Int) = a + 1
    }
  }
}

abstract class DeliteOPReduce[R] {
  def in: DeliteCollection[R]
  def reduce(r1: R, r2: R): R
}

object TestKernelReduce {
  def apply(in0: ArrayColl[Int]): DeliteOPReduce[Int] = {
    new DeliteOPReduce[Int] {
      def in = in0
      def reduce(r1: Int, r2: Int) = r1 + r2
    }
  }
}

abstract class DeliteOPZip[A,B,R, CR <: DeliteCollection[R]] {
  def inA: DeliteCollection[A]
  def inB: DeliteCollection[B]
  def out: CR
  def zip(a: A, b: B): R
}

object TestKernelZip {
  def apply(in0: ArrayColl[Int], in1: ArrayColl[Int], in2: ArrayColl[Int]): DeliteOPZip[Int,Int,Int, ArrayColl[Int]] = {
    new DeliteOPZip[Int,Int,Int, ArrayColl[Int]] {
      def inA = in1
      def inB = in2
      def out = in0
      def zip(a: Int, b: Int) = a + b
    }
  }
}

abstract class DeliteOPMapReduce[A,R] {
  def in: DeliteCollection[A]
  def map(elem: A): R
  def reduce(r1: R, r2: R): R
  def mapreduce(acc: R, elem: A) = reduce(acc, map(elem))
}

object TestKernelMapReduce {
  def apply(in0: ArrayColl[Int]): DeliteOPMapReduce[Int,Int] = {
    new DeliteOPMapReduce[Int,Int] {
      def in = in0
      def map(elem: Int): Int = elem * elem
      def reduce(acc: Int, elem: Int): Int = acc + elem
    }
  }
}