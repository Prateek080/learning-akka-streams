package p3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}

object BidirectionFlows extends App{

  implicit val system = ActorSystem("BidirectionFlows")
  implicit val materializer = ActorMaterializer()

  /*
  Example: cryptography
   */

  def encrypt(n: Int)(string: String) = string.map(c => (c+n).toChar)
  def decrypt(n: Int)(string: String) = string.map(c => (c-n).toChar)

  //bidiFlow
  val bidiCryptoGraph = GraphDSL.create(){ implicit builder =>
    val encryptionFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptionFlowShape = builder.add(Flow[String].map(decrypt(3)))

    BidiShape.fromFlows(encryptionFlowShape, decryptionFlowShape)
  }

  val stringList = List("Akka", "is", "awesome", "done", "rock")
  val unencryptedSource = Source(stringList)
  val encryptedSource = Source(stringList.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder  =>
        import GraphDSL.Implicits. _

      val unencyptedSourceShape = builder.add(unencryptedSource)
      val encyptedSourceShape = builder.add(encryptedSource)
      val bidi = builder.add(bidiCryptoGraph)
      val encyptedSinkShape = builder.add(Sink.foreach[String](str => println(s"Encrypted: $str")))
      val decyptedSinkShape = builder.add(Sink.foreach[String](str => println(s"Decrypted: $str")))

      unencyptedSourceShape ~> bidi.in1 ; bidi.out1 ~> encyptedSinkShape
      decyptedSinkShape <~ bidi.out2; bidi.in2 <~ encyptedSourceShape

      ClosedShape
    }
  )

  cryptoBidiGraph.run()

}
