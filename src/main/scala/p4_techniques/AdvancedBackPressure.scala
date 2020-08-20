package p4_techniques

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

object AdvancedBackPressure extends App {
  implicit val system = ActorSystem("AdvancedBackPressure")
  implicit val materializer = ActorMaterializer()

  val controlledFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)

  case class PagerEvent(desc: String, date: Date, nInstances: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = Source(List(
    PagerEvent("400xx at Gateway", new Date),
    PagerEvent("Internal Server Error", new Date),
    PagerEvent("502 Bad Gateway", new Date),
    PagerEvent("Not Routing Properly", new Date)
  ))

  val oncallEngineer = "a@gmail.com"

  def sendEmail(notification: Notification) =
    println(s"Dear ${notification.email}, Please check event ${notification.pagerEvent.desc}")

  val notificationSink = Flow[PagerEvent].map(event => Notification(oncallEngineer, event))
    .to(Sink.foreach[Notification](sendEmail))

//  events.to(notificationSink).run()

  /*
  un-Backpressure source
  Slow Consumer
   */

  def sendEmailSlow(notification: Notification) = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email}, Please check event ${notification.pagerEvent.desc}")
  }

  val aggregateNotificationFlow = Flow[PagerEvent]
            .conflate((event1, event2) => {
              val nInstances = event1.nInstances + event2.nInstances
              PagerEvent(s"You have $nInstances events that required attention", new Date, nInstances)
            })
            .map(res => Notification(oncallEngineer, res))

  //Aggregation Alternative to BackPressure
//  events.via(aggregateNotificationFlow).async.to(Sink.foreach[Notification](sendEmailSlow)).run()

  /*
  Slow Producers: extrapolate and fast sink
   */

  import scala.concurrent.duration._
  val slowCounter =  Source(Stream.from(1)).throttle(1,1 second)
  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(elem => Iterator.continually(elem))
  slowCounter.via(extrapolator).to(hungrySink).run()

}
