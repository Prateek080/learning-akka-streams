package p2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackPressure extends App {

  implicit val system = ActorSystem("BackPressure")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int]{ x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  val simpleFlow = Flow[Int].map{ x =>
    println(s"Incoming: $x")
    x+1
  }

//  fastSource.to(slowSink).run() //fusing?

//  fastSource.async
//      .via(simpleFlow).async
//      .to(slowSink)
//      .run()

  /*
  Reactions to Backpressure (in order)
  - try to slow down if possible
  - buffer elements-
  - drop down elements from the buffer if it overflows
  - tear down/kill the whole stream
   */

  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropTail)
  fastSource.async
    .via(bufferedFlow).async
    .to(slowSink)
    .run()
}
