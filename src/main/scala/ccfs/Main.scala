package ccfs

import ccfs.util.ScanResultIterator
import com.typesafe.config.ConfigFactory
import redis.clients.jedis._

import scala.collection.JavaConverters._

object Main {

  val DISPENSER_KEY_PREFIX = "dispenser:*"

  val config = ConfigFactory.load().getConfig("redis")
  val poolConfig = new JedisPoolConfig
  poolConfig.setMaxTotal(config.getInt("pool.maxTotal"))

  val jedisPool = new JedisPool(poolConfig,
    config.getString("host"),
    config.getInt("port"))


  def list(jedis: Jedis, pattern: String = DISPENSER_KEY_PREFIX): List[List[String]] = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(List.empty[List[String]])((acc, res) => res.getResult.asScala.toList :: acc)
  }

  def main(args: Array[String]): Unit = {
    withJedis(jedisPool)(jedis => {
      val listing = list(jedis)

      val s = listing.mkString(",\n")

      println(s)
    })
  }

}
