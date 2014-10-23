/**
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.javadsl

import akka.stream.javadsl
import akka.stream.scaladsl2
import org.reactivestreams.{ Publisher, Subscriber }
import scaladsl2.FlowMaterializer

import scala.concurrent.Future

/** Java API */
object Sink {

  import akka.stream.scaladsl2.JavaConverters._

  /** Adapt [[scaladsl2.Sink]] for use within Java DSL */
  def adapt[O](sink: scaladsl2.Sink[O]): javadsl.Sink[O] =
    new Sink(sink)

  /**
   * A `Sink` that will invoke the given function for every received element, giving it its previous
   * output (or the given `zero` value) and the element as input.
   * The returned [[scala.concurrent.Future]] will be completed with value of the final
   * function evaluation when the input stream ends, or completed with `Failure`
   * if there is an error is signaled in the stream.
   */
  def fold[U, In](zero: U, f: japi.Function2[U, In, U]): javadsl.KeyedSink[In, Future[U]] =
    new KeyedSink(scaladsl2.Sink.fold[U, In](zero)(f.apply))

  /**
   * Helper to create [[Sink]] from `Subscriber`.
   */
  def create[In](subs: Subscriber[In]): Sink[In] =
    new Sink[In](scaladsl2.Sink(subs))

  /**
   * Creates a `Sink` by using an empty [[FlowGraphBuilder]] on a block that expects a [[FlowGraphBuilder]] and
   * returns the `UndefinedSource`.
   */
  def create[T]()(block: japi.Function[FlowGraphBuilder, UndefinedSource[T]]): Sink[T] =
    new Sink(scaladsl2.Sink.apply() { b ⇒ block.apply(b.asJava).asScala })

  /**
   * Creates a `Sink` by using a FlowGraphBuilder from this [[PartialFlowGraph]] on a block that expects
   * a [[FlowGraphBuilder]] and returns the `UndefinedSource`.
   */
  def create[T](graph: PartialFlowGraph, block: japi.Function[FlowGraphBuilder, UndefinedSource[T]]): Sink[T] =
    new Sink[T](scaladsl2.Sink.apply(graph.asScala) { b ⇒ block.apply(b.asJava).asScala })

  /**
   * A `Sink` that immediately cancels its upstream after materialization.
   */
  def cancelled[T]: Sink[T] =
    new Sink(scaladsl2.Sink.cancelled)

  /**
   * A `Sink` that will consume the stream and discard the elements.
   */
  def ignore[T](): Sink[T] =
    new Sink(scaladsl2.Sink.ignore)

  /**
   * A `Sink` that materializes into a [[org.reactivestreams.Publisher]].
   * that can handle one [[org.reactivestreams.Subscriber]].
   */
  def publisher[In](): KeyedSink[In, Publisher[In]] =
    new KeyedSink(scaladsl2.Sink.publisher)

  /**
   * A `Sink` that will invoke the given procedure for each received element. The sink is materialized
   * into a [[scala.concurrent.Future]] will be completed with `Success` when reaching the
   * normal end of the stream, or completed with `Failure` if there is an error is signaled in
   * the stream..
   */
  def foreach[T](f: japi.Procedure[T]): KeyedSink[T, Future[Unit]] =
    new KeyedSink(scaladsl2.Sink.foreach(f.apply))

  /**
   * A `Sink` that materializes into a [[org.reactivestreams.Publisher]]
   * that can handle more than one [[org.reactivestreams.Subscriber]].
   */
  def fanoutPublisher[T](initialBufferSize: Int, maximumBufferSize: Int): KeyedSink[Publisher[T], T] =
    new KeyedSink(scaladsl2.Sink.fanoutPublisher(initialBufferSize, maximumBufferSize))

  /**
   * A `Sink` that when the flow is completed, either through an error or normal
   * completion, apply the provided function with [[scala.util.Success]]
   * or [[scala.util.Failure]].
   */
  def onComplete[In](onComplete: japi.Procedure[Unit]): Sink[In] =
    new Sink(scaladsl2.Sink.onComplete[In](x ⇒ onComplete.apply(x)))

  /**
   * A `Sink` that materializes into a `Future` of the first value received.
   */
  def future[In]: KeyedSink[In, Future[In]] =
    new KeyedSink(scaladsl2.Sink.future[In])

  /**
   * A `Sink` that will invoke the given function for every received element, giving it its previous
   * output (or the given `zero` value) and the element as input.
   * The returned [[scala.concurrent.Future]] will be completed with value of the final
   * function evaluation when the input stream ends, or completed with `Failure`
   * if there is an error is signaled in the stream.
   */
  def fold[U, T](zero: U, f: Function[akka.japi.Pair[U, T], U]): KeyedSink[T, U] = {
    val sSink = scaladsl2.Sink.fold[U, T](zero) { case (a, b) ⇒ f.apply(akka.japi.Pair(a, b)) }
    new KeyedSink(sSink)
  }

}

/**
 * Java API
 *
 * A `Sink` is a set of stream processing steps that has one open input and an attached output.
 * Can be used as a `Subscriber`
 */
class Sink[-In](delegate: scaladsl2.Sink[In]) {

  /** Converts this Sink to it's Scala DSL counterpart */
  def asScala: scaladsl2.Sink[In] = delegate

  // RUN WITH //

  /**
   * Connect the `KeyedSource` to this `Flow` and then connect it to the `KeyedSource` and run it.
   * The returned tuple contains the materialized values of the `Source` and `Sink`, e.g. the `Subscriber` of a
   * [[akka.stream.scaladsl2.SubscriberSource]] and and `Publisher` of a [[akka.stream.scaladsl2.PublisherSink]].
   *
   * @tparam T materialized type of given Source
   */
  def runWith[T](source: javadsl.KeyedSource[In, T], materializer: FlowMaterializer): T =
    asScala.runWith(source.asScala)(materializer).asInstanceOf[T]

  /**
   * Connect this `Source` to a `Source` and run it. The returned value is the materialized value
   * of the `Sink`, e.g. the `Publisher` of a [[akka.stream.scaladsl2.PublisherSink]].
   */
  def runWith(source: javadsl.Source[In], materializer: FlowMaterializer): Unit =
    asScala.runWith(source.asScala)(materializer)
}

/**
 * Java API
 *
 * A `Sink` that will create an object during materialization that the user will need
 * to retrieve in order to access aspects of this sink (could be a completion Future
 * or a cancellation handle, etc.)
 */
final class KeyedSink[-In, M](delegate: scaladsl2.KeyedSink[In]) extends javadsl.Sink[In](delegate) {
  override def asScala: scaladsl2.KeyedSink[In] = super.asScala.asInstanceOf[scaladsl2.KeyedSink[In]]
}