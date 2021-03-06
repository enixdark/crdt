/*
 * Copyright 2016 Machinomy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.machinomy.crdt.op

import com.machinomy.crdt.op.GraphLikeA._

/**
  * 2P2P-Graph
  */
case class TPTPGraph[A, V <: VertexLike[A], E <: EdgeLike[A, V]](va: Set[V], vr: Set[V], ea: Set[E], er: Set[E]) {
  type Self = TPTPGraph[A, V, E]

  def contains(v: V): Boolean = vertices.contains(v)

  def contains(e: E): Boolean = contains(e.u) && contains(e.v) && (ea -- er).contains(e)

  def add(v: V): TPTPGraph.UpdateResult[A, V, E] = {
    val nextVa = va + v
    val next: Self = copy(va = nextVa)
    (next, Some(TPTPGraph.AddVertex(v)))
  }

  def add(e: E): TPTPGraph.UpdateResult[A, V, E] = {
    if (contains(e.u) && contains(e.v)) {
      val nextEa = ea + e
      val next: Self = copy(ea = nextEa)
      (next, Some(TPTPGraph.AddEdge[A, V, E](e)))
    } else {
      (this, None)
    }
  }

  def isSingle(v: V): Boolean = (ea -- er).forall(e => e.u != v && e.v != v)

  def remove(v: V): TPTPGraph.UpdateResult[A, V, E] = {
    if (contains(v) && isSingle(v)) {
      val nextVr = vr + v
      val next: Self = copy(vr = nextVr)
      (next, Some(TPTPGraph.RemoveVertex[A, V](v)))
    } else {
      (this, None)
    }
  }

  def remove(e: E): TPTPGraph.UpdateResult[A, V, E] = {
    if (contains(e)) {
      val nextEr = er + e
      val next: Self = copy(er = nextEr)
      (next, Some(TPTPGraph.RemoveEdge[A, V, E](e)))
    } else {
      (this, None)
    }
  }

  def vertices: Set[V] = va -- vr

  def edges: Set[E] = ea -- er

  def run(operation: TPTPGraph.AddVertex[A, V]): TPTPGraph.UpdateResult[A, V, E] = add(operation.v)

  def run(operation: TPTPGraph.AddEdge[A, V, E]): TPTPGraph.UpdateResult[A, V, E] = add(operation.e)

  def run(operation: TPTPGraph.RemoveVertex[A, V]): TPTPGraph.UpdateResult[A, V, E] = remove(operation.v)

  def run(operation: TPTPGraph.RemoveEdge[A, V, E]): TPTPGraph.UpdateResult[A, V, E] = remove(operation.e)
}

object TPTPGraph {
  type UpdateResult[A, V <: VertexLike[A], E <: EdgeLike[A, V]] = (TPTPGraph[A, V, E], Option[TPTPGraph.Update[A]])

  def apply[A, V <: VertexLike[A], E <: EdgeLike[A, V]](): TPTPGraph[A, V, E] = {
    val va = Set.empty[V]
    val vr = Set.empty[V]
    val ea = Set.empty[E]
    val er = Set.empty[E]
    new TPTPGraph(va, vr, ea, er)
  }

  sealed trait Update[A]
  case class AddVertex[A, V <: VertexLike[A]](v: V) extends Update[A]
  case class AddEdge[A, V <: VertexLike[A], E <: EdgeLike[A, V]](e: E) extends Update[A]
  case class RemoveVertex[A, V <: VertexLike[A]](v: V) extends Update[A]
  case class RemoveEdge[A, V <: VertexLike[A], E <: EdgeLike[A, V]](e: E) extends Update[A]
}
