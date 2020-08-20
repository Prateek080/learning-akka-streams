package p5_advancedAkka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{ActorMaterializer, Attributes, Inlet, Outlet, SinkShape, SourceShape}

import scala.collection.mutable
import scala.util.Random

object CustomOperator extends App {
  implicit val system = ActorSystem("CustomOperator")
  implicit val materializer = ActorMaterializer()

  // Ex -1 A custom Source which emits Random numbers until cancelled

  class RandomGenerator(max: Int) extends  GraphStage[/*step 0 define the shape*/SourceShape[Int]]{

    // step 1 : define the ports and component specific members
    val outPort = Outlet[Int]("RandomGenerator")
    val random = new Random()

    // step 2: construct a new shape
    override def shape: SourceShape[Int] = SourceShape(outPort)

    // step 3: create the logic
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
     // step 4: define mutable state and implement logic here
      setHandler(outPort, new OutHandler {
        // when there is demand from downstream
        override def onPull(): Unit = {
            //emit new element
          val nextNumber = random.nextInt(max)
          push(outPort, nextNumber)
        }
      })
    }
  }

  val randomGeneratorSource = Source.fromGraph(new RandomGenerator(100))
//  randomGeneratorSource.runWith(Sink.foreach(println))


  // A custom Sink that print elements in batches of give size

  class Batcher(batchSize: Int) extends GraphStage[SinkShape[Int]]{

    val inPort = Inlet[Int]("batcher")

    override def shape: SinkShape[Int] = SinkShape[Int](inPort)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      //mutable state
      val batch = new mutable.Queue[Int]

      override def preStart(): Unit = {
        pull(inPort)
      }

      setHandler(inPort, new InHandler {
        // when upstream wants to send element
        override def onPush(): Unit = {
          val nextElement = grab(inPort)
          batch.enqueue(nextElement)
          //Assume Complex Computation
          Thread.sleep(1000)
          if(batch.size >= batchSize){
            println("New Batch: " + batch.dequeueAll(_ => true).mkString("[",",","]"))
          }
          pull(inPort)
        }

        override def onUpstreamFinish(): Unit = {
          if(batch.nonEmpty){
            println("New Batch: " + batch.dequeueAll(_ => true).mkString("[",",","]"))
            println("Stream Finished")
          }
        }
      })
    }

  }

  val batcherSink = Sink.fromGraph(new Batcher(10))

  randomGeneratorSource.to(batcherSink).run()
}
