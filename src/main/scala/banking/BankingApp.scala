package banking

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions

object BankingApp extends App {
  // Load configuration
  val config = ConfigFactory.load()

  // Create an instance of your configured database
  val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("myDatabaseConfig", config)
  val db: JdbcProfile#Backend#Database = databaseConfig.db


  //using akka here with time for it
  var actorSystem = ActorSystem("BankingActorSystem")
  var transactionActor = actorSystem.actorOf(TransactionActor.props, "transactionActor")
  var userActor = actorSystem.actorOf(UserActor.props, "userActor")
  implicit val timeout: Timeout = Timeout(10.seconds)


  // Create instances of your table queries
  var UsersTable = TableQuery[UsersTable]
  var AccountsTable = TableQuery[AccountsTable]
  var TransactionsTable = TableQuery[TransactionsTable]
  val transactions = TableQuery[TransactionsTable]

  // Placeholder for the current loggedin user
  var currentUser: Option[User] = None

  // Login loop
  while (currentUser.isEmpty) {
    currentUser = authenticateUser()
    if (currentUser.isEmpty) {
      println("Invalid credentials. Please try again.")
    }
  }

  // Main program loop
  var exitLoop = false
  while (!exitLoop) {
    // Check if the user is an admin
    val isAdmin = currentUser.exists(user => user.role == "1")

    if (isAdmin) {
      // Display admin menu
      displayAdminMenu()

      val adminChoice = getUserInput("Enter your choice: ")


      adminChoice match {
        case "1" =>
          // Admin: Display Details
          println("Admin Details:")
          currentUser.foreach(println)

        case "2" =>
          // Admin: Add New Customer
          addNewUser()

        case "3" =>
          // Admin: list Transaction Details
          listTransactions()
        case "4" =>
          // Admin: manage Transaction
          manageTransactions()

        case "5" =>
          // Admin: Update User by ID
          updateUserById()

        case "6" =>
          // Admin: List All Users
          listAllUsers()

        case "7" =>
          // Admin: Delete User by ID
          deleteUserById()
        case "8" =>
          // Admin: Assign Balance to User
          addToBalanceToUser()
        case "9" =>
          // Admin: Assign Balance to User
          updateBalance()

        case "10" =>
          exitLoop = true
          println("Exiting the program.")

        case _ =>
          println("Invalid choice. Please enter a valid option.")
      }
    } else {
      // Display customer menu
      displayCustomerMenu()

      val customerChoice = getUserInput("Enter your choice: ")

      customerChoice match {
        case "1" =>
          // Customer: Display Details
          println("Customer Details:")
          currentUser.foreach(println)

        case "2" =>
          // Customer: Update Information
          updateCustomerInformation()

        case "3" =>
          // Customer: Display Account Balance
          displayUserBalance()

        case "4" =>
          // Customer: Transactions Submenu
          displayTransactionSubmenu()

        case "5" =>
          exitLoop = true
          println("Exiting the program.")

        case _ =>
          println("Invalid choice. Please enter a valid option.")
      }
    }
  }

  //---------------------------- Implementations of methods used in all system ---------------------*/

  def getUserInput(info: String): String = {
    print(info)
    scala.io.StdIn.readLine()
  }

  def authenticateUser(): Option[User] = {
    println("Please log_in to use our bank system :) ")
    val username = getUserInput("Username: ")
    val password = getUserInput("Password: ")

    val userQuery = UsersTable.filter(user => user.name === username && user.password === password)
    val userFuture: Future[Option[User]] = db.run(userQuery.result.headOption)

    Await.result(userFuture, Duration.Inf)
  }

  def displayAdminMenu(): Unit = {
    // Implement admin menu display
    println("1. Display Admin Details")
    println("2. Add New user")
    println("3. list Transactions Details")
    println("4. manage Transactions ")
    println("5. Update User Details")
    println("6. list all users")
    println("7. Delete User")
    println("8. add to balance to User")
    println("9. update balance to User")
    println("10. Exit")
  }

  def displayCustomerMenu(): Unit = {
    // Implement customer menu display
    println("1. Display Customer Details")
    println("2. Update Customer Information")
    println("3. Display Your Balance")
    println("4. Make Transaction")
    println("5. Exit")
  }

