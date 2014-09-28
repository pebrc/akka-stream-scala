package sample.stream

import akka.actor.ActorSystem
import akka.stream.MaterializerSettings
import akka.stream.scaladsl2.{ IterableSource, ForeachSink, FlowFrom, FlowMaterializer }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

/**
 * Created by p_brc on 28/09/2014.
 */
object LinearFlowExample {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("linear")

    // doubles as execution context for the futures...
    implicit val ec = system.dispatcher

    val settings = MaterializerSettings(system)
    implicit val mat = FlowMaterializer(settings)

    val sink = ForeachSink[String](i => println(s"i = $i"))

    val flow = FlowFrom[Int].
      map(_.toString).
      filter(_.length == 2).
      drop(2).
      withSink(sink)

    //reuse flow definition with different sources (or sinks) here out[1-3]
    val out1 = flow.withSource(IterableSource(1 to 10)).run().getSinkFor(sink)

    val out2 = flow.withSource(IterableSource(1 to 100)).run().getSinkFor(sink)

    val out3 = flow.withSource(IterableSource(1 to 1000)).run().getSinkFor(sink)

    //clean up

    Await.result(Future.sequence(Seq(out1, out2, out3)), Duration.Inf)

    system.shutdown()

  }

}
