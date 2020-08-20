package p4_techniques

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.{ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl.{RestartSource, Sink, Source}

import scala.util.Random

object FaultTolerance extends App {
  implicit val system = ActorSystem("FaultTolerance")
  implicit val materializer = ActorMaterializer()

  /*
  1 - Logging --> Default log level info
   */
  val faultySource = Source(1 to 10).map(e => if(e == 6) throw new RuntimeException else e)
  faultySource.log("trackingElements")
      .to(Sink.ignore)
//      .run()

  /*
  2 Gracefully Terminating a stream
   */
  faultySource.recover{
    case _: RuntimeException => Int.MinValue
  }.log("gracefulSource")
    .to(Sink.ignore)
//    .run()

  /*
  3 Recover with another stream
   */
  faultySource.recoverWithRetries(3, {
    case _: RuntimeException => Source(90 to 99)
  })
    .log("RecoverWithRetries")
    .to(Sink.ignore)
   .run()

  /*
  4 Backoff Supervision - When actor fail, supervisor try to start with exponential delay
   */
  import scala.concurrent.duration._
  val restartSource = RestartSource.onFailuresWithBackoff(
    minBackoff = 1 second,
    maxBackoff = 30 seconds,
    randomFactor = 0.2
  )(() => {
    val randomNo = new Random().nextInt(20)
    Source(1 to 10).map(el => if(el == randomNo) throw new RuntimeException else el)
  })
    .log("RestartBackOff")
    .to(Sink.ignore)
//    .run()

  /*
    5 - Supervision Strategy
   */
  val numbers = Source(1 to 20).map(el => if(el == 13) throw new RuntimeException else el).log("supervision")
  val supervisedNumbers = numbers.withAttributes((ActorAttributes.supervisionStrategy{
    case _: RuntimeException => Resume
    case _ => Stop
  }))

  supervisedNumbers.to(Sink.ignore)
//    .run()
}
