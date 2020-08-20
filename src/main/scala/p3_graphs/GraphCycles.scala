package p3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, UniformFanInShape}

object GraphCycles extends App {
  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))

    val incrementerShape = builder.add(Flow[Int].map{ x =>
      println(s"Acceleratin $x")
      x+1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                    mergeShape <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(accelerator).run()
  //cycleDeadlock

  /*
  Solution 1: MergePreferred
   */

  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))

    val incrementerShape = builder.add(Flow[Int].map{ x =>
      println(s"Acceleratin $x")
      x+1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape.preferred <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(actualAccelerator).run()


  /*
Solution 2: Buffers
 */

  val bufferedAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))

    val incrementerShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map{ x =>
      println(s"Acceleratin $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(bufferedAccelerator).run()

  /*
    Exercise: create a fan-in shape
    Two inputs which will be fed with Exactly ONE number (1 and 1)
    output will eimt an INFINITE FIBONACCI SEQUENCE based off these 2 numbers
*/

  val fibbonaciGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val zip = builder.add(Zip[Int,Int])
    val mergePreferred = builder.add(MergePreferred[(Int, Int)](1))
    val fibologic = builder.add(Flow[(Int,Int)].map{ pair =>
      val last = pair._1
      val previous = pair._2
      Thread.sleep(1000)
      (last+previous, last)
    })
    val bdcast = builder.add(Broadcast[(Int,Int)](2))
    val extractLast = builder.add(Flow[(Int,Int)].map(_._1))

   zip.out ~> mergePreferred ~> fibologic  ~> bdcast ~> extractLast
              mergePreferred.preferred                 <~ bdcast

    UniformFanInShape(extractLast.out, zip.in0, zip.in1)
  }

  val fiboGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

       val source1 = builder.add(Source.single[Int](1))
      val source2 = builder.add(Source.single[Int](1))
      val sink = builder.add(Sink.foreach[Int](println))
      val fibo = builder.add(fibbonaciGraph)

      source1 ~> fibo.in(0)
      source2 ~> fibo.in(1)
      fibo.out ~> sink

      ClosedShape
    }
  )

  fiboGraph.run()

}
