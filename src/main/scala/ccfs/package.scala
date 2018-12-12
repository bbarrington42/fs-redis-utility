import ccfs.RedisOps.Properties
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

  implicit val sortedMapMonoid = new Monoid[Properties] {
    override def zero: Properties = SortedMap.empty

    override def append(f1: Properties, f2: => Properties): Properties = f1 ++ f2
  }
}
