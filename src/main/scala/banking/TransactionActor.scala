package banking

import akka.actor.{Actor, Props}
import banking.BankingApp.{addTransaction, deposit, transfer, withdraw}

object TransactionActor {
  // Define messages
  case class AddTransaction(custId: Int, transactionType: String, amount: Int)
  case class ManageTransaction( transactionType: String, amount: Int)
  case class Withdraw(userId: Int, amount: Int)
  case class Deposit(userId: Int, amount: Int)
  case class Transfer( sourceAccountId: Int, targetAccountId: Int, amount: Int)

  def props: Props = Props[TransactionActor]
}

class TransactionActor extends Actor {
  import TransactionActor._

  override def receive: Receive = {
    case ManageTransaction( transactionType, amount) =>
      // Print a message
      println(s"Received manageTransaction message:  type=$transactionType, amount=$amount")

      // You can add your database logic or any other processing here

      // Sender() here represents the sender of the message (BankingApp in this case)
      sender() ! "Transaction processed successfully" // Send a response back if needed

    case AddTransaction(custId, transactionType, amount) =>
      // Print a message
      println(s"Received AddTransaction message: custId=$custId, type=$transactionType, amount=$amount")

      // You can add your database logic or any other processing here

      // Sender() here represents the sender of the message (BankingApp in this case)
      sender() ! "Transaction processed successfully" // Send a response back if needed

    case Withdraw(userId, amount) =>
      val transactionType = "Withdrawal"
      withdraw(userId, amount)
      addTransaction(userId, transactionType, amount,status ="approved")
      sender() ! s"Withdrawal processed for user $userId, amount $amount"

    case Deposit(userId, amount) =>
      val transactionType = "Deposit"
      deposit(userId, amount)
      addTransaction(userId, transactionType, amount,status ="approved")
      sender() ! s"Deposit processed for user $userId, amount $amount"

    case Transfer(sourceAccountId, targetAccountId, amount) =>
      val transactionType = "Transfer"
      transfer(sourceAccountId, targetAccountId, amount)
      addTransaction(sourceAccountId, transactionType, -amount,status ="approved") // Subtract from the source account
      addTransaction(targetAccountId, transactionType, amount,status ="approved") // Add to the target account
      sender() ! s"Transfer processed from $sourceAccountId to $targetAccountId, amount $amount"
  }

}