import akka.actor.{Actor,ActorRef,ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.Source

// This is the first actor that reads the initial list file and each line of the text file is an element of the list.
class a1 extends Actor{
  // this variable is so the program knows where to send the result when it is done
  var sendto : ActorRef = context.self
  // The manditory retrieve method for the actor class that recieves messages
  def receive: Receive={
//  This is first list that we will recieve from main
    case l1: List[String] =>
      // empty map to be sent to a2 so that we can get our results
      val initialMap = Map.empty[Char, Int].withDefaultValue(0)

      // an instance of the second actor to count the occurence of characters
      val a2 = context.actorOf(Props(new a2(l1, initialMap)))
      // the send variable for who to send the list too when we are done
      sendto = sender()
    //the final result of all the concurrent maps
    case finalResult: Map[Char, Int] =>
      //The result of the final map is sent to the main method as the final result
      sendto !finalResult

  }
}

// This is the second actor that reads the first case of a1
//this recieves the list from a1 and a map, and breaks down this list until none is left to count
// then this result is returned to the actor parent.
class a2(var listinput : List[String], var answer : Map[Char, Int]) extends Actor {
  //first condition to check if the list is empty and if it is send a message back to the parent list.
  if (listinput.length == 0) {
    context.parent ! answer
  }
  else {
    // temp is the first element of the input list and this line removes the spaces in the line so they dont get counted.
    var temp = listinput.head.replaceAll(" ", "")
    // if the list isnt empty then run the analysis on the list
    if (listinput.length > 0) {
      // creates a char list for the temp element of the list
      var charList = temp.toList
      // the value temp map is the temporary map that groups the unique characters and lists the amount of times it appears and took inspiration from this url https://stackoverflow.com/questions/11448685/scala-how-can-i-count-the-number-of-occurrences-in-a-list
      val tempmap = (charList.groupBy(identity).view.mapValues(_.size).toMap)

      // this line merges the two maps and sums the value of the duplicate keys
      answer = answer ++ tempmap.map { case (k1, v1) => k1 -> (v1 + (answer).getOrElse(k1, 0)) }
      // this value then drops the current list head and then calls the second actor again with the editied list input.
      listinput = listinput.drop(1)
      val a3 = context.actorOf(Props(new a2(listinput, answer)))
    }
  }

    // The manditory revieve method for the actor class that recieves messages
    def receive: Receive = {
      //  This is first list that we will recieve from main
      case answer: Map[Char, Int] =>
        context.parent ! answer

    }

}
object counter {
  def main(args: Array[String]): Unit = {
    val list =  Source.fromFile("stuff.txt").getLines.toList
    println(list)
    val system = ActorSystem("counter") // The line for the creation of our main actor system a1
    val firstActor = system.actorOf(Props[a1])// create an instance of the main actor
    val timeout = Timeout (FiniteDuration(Duration("15 seconds").toSeconds, SECONDS))
    val future = ask (firstActor,list) (timeout)
    val result = Await.result (future, timeout.duration)
    println ("The sorted list is " + result)
    system.terminate()
   // firstActor ! list



  }

}
