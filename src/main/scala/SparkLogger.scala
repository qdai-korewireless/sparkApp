object SparkLogger extends Serializable {
  @transient lazy val log = org.apache.log4j.LogManager.getLogger("SparkApp")

  def debug(msg: String): Unit =   {
    log.debug(msg)
  }

  def error(msg:String, ex: Exception = null): Unit =  {
    log.error(msg, ex)
  }

  def info(msg: String): Unit = {
    log.info(msg)
  }

  def warn(msg: String): Unit =  {
    log.warn(msg)
  }

  def level(): String = {
    log.getEffectiveLevel.toString
  }
}
