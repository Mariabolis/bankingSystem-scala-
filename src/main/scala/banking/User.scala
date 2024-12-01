package banking

import slick.jdbc.MySQLProfile.api._

case class User(id: Int, name: String, email: String, password: String, role: String)


