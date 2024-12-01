package banking
import slick.jdbc.MySQLProfile.api._
class AccountsTable(tag: Tag) extends Table[Account](tag, "accounts") {
  def id= column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def cId = column[Int]("cId")
  def balance = column[Int]("balance")

  // other columns...
  def * = (id, cId, balance) <> (Account.tupled, Account.unapply)
}