  def displayTransactionSubmenu(): Unit = {
    // Implement transactions submenu display
    println("1. Withdraw")
    println("2. Deposit")
    println("3. Transfer")
    println("4. view last Transactions")
    println("5. Back to Main Menu")
    println("6. exit")

    val transactionChoice = getUserInput("Enter your choice: ")

    transactionChoice match {
      case "1" =>
        // Customer: Withdraw
        currentUser.foreach { user =>
          val accountId = user.id // Automatically use the logged-in user's ID as the account ID
          val amount = getUserInput("Enter the amount to withdraw: ").toInt
          withdraw(accountId, amount)
        }

      case "2" =>
        // Customer: Deposit
        currentUser.foreach { user =>
          val accountId = user.id // Automatically use the logged-in user's ID as the account ID
          val amount = getUserInput("Enter the amount to deposit: ").toInt
          deposit(accountId, amount)
        }

      case "3" =>
        // Customer: Transfer
        currentUser.foreach { user =>
          val sourceAccountId = user.id // Automatically use the logged-in user's ID as the source account ID
          val targetAccountId = getUserInput("Enter the ID of the target account: ").toInt
          val amount = getUserInput("Enter the amount to transfer: ").toInt
          transfer(sourceAccountId, targetAccountId, amount)
        }

      case "4" =>
        // Customer: View Transactions
        viewRecentTransactions()

      case "5" =>
        // Back to main menu
        println("Returning to the main menu.")

      case "6" =>
        exitLoop = true
        println("Exiting the program.")

      case _ =>
        println("Invalid choice. Please enter a valid option.")
    }
  }

  // -------------------------admin functions------------------------------------*/

  def addNewUser(): Unit = {
    println("Enter details for the new customer:")
    val newName = getUserInput("Enter name: ")
    val newEmail = getUserInput("Enter email: ")
    val newPassword = getUserInput("Enter password: ")
    val newRole = getUserInput("Enter role: ")
    val initialBalance = getUserInput("Enter initial balance: ").toInt

    // Create a new User with role CustomerRole and balance
    val newUser = User(0, newName, newEmail, newPassword, newRole)
    println(s"New user added successfully: $newUser")

    val insertUserAction = UsersTable += newUser
    val userIdFuture: Future[Int] = db.run(insertUserAction).flatMap { _ =>
      val getIdAction = sql"SELECT LAST_INSERT_ID()".as[Int].head
      db.run(getIdAction)
    }
    val userId = Await.result(userIdFuture, Duration.Inf)

    // Insert a new row into the AccountsTable with the corresponding cId and initial balance
    val newAccount = Account(id = None, cId = userId, balance = initialBalance) // Modify this line
    val insertAccountAction = AccountsTable += newAccount
    val getIdAction = sql"SELECT LAST_INSERT_ID()".as[Int].head
    db.run(getIdAction)
    val accountIdFuture: Future[Int] = db.run(insertAccountAction).flatMap { _ =>
      val getIdAction = sql"SELECT LAST_INSERT_ID()".as[Int].head
      db.run(getIdAction)
    }

    val accountId = Await.result(accountIdFuture, Duration.Inf)

    println(s"User and account added successfully. User ID: $userId, Account ID: $accountId")
    val addUserMessage = UserActor.AddUser(newName, newEmail, newPassword, initialBalance)
    userActor ! addUserMessage

    Future.successful(())
  }

  def updateUserById(): Unit = {
    val userIdToUpdate = getUserInput("Enter the ID of the user to update: ").toInt

    val userToUpdateQuery = UsersTable.filter(_.id === userIdToUpdate)
    val existingUserFuture: Future[Option[User]] = db.run(userToUpdateQuery.result.headOption)

    val existingUser = Await.result(existingUserFuture, Duration.Inf)

    existingUser match {
      case Some(user) =>
        val newName = getUserInput("Enter new name: ")
        val newEmail = getUserInput("Enter new email: ")


        val updatedUser = User(user.id, newName, newEmail, user.password, "0")

        // Use UserActor to update the user
        val updateUserMessage = UserActor.UpdateUser(newName, newEmail, user.password, 0)
        userActor ! updateUserMessage

        val updateAction = userToUpdateQuery.update(updatedUser)
        val updateResult: Future[Int] = db.run(updateAction)

        val rowsUpdated = Await.result(updateResult, Duration.Inf)

        if (rowsUpdated > 0) {
          println(s"User with ID $userIdToUpdate updated successfully.")
        } else {
          println(s"No user found with ID $userIdToUpdate.")
        }

      case None =>
        println(s"No user found with ID $userIdToUpdate.")
    }
  }

