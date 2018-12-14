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

      val props = zplProps(jedis)

      println(props.matches("ZPL199137J"))

      println(props.map.keys)
      //      val r = matches(map, "ZPL100493W")
      //
      //      println(r)

    })
  }

}
