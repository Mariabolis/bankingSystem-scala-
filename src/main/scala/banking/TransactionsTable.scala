package banking

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

import java.sql.Timestamp



class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "transactions") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def custId: Rep[Int] = column[Int]("custId")
  def transactionType: Rep[String] = column[String]("transactionType")
  def amount: Rep[Int] = column[Int]("amount")
  def timestamp: Rep[Timestamp] = column[Timestamp]("timestamp")
  def status: Rep[String] = column[String]("status")

  def * : ProvenShape[Transaction] =
    (id, custId, transactionType, amount, timestamp, status) <> (Transaction.tupled, Transaction.unapply)
}
