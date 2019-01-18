package ccfs

import ccfs.RedisOps._
import com.typesafe.config.ConfigFactory
import redis.clients.jedis._

object Main {

  val DISPENSER_KEY_PREFIX = "dispenser:"

  val DISPENSER_KEY_PATTERN = s"$DISPENSER_KEY_PREFIX*"

  val config = ConfigFactory.load().getConfig("redis")
  val poolConfig = new JedisPoolConfig
  poolConfig.setMaxTotal(config.getInt("pool.maxTotal"))

  val jedisPool = new JedisPool(poolConfig,
    config.getString("host"),
    config.getInt("port"))


  def main(args: Array[String]): Unit = {
    withJedis(jedisPool)(jedis => {

      val keys = getKeys(jedis)

      println(s"# keys: ${keys.length}")

      //println(keys.mkString("\n"))


      val props = sessionProps(jedis)
        //zplProps(jedis)

      val s = "9736AEEA-26F2-9652-1C9C-EDA87AF402E5"
      println(s"session: $s, ${props.matches(s)}")

//      val zpl = "ZPL10011CE"
//
//      println(s"zpl: $zpl, ${props.matches(zpl)}")

//      val n = delKeys(jedis)
//
//      println(s"Deleted: $n keys")

      //val props = zplProps(jedis)
//
//      println(props)

      //println(props.matches("ZPL3205270"))
//
//      println(props.map.keys)
      //      val r = matches(map, "ZPL100493W")
      //
      //      println(r)

    })
  }

}
