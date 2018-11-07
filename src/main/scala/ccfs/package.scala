import redis.clients.jedis.{Jedis, JedisPool}

package object ccfs {

  def withJedis(jedisPool: JedisPool)(f: Jedis => Unit): Unit = {
    val jedis = jedisPool.getResource
    try {
      f(jedis)
    } finally jedis.close()
  }

}
