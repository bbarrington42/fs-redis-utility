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
  type ZPLMap = Map[String, Hash]  // Dispenser properties keyed by serial number

  // Fetch all dispenser keys. Dispenser keys are the session ID prefixed by 'dispenser:'
  def getKeys(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN): List[String] = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(List.empty[String])((acc, res) => res.getResult.asScala.toList ++ acc)
  }

  def zplMap(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN)(implicit monoid: Monoid[ZPLMap]): ZPLMap = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(monoid.zero)((acc, res) => {
      // todo For testing
      val l = res.getResult.asScala.toList
      println(s"Retrieved ${l.length} entries")
      monoid.append(keysToMap(jedis, "zpl", l), acc)})
  }

  // Retrieve a hash entry given a key
  private def getHash(jedis: Jedis, hashKey: String): Hash =
    Map[String, String](jedis.hgetAll(hashKey).asScala.toSeq: _*)

  private def toMapEntry(hash: Hash, entryKey: String): Option[(String, Hash)] =
    hash.get(entryKey).map(v => v -> hash)

  private def keysToMap(jedis: Jedis, mapKey: String, keys: List[String]): ZPLMap = {
    val entries = keys.map(key => toMapEntry(getHash(jedis, key), mapKey))
    Traverse[List].sequence(entries).map(_.toMap).getOrElse(Map.empty)
  }

}
