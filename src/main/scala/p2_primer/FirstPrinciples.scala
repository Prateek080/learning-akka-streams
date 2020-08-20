package p2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object FirstPrinciples extends  App{

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer()

//  val source  = Source(1 to 10)
//  val sink = Sink.foreach[Int](println)

//  val graph = source.to(sink).run()
//
//  val flow = Flow[Int].map(x => x + 1)
//  val sourceWithFlow = source.via(flow)
//  val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()
//  source.to(flowWithSink).run()
//  source.via(flow).to(sink).run()

  //Nulls are now allowed in reactive streams

  val names = List("Alice","Bob","Charlie","David","Martin","Scala")
  val nameSource = Source(names)
  val longNameFlow = Flow[String].filter(p => p.length > 5)
  val limitFlow = Flow[String].take(2)
  val nameSink = Sink.foreach[String](println)

  nameSource.via(longNameFlow).via(limitFlow).to(nameSink).run()
  nameSource.filter(_.length > 5).take(2).runForeach(println)
}
