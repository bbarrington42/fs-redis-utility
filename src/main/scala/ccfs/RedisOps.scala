package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis
import scalaz.{Foldable, Monoid}
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

  private def toMapEntry(hash: Hash, entryKey: String): KeyedMap =
    hash.get(entryKey).map(v => Map(v -> hash)).getOrElse(Map.empty)

  private def keyToMap(jedis: Jedis, mapKey: String)(hashKey: String): KeyedMap =
    toMapEntry(getHash(jedis, hashKey), mapKey)

  private def keysToMap[F[_]](jedis: Jedis, mapKey: String, keys: F[String])(implicit foldable: Foldable[F]): KeyedMap =
    foldable.foldMap[String, KeyedMap](keys)(keyToMap(jedis, mapKey))

}
