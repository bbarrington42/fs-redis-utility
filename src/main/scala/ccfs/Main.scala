package ccfs

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}
import com.typesafe.config

object Main {

  val config = ConfigFactory.load().getConfig("redis")
  val poolConfig = new JedisPoolConfig
  poolConfig.setMaxTotal(config.getInt("pool.maxTotal"))

  val jedisPool = new JedisPool(poolConfig, config.getString("host"), config.getInt("port"))

  def connect(jedis: Jedis): Unit = {

  }

  def main(args: Array[String]): Unit = {

  }

}
