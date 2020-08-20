package p4_techniques


import java.util.Date

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

object IntegrateWithExternalServices extends App {
  implicit val system = ActorSystem("IntegrateWithExternalServices")
  implicit val materializer = ActorMaterializer()
  implicit val dispatcer = system.dispatchers.lookup("dedicated-dispatcher")


  def genericExtService[A,B](element: A): Future[B] = ???

  //Implement a Pager Duty
  case class PagerEvent(app: String, desc: String, date: Date)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra","Infra Broker", new Date),
    PagerEvent("ContestJoin","Infra Broker", new Date),
    PagerEvent("ContestQueue","Infra Broker", new Date),
    PagerEvent("GateWayService","Not Routing Properly", new Date)
  ))

  object  PagerService{
    private val engineers = List("A","B","C","D")
    private val emails = Map(
      "A" -> "A EMAILID",
      "B" -> "B EMAILID",
      "C" -> "C EMAILID",
      "D" -> "D EMAILID"
    )

    def processEvent(pagerEvent: PagerEvent) = Future {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24*3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      println(s"Sending engineer $engineerEmail a high priority message: $pagerEvent")
      Thread.sleep(1000)

      engineerEmail
    }
  }

  val  infraEvents = eventSource.filter(_.app == "AkkaInfra")
  val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
  //MapAsync => guarantee the relative order of elemetns

  val pagedEmailSink = Sink.foreach[String](email => println(s"Mail send Successfully on $email"))

  pagedEngineerEmails.to(pagedEmailSink).run()

}
