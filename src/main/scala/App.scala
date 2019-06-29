import com.typesafe.config.ConfigFactory
import org.apache.log4j.PropertyConfigurator
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.OutputMode

import scala.collection.mutable

object App {
  def main(args:Array[String]):Unit={


    PropertyConfigurator.configure(getClass.getClassLoader.getResourceAsStream("log4j.properties"))

    val config = ConfigFactory.load("config.conf")
    val sparkConfig = new SparkConf()
      .setAppName(config.getString("spark.appName"))
      .setMaster(config.getString("spark.master"))
      .set("spark.cassandra.connection.host", config.getString("cassandra.server"))
      .set("spark.cassandra.auth.username", config.getString("cassandra.username"))
      .set("spark.cassandra.auth.password", config.getString("cassandra.password"))
      .set("cassandra.keyspace", config.getString("cassandra.keyspace"))
      .set("spark.scheduler.mode", config.getString("spark.scheduler.mode"))

    val spark = SparkSession
      .builder
      .config(sparkConfig)
      .getOrCreate()
    import spark.implicits._

    val map = new mutable.HashMap[String, String]()
    map += ("kafka.bootstrap.servers" -> config.getString("kafka.servers"))
    val topics = config.getString("kafka.streams.sparkApp.topicIn")
    map += ("subscribe" -> topics)
    map += ("checkpointLocation" -> config.getString("spark.checkpointLocation"))
    val src = spark.readStream.format("kafka").options(map).load()
    val result = src.selectExpr("cast(key as string) as key", "cast(value as string) as value").map(r=>(r(0).toString,r(1).toString)).as[(String,String)].map(x => {

     SparkLogger.log.error(s"key:${x._1}, value:${x._2}")
      KafkaMsg(key = x._1, value = x._2, topic =config.getString("kafka.streams.sparkApp.topicOut") )
    }).toDF

    result.writeStream.format("kafka").option("kafka.bootstrap.servers",config.getString("kafka.servers"))
      .outputMode(OutputMode.Append())
      .option("checkpointLocation",config.getString("spark.checkpointLocation")).start()

    spark.streams.awaitAnyTermination()
  }
}
