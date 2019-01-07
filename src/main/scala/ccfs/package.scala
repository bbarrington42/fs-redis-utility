import ccfs.RedisOps.RedisProperties
import redis.clients.jedis.{Jedis, JedisPool}
import scalaz.Monoid

import scala.collection.immutable.SortedMap

package object ccfs {

  def withJedis(jedisPool: JedisPool)(f: Jedis => Unit): Unit = {
    val jedis = jedisPool.getResource
    try {
      f(jedis)
    } finally jedis.close()
  }

  implicit val sortedMapMonoid = new Monoid[RedisProperties] {
    override def zero: RedisProperties = RedisProperties(SortedMap.empty)

    override def append(f1: RedisProperties, f2: => RedisProperties): RedisProperties = RedisProperties(f1.map ++ f2.map)
  }
}
