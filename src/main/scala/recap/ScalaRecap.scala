package scala

import scala.concurrent.Future
import scala.util.{Failure, Success}
object ScalaRecap extends App{

  val aCondition: Boolean = false;
  def func(x: Int) = {
    if(x == 5) 50 else 100
  }

  //OO features

  class Fruit
  trait Juices {
    def color(a: Fruit): Unit
  }

  //generics
  abstract  class MyList[+A]

  //method notations
  1 + 2   //infix notation
  1.+(2)

  //FP
  val inc: Int => Int = (x: Int) => x + 1
  inc(1)

  List(1,2,3).map(inc)
  //HOF - take other functions as arguements: flatMap, Map
  //Monad: Option, Try

  //Pattern Matching
  val unknown: Any = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  try{
    throw  new RuntimeException
  }
  catch{
    case e: Exception => println("caught")
  }

  /*
  Scala Advanced
   */

  //multithreading
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future {
    42
  }

  future.onComplete({
    case Success(value) => println("Success case")
    case Failure(exception) => println("Failure case")
  })

  // Based on Patter matching
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 65
    case _ => 999
  }

  //type alisases
  type AkkaReceive = PartialFunction[Any, Unit]
  def receive: AkkaReceive = {
    case 1 => println("hello")
    case 2 => println("Hi")
  }

  //Implicits
  implicit  val timeout = 3000
  def setTimeout(f: () => Unit)(implicit  timeout: Int) = f()

  setTimeout(() => println("timeout"))
}
