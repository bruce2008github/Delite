package ppl.dsl.optiql.datastruct.scala.container

class Grouping[TKey, TElement](val key: TKey, val elems: Iterable[TElement]) extends Iterable[TElement] {
  def iterator = elems.iterator
}