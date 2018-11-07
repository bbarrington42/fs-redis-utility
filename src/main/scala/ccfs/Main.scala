package ccfs

import com.typesafe.config.ConfigFactory
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

object Main {

  val config = ConfigFactory.load().getConfig("redis")
  val poolConfig = new JedisPoolConfig
  poolConfig.setMaxTotal(config.getInt("pool.maxTotal"))

  val jedisPool = new JedisPool(poolConfig, config.getString("host"), config.getInt("port"))

  def connect(jedis: Jedis): Unit = {

  }

  def main(args: Array[String]): Unit = {
    withJedis(jedisPool)(connect)
  }

}
