import java.sql.{Connection, DriverManager}
import java.util.Properties

import com.typesafe.config.Config
import org.apache.commons.dbutils.handlers.BeanListHandler
import org.apache.commons.dbutils.{DbUtils, QueryRunner}

import scala.reflect.ClassTag

class SQLDBConnector(config: Config) {
  def getConnectionProperties(): Properties = {

    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")

    val connectionProperties = new Properties()
    connectionProperties.put("user", config.getString("mssql.user"))
    connectionProperties.put("password", config.getString("mssql.password"))

    val driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    connectionProperties.setProperty("Driver", driverClass)
    connectionProperties
  }

  def getJdbcUrl(db: String = "be"): String = {
    val host = config.getString("mssql.host")
    val port = config.getString("mssql.port")
    val dbName = config.getString(s"mssql.${db}")
    s"jdbc:sqlserver://$host:$port;database=$dbName;useBulkCopyForBatchInsert=true"
  }

  def getConnection(db: String = "be"): Connection = {
    val prop = getConnectionProperties()
    val jdbcUrl = getJdbcUrl(db)
    val connection = DriverManager.getConnection(jdbcUrl, prop)
    connection.setAutoCommit(true)
    connection
  }

  def execute(conn: Connection, query: String, params: Array[Object]): Int = {
    val runQuery = new QueryRunner()
    try {
      runQuery.update(conn, query, params: _*)
    }
    catch {
      case ex: Exception =>
        throw ex
    }
  }
  def select[T](conn: Connection, query: String, params: Array[Object])(implicit ctag: ClassTag[T]): Array[T] = {
    import collection.JavaConverters._

    def objClass: Class[T] = ctag.runtimeClass.asInstanceOf[Class[T]]

    val runQuery = new QueryRunner()
    val mapper = new BeanListHandler(objClass)
    try {
      val result = runQuery.query(conn, query, mapper, params: _*)
      if(result == null) Array() else
        result.asScala.toArray
    }
    catch {
      case ex: Exception => {
        throw ex
        null
      }
    }
  }
  def select[T](query: String, params: Array[Object],db:String = "be")(implicit ctag: ClassTag[T]): Array[T] = {
    val connection = getConnection(db)
    try {
      select(connection, query, params)
    }
    catch {
      case ex: Exception =>
        throw ex
        null
    }
    finally {
      DbUtils.close(connection)
    }
  }

}
