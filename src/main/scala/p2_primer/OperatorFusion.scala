package p2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {

  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach(println)

  //Runs on same actor. Also called as operator/component Fusion
//  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()

  //Good when quick operations. Bad if slow operations

  val complexFlow = Flow[Int].map{ x =>
    Thread.sleep(1000)
    x + 1
  }

  val complexFlow2 = Flow[Int].map{ x =>
    Thread.sleep(1000)
    x * 10
  }

//==  simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()

  //Async Boundary
//  simpleSource.via(complexFlow).async //runs on one actor
//    .via(complexFlow2).async //runs on another actor
//    .to(simpleSink).run() //runs on third actor

  //Ordering Guranatees
  Source(1 to 3)
    .map(element => {println(s"Flow A: $element"); element}).async
    .map(element => {println(s"Flow B: $element"); element}).async
    .map(element => {println(s"Flow C: $element"); element}).async
    .runWith(Sink.ignore)

}
