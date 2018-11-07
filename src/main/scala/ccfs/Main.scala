package ccfs

import com.typesafe.config.ConfigFactory
import redis.clients.jedis._
import scalaz.Monoid

import scala.annotation.tailrec

import scala.collection.JavaConverters._

import scalaz.std.list._

object Main {

  val config = ConfigFactory.load().getConfig("redis")
  val poolConfig = new JedisPoolConfig
  poolConfig.setMaxTotal(config.getInt("pool.maxTotal"))

  val jedisPool = new JedisPool(poolConfig, config.getString("host"), config.getInt("port"))



  val DISPENSER_KEY_PREFIX = "dispenser:"

  // Generalize operations requiring a scan of all keys matching a pattern followed by a transformation of the result.
  // The result must be a Monoid.
  def scan[T](jedis: Jedis, keyPattern: String, xform: (ScanResult[String], Jedis) => T)(implicit m: Monoid[T]): T = {
    val params = new ScanParams
    params.`match`(keyPattern)

    @tailrec
    def _scan(result: ScanResult[String], acc: T): T = {
      val t = xform(result, jedis)
      val nextCursor = result.getStringCursor
      if (ScanParams.SCAN_POINTER_START == nextCursor) m.append(acc, t)
      else _scan(jedis.scan(nextCursor, params), m.append(acc, t))
    }

    _scan(jedis.scan(ScanParams.SCAN_POINTER_START, params), m.zero)
  }

  def collect(result: ScanResult[String], jedis: Jedis): List[String] = {
    result.getResult.asScala.toList
  }

  def list(jedis: Jedis): List[String] = {
    scan(jedis, DISPENSER_KEY_PREFIX, collect)
  }

  def main(args: Array[String]): Unit = {
    withJedis(jedisPool)(jedis => {
      val listing = list(jedis)

      println(listing)
    })
  }

}
