package p3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)

  val output = Sink.foreach[(Int, Int)](println)

//  val graph = RunnableGraph.fromGraph(
//    //builder => MUTABLE DATA STRUCTURE
//    GraphDSL.create(){  implicit  builder: GraphDSL.Builder[NotUsed] =>
//      import GraphDSL.Implicits._
//      // IN the entire process till closeshape, builder is mutated.
//      // 2 - Add necessary components of this Graph
//      val bdcast = builder.add(Broadcast[Int](2)) // fan-out operator
//       val zip = builder.add(Zip[Int, Int])  //fan-in operator
//
//      // 3 - Tying up the components
//      input ~> bdcast
//
//      bdcast.out(0) ~> incrementer ~> zip.in0
//      bdcast.out(1) ~> multiplier ~> zip.in1
//
//      zip.out ~> output
//
//      // 4 - Closed Shape
//      ClosedShape // FREEZE the Builders Shape to create the graph
//    }
//  )
//  graph.run()



  // exercise 1 -> Source to two Sinks

//  val firstSink = Sink.foreach[Int](x => println(s"First Sink: $x"))
//  val secondSink = Sink.foreach[Int](x => println(s"Second Sink: $x"))
//
//  val sourcetoTwoSinksGraph = RunnableGraph.fromGraph(
//    GraphDSL.create(){  implicit  builder =>
//      import GraphDSL.Implicits._
//
//      val bdcast = builder.add(Broadcast[Int](2))
//      input ~> bdcast ~> firstSink
//               bdcast ~> secondSink
//
//      ClosedShape
//    }
//  )
//  sourcetoTwoSinksGraph.run()




  // exercise 2 -> Balance
  import scala.concurrent.duration._
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)
  val sink1 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 1 number of element: ${count}")
    count+1
  })
  val sink2 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 2 number of element: ${count}")
    count+1
  })

  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){  implicit  builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge
      balance ~> sink2

      ClosedShape
    }
  )

  balanceGraph.run()

}
