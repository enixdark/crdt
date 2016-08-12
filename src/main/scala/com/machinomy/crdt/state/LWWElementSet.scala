package com.machinomy.crdt.state

import cats.kernel.Semilattice
import cats.syntax.all._

case class LWWElementSet[E, T: TombStone : Ordering, B: Bias](additions: GTSet[E, T], removals: GTSet[E, T])
  extends Convergent[E, Set[E]]{

  type Self = LWWElementSet[E, T, B]

  val tombStone = implicitly[TombStone[T]]

  def +(e: E) = copy(additions = additions + (e -> tombStone.next))

  def +(pair: (E, T)) = copy(additions = additions + pair)

  def -(e: E) = copy(removals = removals + (e -> tombStone.next))

  def -(pair: (E, T)) = copy(removals = removals + pair)

  override def value: Set[E] = {
    def fill(additions: Seq[(E, T)], removals: GTSet[E, T], result: Set[E] = Set.empty): Set[E] =
      if (additions.isEmpty) {
        result
      } else {
        val (element, additionT) = additions.head
        removals.state.get(element) match {
          case Some(removalT) =>
            val bias = implicitly[Bias[B]]
            bias.apply(element, additionT, removalT) match {
              case Some(e) =>
                fill(additions.tail, removals, result + element)
              case None =>
                fill(additions.tail, removals, result)
            }
          case None =>
            fill(additions.tail, removals, result + element)
        }
      }

    fill(additions.state.toSeq, removals)
  }
}

object LWWElementSet {
  def apply[E, T: TombStone : Ordering, B: Bias](): LWWElementSet[E, T, B] = new LWWElementSet[E, T, B](GTSet[E, T](), GTSet[E, T]())

  implicit def semilattice[E, T: TombStone : Ordering, B: Bias] = new Semilattice[LWWElementSet[E, T, B]] {
    override def combine(x: LWWElementSet[E, T, B], y: LWWElementSet[E, T, B]): LWWElementSet[E, T, B] = {
      val additions = y.additions |+| x.additions
      val removals = y.removals |+| x.removals
      LWWElementSet(additions, removals)
    }
  }
}
