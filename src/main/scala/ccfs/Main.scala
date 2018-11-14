package ccfs

import ccfs.RedisOps._
import com.typesafe.config.ConfigFactory
import redis.clients.jedis._
import scalaz.std.AllInstances._

object Main {

  val DISPENSER_KEY_PATTERN = "dispenser:*"

  val config = ConfigFactory.load().getConfig("redis")
  val poolConfig = new JedisPoolConfig
  poolConfig.setMaxTotal(config.getInt("pool.maxTotal"))

  val jedisPool = new JedisPool(poolConfig,
    config.getString("host"),
    config.getInt("port"))


  def main(args: Array[String]): Unit = {
    withJedis(jedisPool)(jedis => {

      val map = zplMap(jedis)
      val r = matches(map, "ZPL8")

      println(s"${r.size} entries")

    })
  }

}
