package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis
import scalaz.std.list._
import scalaz.std.option._
import scalaz.{Monoid, Traverse}

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

  private def toMapEntry(hash: Hash, entryKey: String): Option[(String, Hash)] =
    hash.get(entryKey).map(v => v -> hash)

  private def keysToMap(jedis: Jedis, mapKey: String, keys: List[String]): KeyedMap = {
    val entries = keys.map(key => toMapEntry(getHash(jedis, key), mapKey))
    Traverse[List].sequence(entries).map(_.toMap).getOrElse(Map.empty)
  }

}
