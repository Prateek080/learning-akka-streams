package p5_advancedAkka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{ActorMaterializer, Attributes, FlowShape, Inlet, Outlet, SinkShape, SourceShape}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success}

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
          Thread.sleep(100)
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
//  randomGeneratorSource.to(batcherSink).run()


  /*
  Exercise Custom FLow - a simple filter flow
  - 2 ports : an input port and output port
   */
  class SimpleFilterFlow[T](predicate: T => Boolean) extends  GraphStage[FlowShape[T, T]]{

    val inPort = Inlet[T]("filterIn")
    val outPort = Outlet[T]("filterOut")

    // step 2: construct a new shape
    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      setHandler(outPort, new OutHandler {
        override def onPull(): Unit = pull(inPort)
      })

      setHandler(inPort, new InHandler {
        override def onPush(): Unit = {
          try{
            val nextElement = grab(inPort)
            if(predicate(nextElement)){
              push(outPort, nextElement) // pass it on
            }
            else {
              pull(inPort) // ask for another ekenebt
            }
          }
          catch {
            case e: Throwable => failStage(e)
          }
        }
      })
    }

  }

  val myFilter = Flow.fromGraph(new SimpleFilterFlow[Int](_ > 5))

//  randomGeneratorSource.via(myFilter).to(batcherSink).run()


  /*
  Materialized values in graph stages
  --- flow that count number of elements go through it
   */

  class CounterFlow[T] extends GraphStageWithMaterializedValue[FlowShape[T,T], Future[Int]] {

    val inPort = Inlet[T]("counterIn")
    val outPort = Outlet[T]("counterOut")

    override val shape = FlowShape(inPort, outPort)

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
      val promise = Promise[Int]
      val logic = new GraphStageLogic(shape) {
        var counter =0;
        //setting mutable state
        setHandler(outPort, new OutHandler {
          override def onPull(): Unit = pull(inPort)

          override def onDownstreamFinish(): Unit = {
            promise.success(counter)
            super.onDownstreamFinish()
          }
        })

        setHandler(inPort, new InHandler {
          override def onPush(): Unit = {
            val nextElement = grab(inPort)
            counter += 1
            push(outPort, nextElement)
          }

          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
        })
      }

      (logic, promise.future)
    }
  }

  import system.dispatcher
  val counterFlow = Flow.fromGraph(new CounterFlow[Int])
  Source(1 to 10)
    .viaMat(counterFlow)(Keep.right)
    .to(Sink.foreach(x => if(x==7) throw new RuntimeException("gotcha sink") else println(x)))
    .run()
    .onComplete{
      case Success(value) => println(s"Total count: $value")
      case Failure(ex) => println(s"Error: $ex")
    }

}
