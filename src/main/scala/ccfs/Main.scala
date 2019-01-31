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

  def nonEmpty(props: RedisProperties): RedisProperties =
    RedisProperties(props.map.filter { case (_, value) => value.nonEmpty })


  def main(args: Array[String]): Unit = {
    withJedis(jedisPool)(jedis => {

      //println(jedis.info())

      val keys = getKeys(jedis)

      println(s"# total keys: ${keys.length}")

      //println(keys.mkString("\n"))

      val sessProps = sessionProps(jedis)
      val zProps = nonEmpty(zplProps(jedis)).map
      val zpls = zProps.keys.mkString("\n")

      println(s"${zProps.size} connected dispensers")
      println(zpls)

    })
  }

}
