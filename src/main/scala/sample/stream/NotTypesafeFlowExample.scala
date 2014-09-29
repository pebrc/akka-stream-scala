package sample.stream

import akka.actor.ActorSystem
import akka.stream.MaterializerSettings
import akka.stream.scaladsl2._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created by p_brc on 28/09/2014.
 */
object NotTypesafeFlowExample {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("linear")

    // doubles as execution context for the futures...
    implicit val ec = system.dispatcher

    val settings = MaterializerSettings(system)
    implicit val mat = FlowMaterializer(settings)

    val sink = ForeachSink[String](i => println(s"i = $i"))

    val flow = FlowFrom[String].
      filter(_.length == 2).
      drop(2).
      withSink(sink)

    try {
      /*
       * this is a (already fixed) bug... but it illustrates nicely that actors are not type-safe
       * https://github.com/akka/akka/pull/15994/files
       */
      val breaks = flow.withSource(ThunkSource(() => Some("Text"))).run().getSinkFor(sink)
      Await.result(breaks, Duration.Inf)
    } finally {
      system.shutdown()
    }

  }

}
