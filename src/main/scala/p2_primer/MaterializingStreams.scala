package p2_primer

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future

object MaterializingStreams extends App{

  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()
  val simpleGraph  = Source(1 to 10).to(Sink.foreach[Int](println))

  val source  = Source(1 to 10)
  val sink = Sink.reduce[Int]((a,b) => a + b)

//  val someFuture = source.runWith(sink);
//
//  someFuture.onComplete{
//    case Success(value) => println(s"value $value")
//    case Failure(ex) => println(s"ex $ex")
//  }



  //choosing materialized value
  val matSource  = Source(1 to 10)
  val matFlow = Flow[Int].map(x => x+1)
  val matSink = Sink.foreach[Int](println)
  val graph: RunnableGraph[Future[Done]] = matSource.viaMat(matFlow)(Keep.right).toMat(matSink)(Keep.right)

//  graph.run().onComplete({
//    case Success(_) => println("Finished")
//    case Failure(ex) => println(s"Finished with Exception : ${ex}")
//  })

  //Sugards
//  Source(1 to 10).runWith(Sink.reduce(_ + _))
//  Source(1 to 10).runReduce(_ + _)

//  Flow[Int].map(x => 2*x).runWith(matSource, matSink)

  val exSource = Source(1 to 10).toMat(Sink.last)(Keep.right).run()

  val sentenceSource = Source(List(
    "Akka is good",
    "i love anime",
    "you want to live"
  ))

  val wordCountSink = Sink.fold[Int, String](0)((cur, sentence) => cur + sentence.split(" ").length)
  val g1 = sentenceSource.runWith(wordCountSink)

  val wordCountFlow = Flow[String].fold[Int](0)((cur, sentence) => cur + sentence.split(" ").length)
  val g2 = sentenceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run();
  val g3 = sentenceSource.via(wordCountFlow).runWith(Sink.head)
  val g4 = wordCountFlow.runWith(sentenceSource, Sink.head)._2

}
