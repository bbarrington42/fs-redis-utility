package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis
import scalaz.std.list._
import scalaz.std.option._
import scalaz.{Monoid, Traverse}

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap


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

  // Get all hashes for the given keys using a pipeline
  private def getHashes(jedis: Jedis, hashKeys: List[String]): List[Hash] = {
    val pipeline = jedis.pipelined()
    val responses = hashKeys.map(key => pipeline.hgetAll(key))
    pipeline.sync()
    responses.map(response => Map[String, String](response.get().asScala.toSeq: _*))
  }

  private def toMapEntries(hashes: List[Hash], key: String): List[(String, Hash)] =
    hashes.foldLeft(List.empty[(String, Hash)])((acc, hash) =>
      hash.get(key).map(k => k -> hash :: acc).getOrElse(acc))

  private def keysToMap(jedis: Jedis, mapKey: String, keys: List[String]): ZPLMap =
    SortedMap[String, Hash](toMapEntries(getHashes(jedis, keys), mapKey): _*)
}