  def listAllUsers(): Unit = {
    val allUsersQuery = UsersTable
    val allUsersFuture: Future[Seq[User]] = db.run(allUsersQuery.result)

    val allUsers = Await.result(allUsersFuture, Duration.Inf)

    if (allUsers.nonEmpty) {
      println("All Users:")
      allUsers.foreach(println)
    } else {
      println("No users found.")
    }
  }

  def deleteUserById(): Unit = {
    val userIdToDelete = getUserInput("Enter the ID of the user to delete: ").toInt
//retrieve user with sent id
    val userToDeleteQuery = UsersTable.filter(_.id === userIdToDelete)
    val existingUserFuture: Future[Option[User]] = db.run(userToDeleteQuery.result.headOption)

    val existingUser = Await.result(existingUserFuture, Duration.Inf)

    existingUser match {
      case Some(_) =>
        // UserActor to delete the user
        val deleteUserMessage = UserActor.DeleteUser(userIdToDelete)
        userActor ! deleteUserMessage

        val deleteAction = userToDeleteQuery.delete
        val deleteResult: Future[Int] = db.run(deleteAction)

        val rowsDeleted = Await.result(deleteResult, Duration.Inf)

        if (rowsDeleted > 0) {
          println(s"User with ID $userIdToDelete deleted successfully.")
        } else {
          println(s"No user found with ID $userIdToDelete.")
        }

      case None =>
        println(s"No user found with ID $userIdToDelete.")
    }
  }


  // add balance to a user
  def addToBalanceToUser(): Unit = {
    val userIdToUpdate = getUserInput("Enter the ID of the user to assign balance: ").toInt
    val balanceToAdd = getUserInput("Enter the balance to add: ").toInt

    // Insert a new row into the AccountsTable with the corresponding cId
    val newAccount = Account(id = None, cId = userIdToUpdate, balance = balanceToAdd)
    val insertAction = AccountsTable += newAccount

    // Use flatMap to chain the insert with a query to retrieve the generated ID
    val accountIdFuture: Future[Int] = db.run(insertAction).flatMap { _ =>
      // Your ID column is auto-incremented
      val getIdAction = sql"SELECT LAST_INSERT_ID()".as[Int].head
      db.run(getIdAction)
    }

    accountIdFuture.map { userIdToUpdate =>
      println(s"Balance added to user successfully.")
    }.recover {
      case exception =>
        println(s"Error assigning balance: ${exception.getMessage}")
    }
  }

  // Update balance for a user
  def updateBalance(): Unit = {
//    implicit val timeout: Timeout = Timeout(5.seconds)
    val userIdToUpdate = getUserInput("Enter the ID of the user to update balance: ").toInt
    val newBalance = getUserInput("Enter the new balance: ").toInt

    val userQuery = AccountsTable.filter(account => account.cId === userIdToUpdate)
    val existingAccountFuture: Future[Option[Account]] = db.run(userQuery.result.headOption)

    existingAccountFuture.flatMap {
      case Some(existingAccount) =>
        val updatedAccount = existingAccount.copy(balance = newBalance)

        val updateAction = userQuery.update(updatedAccount)
        val updateResult: Future[Int] = db.run(updateAction)

        updateResult.map { rowsUpdated =>
          if (rowsUpdated > 0) {
            println(s"Balance updated for user with ID $userIdToUpdate successfully.")
          } else {
            println(s"No user found with ID $userIdToUpdate.")
          }
        }

      case None =>
        println(s"No user found with ID $userIdToUpdate.")
        Future.successful(())
    }.recover {
      case exception =>
        println(s"Error updating balance: ${exception.getMessage}")
    }
  }


  def listTransactions(): Unit = {
    val transactionsQuery = TransactionsTable.sortBy(_.timestamp.desc).result

    val transactionsFuture: Future[Seq[Transaction]] = db.run(transactionsQuery)

    val transactions = Await.result(transactionsFuture, Duration.Inf)

    if (transactions.nonEmpty) {
      println("All Transactions:")
      transactions.foreach(println)
    } else {
      println("No transactions found.")
    }
  }

