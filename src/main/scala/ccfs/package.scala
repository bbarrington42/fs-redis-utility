import ccfs.RedisOps.ZPLMap
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

  implicit val sortedMapMonoid = new Monoid[ZPLMap] {
    override def zero: ZPLMap = SortedMap.empty

    override def append(f1: ZPLMap, f2: => ZPLMap): ZPLMap = f1 ++ f2
  }
}
