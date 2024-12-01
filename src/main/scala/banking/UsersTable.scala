package banking

// UsersTable.scala
import slick.jdbc.MySQLProfile.api._

class UsersTable(tag: Tag) extends Table[User](tag, "user") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def email = column[String]("email")
  def password = column[String]("password")
  def role = column[String]("role")

  // Updated mapping
  def * = (id, name, email, password, role).mapTo[User]
}