  def manageTransactions(): Unit = {
    currentUser.foreach { user =>
      if (user.role == "1") {
        val transactionsQuery = TransactionsTable.filter(_.status === "Pending").result

        val pendingTransactionsFuture: Future[Seq[Transaction]] = db.run(transactionsQuery)
        val pendingTransactions = Await.result(pendingTransactionsFuture, Duration.Inf)

        if (pendingTransactions.nonEmpty) {
          println("Pending Transactions:")
          pendingTransactions.foreach(println)

          val transactionIdToManage = getUserInput("Enter the ID of the transaction to manage: ").toInt

          val approveOrDeny = getUserInput("Approve (A) or Deny (D) the transaction? ").toUpperCase()
          val status = if (approveOrDeny == "A") "Approved" else "Denied"

          val updateStatusAction = TransactionsTable.filter(_.id === transactionIdToManage).map(_.status).update(status)
          val updatedRows = Await.result(db.run(updateStatusAction), Duration.Inf)

          if (updatedRows > 0) {
            if (status == "Approved") {
              val transactionAmountQuery = TransactionsTable.filter(_.id === transactionIdToManage).map(_.amount)
              val transactionAmountFuture = db.run(transactionAmountQuery.result.headOption)
              val transactionAmount = Await.result(transactionAmountFuture, Duration.Inf).getOrElse(0)

              val accountIdQuery = TransactionsTable.filter(_.id === transactionIdToManage).map(_.custId)
              val accountIdFuture = db.run(accountIdQuery.result.headOption)
              val accountId = Await.result(accountIdFuture, Duration.Inf).getOrElse(0)

              val accountQuery = AccountsTable.filter(account => account.cId === accountId)
              val balanceQuery = accountQuery.map(_.balance)

              val currentBalance = Await.result(db.run(balanceQuery.result.headOption).map(_.getOrElse(0)), Duration.Inf)
              val updatedBalance = currentBalance + transactionAmount

              val updateBalanceAction = accountQuery.map(_.balance).update(updatedBalance)
              Await.result(db.run(updateBalanceAction), Duration.Inf)
            }

            println(s"Transaction with ID $transactionIdToManage $status successfully.")
            val manageTransactionMessage = TransactionActor.AddTransaction
            transactionActor ! manageTransactionMessage

            // Return a Future for consistency, though it may not be necessary depending on your use case
            Future.successful(())
          } else {
            println(s"No transaction found with ID $transactionIdToManage.")
          }
        } else {
          println("No pending transactions found.")
        }
      } else {
        println("Only admins can manage transactions.")
      }
    }
  }
  //--------------------------------Customers methods------------------------------------*/
  def updateCustomerInformation(): Unit = {
    val newName = getUserInput("Enter new name: ")
    val newEmail = getUserInput("Enter new email: ")
    val newPassword = getUserInput("Enter new password: ")
    currentUser = currentUser.map(_.copy(name = newName, email = newEmail, password=newPassword))
    println("Customer information updated successfully.")
  }


  // Display the balance from the Account table
  def displayUserBalance(): Unit = {
    currentUser.foreach { user =>
      // Display the balance directly from the AccountsTable
      val balanceQuery = AccountsTable.filter(_.cId === user.id)
      val balanceFuture: Future[Option[Int]] = db.run(balanceQuery.map(_.balance).result.headOption)

      val balance = Await.result(balanceFuture, Duration.Inf)
      println(s"User Balance: $balance")
    }
  }


  // Display the transactions

  def addTransaction(custId: Int, transactionType: String, amount: Int, status: String): Future[Unit] = {

    val transaction = Transaction(id = 0, custId = custId, transactionType = transactionType, amount = amount, timestamp = new Timestamp(System.currentTimeMillis()), status = status)
    val insertTransactionAction = TransactionsTable += transaction
    db.run(insertTransactionAction).map(_ => ())
      .recover {
        case ex: Exception =>
          println(s"Error adding transaction: ${ex.getMessage}")
          ex.printStackTrace()
      }
    // Send a message to the transactionActor to add a transaction
    val addTransactionMessage = TransactionActor.AddTransaction(custId, transactionType, amount)
    transactionActor ! addTransactionMessage

    // Return a Future for consistency, though it may not be necessary depending on your use case
    Future.successful(())
  }


  //--------------------------------transactions for customers--------------------------------------------------*/

