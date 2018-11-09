package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis

import scala.collection.JavaConverters._

object RedisOps {

  // Fetch all dispenser keys. Dispenser keys are the session ID prefixed by 'dispener:'
  def getKeys(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN): List[String] = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(List.empty[String])((acc, res) => res.getResult.asScala.toList ++ acc)
  }

  // Retrieve a hash entry given a key
  def getHash(jedis: Jedis, key: String): Map[String, String] =
    Map[String, String](jedis.hgetAll(key).asScala.toSeq: _*)

  // Given a list of keys, build a reverse Map keyed by serial number
  def zplMap(jedis: Jedis, keys: List[String]): Map[String, List[String]] = {
    keys.foldLeft(Map.empty[String, List[String]])((acc, key) => {
      val hash = getHash(jedis, key)
      val entry = hash.get("zpl").map(s => acc + (s -> hash.values.toList))
      entry.getOrElse(acc)
    })
  }
}
