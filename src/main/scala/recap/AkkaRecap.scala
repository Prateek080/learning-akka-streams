package recap

import akka.actor.{Actor, ActorSystem, Props}

class AkkaRecap extends App{

  class SimpleActor extends Actor{
    override def receive: Receive = {
      case message => println(s"Recieved $message")
    }
  }


  val system = ActorSystem("AkkaRecap")
  val actore = system.actorOf(Props[SimpleActor], "simpleActor")
}
