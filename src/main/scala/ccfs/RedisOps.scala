package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis
import scalaz.Monoid

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap


object RedisOps {

  type Hash = Map[String, String]
  type ZPLMap = SortedMap[String, Hash] // Dispenser properties keyed by serial number

  // Fetch all dispenser keys. Dispenser keys are the session ID prefixed by 'dispenser:'
  def getKeys(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN): List[String] = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(List.empty[String])((acc, res) => res.getResult.asScala.toList ++ acc)
  }

  def zplMap(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN)(implicit monoid: Monoid[ZPLMap]): ZPLMap = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(monoid.zero)((acc, res) => {
      // todo For testing
      val keys = res.getResult.asScala.toList
      println(s"Retrieved ${keys.length} entries")
      monoid.append(keysToMap(getHashes(jedis, keys), "zpl"), acc)
    })
  }

  def matches(map: ZPLMap, keyPrefix: String): ZPLMap =
    map.dropWhile { case (k, _) => !k.startsWith(keyPrefix) }.takeWhile { case (k, _) => k.startsWith(keyPrefix) }

  // Get all hashes for the given keys using a pipeline
  private def getHashes(jedis: Jedis, hashKeys: List[String]): List[Hash] = {
    val pipeline = jedis.pipelined()
    val responses = hashKeys.map(key => pipeline.hgetAll(key))
    pipeline.sync()
    responses.map(response => Map[String, String](response.get().asScala.toSeq: _*))
  }

  private[ccfs] def toMapEntries(hashes: List[Hash], key: String): List[(String, Hash)] =
    hashes.foldLeft(List.empty[(String, Hash)])((acc, hash) =>
      hash.get(key).map(k => k -> hash :: acc).getOrElse(acc))

  private[ccfs] def keysToMap(hashes: List[Hash], mapKey: String): ZPLMap =
    SortedMap[String, Hash](toMapEntries(hashes, mapKey): _*)
}
