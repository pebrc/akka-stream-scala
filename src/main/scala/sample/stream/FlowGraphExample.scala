package sample.stream

import akka.actor.ActorSystem
import akka.stream.MaterializerSettings
import akka.stream.scaladsl2._
import FlowGraphImplicits._
import scala.concurrent.ExecutionContext.Implicits.global

object FlowGraphExample {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")

    val settings = MaterializerSettings(system)
    implicit val mat = FlowMaterializer(settings)

    val text =
      """|Lorem Ipsum is simply dummy text of the printing and typesetting industry.
         |Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, 
         |when an unknown printer took a galley of type and scrambled it to make a type 
         |specimen book.""".stripMargin

    val count = FlowFrom[String].map(_.length)
    val toUpper = FlowFrom[String].map(_.toUpperCase)

    val in = IterableSource[String](text.split("\\s").toVector)
    val out = ForeachSink[(Int, String)](println)


    /**
     *                           +--------+
                                 |        |
                     +---------> |  upper +-------+
                     |           |        |       |
+--------+      +----+--+        +--------+    +--+----+        +-------+
|        |      |       |                      |       |        |       |
|  in    +----> | bcast |                      |  zip  | +----> |  out  |
|        |      |       |                      |       |        |       |
+--------+      +---+---+                      +---+---+        +-------+
                    |            +--------+        |
                    |            |        |        |
                    +----------> |  count +--------+
                                 |        |
                                 +--------+

     */

    val g = FlowGraph { implicit b =>
      val bcast = Broadcast[String]
      val zip = Zip[Int, String]

      in ~> bcast ~> count ~> zip.left
      bcast ~> toUpper ~> zip.right
      zip.out ~> out
    }.run()

    out.future(g).onComplete(t => system.shutdown())

  }
}
