package p3_graphs

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {

  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "done", "rock"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
   Composite component(sink)
   - print out all the strings which are lowercase
   - Counts the strings that are short ( < 5 char)
   */

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter) ((printerMatValue, counterMatValue) => counterMatValue){ implicit builder =>
      (printerShape, counterShape) =>
        import GraphDSL.Implicits._

        val bdcast = builder.add(Broadcast[String](2))
        val lowercaseFilter = builder.add(Flow[String].filter(str => str == str.toLowerCase))
        val shortStringFilter = builder.add(Flow[String].filter(str => str.length < 5))

        bdcast ~> lowercaseFilter ~> printerShape
        bdcast ~> shortStringFilter ~> counterShape

        SinkShape(bdcast.in)
    }
  )

  import system.dispatcher
  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
  shortStringCountFuture.onComplete{
    case Success(count) => println(s" Strings $count")
    case Failure(ex) => println(s"Failed count of Short Strings $ex")
  }

  /*
  Exercise
  hint -> broadcase, fold
   */

  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {

    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)

    Flow.fromGraph(
      GraphDSL.create(counterSink) { implicit builder =>
        counterSinkShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShap = builder.add(flow)

          originalFlowShap ~> broadcast ~> counterSinkShape

          FlowShape(originalFlowShap.in, broadcast.out(1))
      }
    )
  }


    val simpleSource = Source(1 to 42)
    val simpleFlow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x)
    val simpleSink: Sink[Any, Future[Done]] = Sink.ignore

    val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).to(simpleSink).run()

  enhancedFlowCountFuture.onComplete{
      case Success(count) => println(s" Elements $count")
      case Failure(ex) => println(s"Failed Eleements $ex")
    }




}
