package ccfs

import ccfs.RedisOps._
import com.typesafe.config.ConfigFactory
import redis.clients.jedis._

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

      val keys = getKeys(jedis)

      println(s"# entries: ${keys.length}")

      val map = sessionMap(jedis)

      val r = matches(map, "02345678-1234-1234-1234-123456789AB1")

      println(r)

      //val map = zplMap(jedis)

      //println(map)

      //val r = matches(map, "ZPL3500371")
//      matches(map, "ZPL3205270"),
//      matches(map, "ZPL320554U"),
//      matches(map, "ZPL320555P"),
//      matches(map, "ZPL3205089"))
//
      //println(r)


    })
  }

}
