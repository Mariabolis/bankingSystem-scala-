package banking

import java.sql.Timestamp

case class Transaction(id: Int, custId: Int, transactionType: String, amount: Int, timestamp: Timestamp, status: String)