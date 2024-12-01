package banking

import akka.actor.{Actor, Props}

object UserActor {
  // Define messages
  case class AddUser(name: String, email: String, password: String ,balance:Int)
  case class UpdateUser(name: String, email: String, password: String ,role:Int)
  case class DeleteUser(userId: Int)

  def props: Props = Props[UserActor]
}

class UserActor extends Actor {
  import UserActor._

  override def receive: Receive = {
    case AddUser(name, email, password, balance) =>
      // Print a message
      println(s"Received AddUser message: name=$name, email=$email, password=$password , balance=$balance")
      sender() ! "User added successfully" // Send a response back if needed


    case UpdateUser(newName, newEmail, newPassword, role) =>
      // Print a message
      println(s"Received UpdtedUser message: new name=$newName, new email=$newEmail, new password=$newPassword , role=$role")
      sender() ! "User updated successfully" // Send a response back if needed

    case DeleteUser(userId) =>
      // Print a message
      println(s"Received deletedUser message:  userId=$userId")
      sender() ! "User deleted successfully" // Send a response back if needed
  }}