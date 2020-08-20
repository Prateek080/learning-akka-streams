package p3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import akka.stream.{ActorMaterializer, FlowShape}

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer()

  val firstSource = Source(1 to 10)
  val secondSource = Source(400 to 1000)

//  val sourceGraph = Source.fromGraph(
//    GraphDSL.create(){  implicit  builder =>
//      import GraphDSL.Implicits._
//
//      val concat = builder.add(Concat[Int](2))
//
//      firstSource ~> concat
//      secondSource ~> concat
//
//      SourceShape(concat.out)
//    }
//  )
//
//  sourceGraph.to(Sink.foreach(println)).run()

  // Complex Sink

//  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1 ${x}"))
//  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2 ${x}"))
//
//  val sinkGraph = Sink.fromGraph(
//    GraphDSL.create(){implicit builder =>
//      import GraphDSL.Implicits._
//
//      val bdcast = builder.add(Broadcast[Int](2))
//
//      bdcast ~> sink1
//      bdcast ~> sink2
//
//      SinkShape(bdcast.in)
//
//    }
//  )
//
//  firstSource.to(sinkGraph).run()


    /*
    Flow composed of two other flows,
    one that adds 1 to a numbner
    one that does number * 10
     */

  val exSource = Source(1 to 10)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)
  val flowGraph = Flow.fromGraph(
      GraphDSL.create(){implicit builder =>
        import GraphDSL.Implicits._

        val incrementerShape = builder.add(incrementer)
        val multiplierShape = builder.add(multiplier)

        incrementerShape ~> multiplierShape
        FlowShape(incrementerShape.in, multiplierShape.out)

      }
    )

 exSource.via(flowGraph).runForeach(println)

  /*
  Exercise - Flow from a sink and a source
   */
}