  def withdraw(accountId: Int, amount: Int): Unit = {
    currentUser.foreach { user =>
      val accountQuery = AccountsTable.filter(account => account.cId === user.id && account.cId === accountId)
      val balanceQuery = accountQuery.map(_.balance)

      val currentBalance = Await.result(db.run(balanceQuery.result.headOption.map(_.getOrElse(0))), Duration.Inf)

      if (currentBalance >= amount) {
        val updatedBalance = currentBalance - amount
        val updateAction = accountQuery.map(_.balance).update(updatedBalance)
        val transactionType = "Withdrawal"
        addTransaction(custId = user.id, transactionType, amount, "approved")
        Await.result(db.run(updateAction), Duration.Inf)

        println(s"Withdrawal successful. New balance: $updatedBalance")
        val addTransactionMessage = TransactionActor.AddTransaction
        transactionActor ! addTransactionMessage

        // Return a Future for consistency, though it may not be necessary depending on your use case
        Future.successful(())
      } else {
        println("Insufficient funds for withdrawal.")
      }
    }
  }

  def deposit(accountId: Int, amount: Int): Unit = {
    currentUser.foreach { user =>

      val transactionStatus = "pending" // Set the initial status as "pending"
      val timestamp = new Timestamp(System.currentTimeMillis()) // Get the current timestamp

      val transactionType = "Deposit"
      val transaction = Transaction(0,custId = user.id, transactionType, amount, timestamp, transactionStatus)

      try {
        val insertAction = transactions returning transactions.map(_.id) += transaction
        val transactionIdFuture: Future[Int] = db.run(insertAction)

        val transactionId = Await.result(transactionIdFuture, Duration.Inf)

        println(s"Deposit request submitted successfully.")
        println(s"Transaction ID: $transactionId")
      } catch {
        case ex: Exception =>
          println("Error occurred while inserting the transaction: " + ex.getMessage)
      }
    }
  }


  def transfer(sourceAccountId: Int, targetAccountId: Int, amount: Int): Unit = {
    currentUser.foreach { user =>
      val sourceAccountQuery = AccountsTable.filter(account => account.cId === user.id && account.id === sourceAccountId)
      val targetAccountQuery = AccountsTable.filter(account => account.id === targetAccountId)

      val resultFuture: Future[Unit] = for {
        sourceAccountOption <- db.run(sourceAccountQuery.result.headOption)
        targetAccountOption <- db.run(targetAccountQuery.result.headOption)
        _ <- {
          (sourceAccountOption, targetAccountOption) match {
            case (Some(sourceAccount), Some(targetAccount)) =>
              if (sourceAccount.balance >= amount) {
                val updatedSourceBalance = sourceAccount.balance - amount
                val updatedTargetBalance = targetAccount.balance + amount

                val updateSourceAction = sourceAccountQuery.map(_.balance).update(updatedSourceBalance)
                val updateTargetAction = targetAccountQuery.map(_.balance).update(updatedTargetBalance)

                db.run(updateSourceAction).flatMap(_ => db.run(updateTargetAction)).map { _ =>
                  val transactionType = "Transfer"
                  addTransaction(custId = user.id, transactionType, -amount, "approved")
                  addTransaction(custId = user.id, transactionType, amount, "approved")
                  println(s"Transfer successful. Updated balances - Source: $updatedSourceBalance, Target: $updatedTargetBalance")
                }.recover {
                  case e: Exception =>
                    println(s"Error updating balances: ${e.getMessage}")
                }
              } else {
                Future.failed(new RuntimeException("Insufficient funds for transfer."))
              }
            case (None, _) =>
              Future.failed(new RuntimeException("Invalid source account."))
            case (_, None) =>
              Future.failed(new RuntimeException(s"Invalid target account with ID $targetAccountId."))
          }
        }
      } yield ()

      resultFuture.recover {
        case e: Exception =>
          println(s"Error during transfer: ${e.getMessage}")
      }
    }
  }

  def viewRecentTransactions(): Unit = {
    currentUser.foreach { user =>
      val transactionsQuery = TransactionsTable.filter(_.custId === user.id).sortBy(_.timestamp.desc).take(10) // Adjust the number of transactions as needed

      val transactionsFuture: Future[Seq[Transaction]] = db.run(transactionsQuery.result)

      val transactions = Await.result(transactionsFuture, Duration.Inf)

      if (transactions.nonEmpty) {
        println("Recent Transactions:")
        transactions.foreach(println)
      } else {
        println("No recent transactions found.")
      }
    }
  }
}