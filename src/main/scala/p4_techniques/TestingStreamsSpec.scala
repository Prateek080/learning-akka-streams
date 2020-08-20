package p4_techniques

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class TestingStreamsSpec extends  TestKit(ActorSystem("TestingStreamsSpec"))
with WordSpecLike
with BeforeAndAfterAll{

  implicit val materializer = ActorMaterializer()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A simple stream" should {
    "satisfy basic assertions" in {
          val simplSource = Source(1 to 10)
          val simpleSink = Sink.fold(0)((a : Int, b: Int) => a + b)

          val sumFuture = simplSource.runWith(simpleSink)
          val sum = Await.result(sumFuture, 2 seconds)
          assert(sum == 55)
    }

    "integrate with test actors via materialized values" in {
      import akka.pattern.pipe
      import system.dispatcher
      val simplSource = Source(1 to 10)
      val simpleSink = Sink.fold(0)((a : Int, b: Int) => a + b)

      val probe = TestProbe()
      simplSource.runWith(simpleSink).pipeTo(probe.ref)
      probe.expectMsg(55)
    }

    "integrate with test actors baed sink" in {
      val simplSource = Source(1 to 5)
      val simpleFlow = Flow[Int].scan[Int](0)(_ + _)
      val streamUnderTest = simplSource.via(simpleFlow)

      val probe = TestProbe()
      val probeSink = Sink.actorRef(probe.ref, "completionMessage")

      streamUnderTest.to(probeSink).run()
      probe.expectMsgAllOf(0, 1,3, 6, 10, 15)
    }

    "integrate with Streams Testkit sink" in {
      val scoreunderTest = Source(1 to 5).map(_ * 2)
      val testSink = TestSink.probe[Int]
      val materializedValue = scoreunderTest.runWith(testSink)

      materializedValue
        .request(5)
        .expectNext(2,4,6,8,10)
        .expectComplete()
    }
  }



}
