package p5_advancedAkka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}

import scala.concurrent.duration._

object DynamicStream extends App {
  implicit val system = ActorSystem("DynamicStream")
  implicit val materializer = ActorMaterializer()
  /*
      1.  Kill Switch
   */

  // Single Kill Switch
  val killSwitchFlow = KillSwitches.single[Int]
  val counter = Source(Stream.from(1)).throttle(1, 1 second).log("counter")
  val sink = Sink.ignore

//  val killSwitch = counter
//    .viaMat(killSwitchFlow)(Keep.right)
//    .to(sink)
//    .run()
//
//  system.scheduler.scheduleOnce(3 seconds){
//    killSwitch.shutdown()
//  }

  // Shared Kill Switch
  val sharedKillSwitch = KillSwitches.shared("PowerButton")
  val anotherCounter = Source(Stream.from(1)).throttle(2, 1 second).log("AnotherCounter")

//  counter.via(sharedKillSwitch.flow).runWith(sink)
//  anotherCounter.via(sharedKillSwitch.flow).runWith(sink)
//
//  system.scheduler.scheduleOnce(3 seconds){
//    sharedKillSwitch.shutdown()
//  }

  /*
     2.  MergeHub
  */

  val dynamicMerge = MergeHub.source[Int]
  val materializedSink = dynamicMerge.to(Sink.foreach[Int](println)).run()

//  Source(1 to 10).runWith(materializedSink)
//  counter.runWith(materializedSink)

  /*
   3.  BroadcastHub
*/
  val dynamicBroadcast = BroadcastHub.sink[Int]
  val materializedSource = Source(1 to 100).runWith(dynamicBroadcast)
//
//  materializedSource.runWith(Sink.ignore)

  /*
  Ex - Combine a mergeHub and a broadcastHub

  A publisher - subscriber component
   */

  val merge = MergeHub.source[String]
  val bdcast = BroadcastHub.sink[String]

  val (publisherPort, subscriberPort) = merge.toMat(bdcast)(Keep.both).run()

  subscriberPort.runWith(Sink.foreach(e => println(s"I received: $e")))
  subscriberPort.map(str => str.length).runWith(Sink.foreach(e => println(s"I got numner: $e")))

  Source(List("he","is", "good")).runWith(publisherPort)
  Source(List("she","will", "talk")).runWith(publisherPort)
  Source.single("Streeeaam").runWith(publisherPort)
}
