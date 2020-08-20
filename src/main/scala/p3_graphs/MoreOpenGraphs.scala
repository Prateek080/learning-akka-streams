package p3_graphs

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}

object MoreOpenGraphs extends App {


  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
  Max3 Operator - To find max of 3 elements
   */

  val max3StaticGraph = GraphDSL.create(){ implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int,Int,Int]((a,b) => Math.max(a,b)))
    val max2 = builder.add(ZipWith[Int,Int,Int]((a,b) => Math.max(a,b)))

    //Step 3
    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source(1 to 10).map(_ => 5)
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)

      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      max3Shape.out ~> maxSink

     ClosedShape
    }
  )
//
//  max3RunnableGraph.run()

  /*
  Non Uniform Fan out shape

  Processing bank Transactions
  Suspicious if amount > 10000

  Stream component for txns
  -output1: let the transaction go through
  -output2: suspicious txn ids.
   */

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("3243253245","A","B", 100, new Date),
    Transaction("3243254545","B","D", 10000, new Date),
    Transaction("3243223245","C","E", 100, new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txId => println(s"Suspicious transaction ID: $txId"))

  val suspiciousTxnStaticGraph = GraphDSL.create() {implicit  builder =>
    import GraphDSL.Implicits._

    val bdcast = builder.add(Broadcast[Transaction](2))
    val suspiciousFilter = builder.add(Flow[Transaction].filter(tx => tx.amount >= 10000))
    val txIDextractor = builder.add(Flow[Transaction].map[String](tx => tx.id))

    bdcast.out(0) ~> suspiciousFilter ~> txIDextractor

    new FanOutShape2(bdcast.in, bdcast.out(1), txIDextractor.out)
  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val suspicousTxnShape = builder.add(suspiciousTxnStaticGraph)

      transactionSource ~> suspicousTxnShape.in
      suspicousTxnShape.out0 ~> bankProcessor
      suspicousTxnShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTxnRunnableGraph.run()
}
