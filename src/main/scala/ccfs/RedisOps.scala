package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis
import scalaz.Monoid
import scalaz.std.AllInstances._

import scala.collection.JavaConverters._


object RedisOps {

  type Hash = Map[String, String]
  type KeyedMap = Map[String, Hash]

  // Fetch all dispenser keys. Dispenser keys are the session ID prefixed by 'dispenser:'
  def getKeys(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN): List[String] = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(List.empty[String])((acc, res) => res.getResult.asScala.toList ++ acc)
  }

  def zplMap(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN)(implicit monoid: Monoid[KeyedMap]): KeyedMap = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(monoid.zero)((acc, res) =>
      monoid.append(keysToMap(jedis, "zpl", res.getResult.asScala.toList), acc))
  }

  // Retrieve a hash entry given a key
  private def getHash(jedis: Jedis, hashKey: String): Hash =
    Map[String, String](jedis.hgetAll(hashKey).asScala.toSeq: _*)

  private def hashToMap(hash: Hash, mapKey: String): Option[KeyedMap] =
    hash.get(mapKey).map(v => Map(v -> hash))

  private def keyToMap(jedis: Jedis, mapKey: String)(hashKey: String): Option[KeyedMap] =
    hashToMap(getHash(jedis, hashKey), mapKey)

  private def foldMap[T](jedis: Jedis, keys: List[String], f: String => Option[T])(implicit monoid: Monoid[T]): T =
    keys.foldLeft(monoid.zero)((acc, key) =>
      f(key).map(m => monoid.append(m, acc)).getOrElse(acc))

  // Given a list of keys, build a reverse Map keyed by serial number
  private def keysToMap(jedis: Jedis, mapKey: String, keys: List[String]): KeyedMap =
    foldMap(jedis, keys, keyToMap(jedis, mapKey))

}
