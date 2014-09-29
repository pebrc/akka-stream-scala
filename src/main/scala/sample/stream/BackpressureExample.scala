package sample.stream

import akka.actor.Actor.Receive
import akka.actor.{ Actor, Props, ActorSystem }
import akka.stream.MaterializerSettings
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ OneByOneRequestStrategy, RequestStrategy, ActorSubscriber }
import akka.stream.scaladsl2.{ TickSource, FlowFrom, ForeachSink, FlowMaterializer }
import scala.concurrent.duration._

/**
 * Created by p_brc on 29/09/2014.
 */
object BackpressureExample {

  case object Ping

  class PingActor extends Actor with ActorSubscriber {
    override def receive = {
      case Ping =>
        println("pong")
        Thread sleep 1 //don't do this
      case OnNext(p: Ping.type) =>
        println("pong")
        Thread sleep 1 //don't do this

    }

    override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("backpressure")

    val client = system.actorOf(Props[PingActor], "Ping")

    if (!args.isEmpty && args(0) == "nopressure") {
      Thread sleep 5000
      while (true) {
        println("ping")
        client ! Ping
      }

    } else {
      val settings = MaterializerSettings(system)
      implicit val mat = FlowMaterializer(settings)

      val flow = FlowFrom[Ping.type].withSource(TickSource(1.second, 1.milli, () => { println("ping"); Ping })).publishTo(ActorSubscriber(client))

    }

  }

}
