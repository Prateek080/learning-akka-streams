package p5_advancedAkka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object Substreams extends App {
  implicit val system = ActorSystem("Substreams")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Example 1 - Grouping a stream by certain function

  val wordSource = Source(List("Akka","is","a", "superb","framework"))
  val groups = wordSource.groupBy(30, word => if(word.isEmpty) '\0' else word.toLowerCase().charAt(0))

//  groups.to(Sink.fold(0)((count, word) => {
//        val newCount = count + 1
//        println(s"I just received $word, count is $newCount")
//        newCount
//  })).run()

  // Example 2 - Merge Substreams Back
  val textSource = Source(List(
      "This is amazing",
    "Akka streams are good",
    "Learning curve is steep"
  ))

  val totalCharCountFuture = textSource
    .groupBy(2, string => string.length % 2)
    .map(_.length) //do your expensive computations
    .mergeSubstreamsWithParallelism(2)
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

//  totalCharCountFuture.onComplete{
//    case Success(value) => println(s"Total char count: $value")
//    case Failure(ex) => println(s"Error: $ex")
//  }


  // Example 4 - Splittin a substream
  val text =   "This is amazing\n" +
  "Akka streams are good\n" +
  "Learning curve is steep\n"

  val anotherCharCountFuture = Source(text.toList)
    .splitWhen(c => c == '\n')
    .filter(_ != "'\n")
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  anotherCharCountFuture.onComplete{
    case Success(value) => println(s"Total char count: $value")
    case Failure(ex) => println(s"Error: $ex")
  }


  // Example 5 - flattening
  val simpleSource = Source(1 to 5)
//  simpleSource.flatMapConcat(x => Source(x to ( 3 * x ))).runWith(Sink.foreach(println))
  simpleSource.flatMapMerge(2, x => Source(x to ( 3 * x ))).runWith(Sink.foreach(println))


}
