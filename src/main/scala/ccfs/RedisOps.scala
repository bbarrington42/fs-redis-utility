package ccfs

import ccfs.Main.DISPENSER_KEY_PATTERN
import ccfs.util.ScanResultIterator
import redis.clients.jedis.Jedis
import scalaz.Monoid

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap


object RedisOps {

  type Hash = Map[String, String]

  case class Properties(map: SortedMap[String, Hash]) {
    def matches(keyPrefix: String): Properties = RedisOps.matches(this, keyPrefix)
  }

  // Fetch all dispenser keys. Dispenser keys are the session ID prefixed by 'dispenser:'
  def getKeys(jedis: Jedis, pattern: String = DISPENSER_KEY_PATTERN): List[String] =
    jedis.keys(pattern).asScala.toList


  def zplProps(jedis: Jedis): Properties = properties(jedis, "zpl")

  def sessionProps(jedis: Jedis): Properties = properties(jedis, "sessionId")

  private[ccfs] def properties(jedis: Jedis, key: String, pattern: String = DISPENSER_KEY_PATTERN)(implicit monoid: Monoid[Properties]): Properties = {
    val iter = ScanResultIterator(jedis, pattern)

    iter.foldLeft(monoid.zero)((acc, res) =>
      monoid.append(toMap(getHashes(jedis, res.getResult.asScala.toList), key), acc))
  }

  // Return a subset of entries matching the serial number prefix
  private[ccfs] def matches(props: Properties, keyPrefix: String): Properties =
    props.copy(map = props.map.dropWhile { case (k, _) => !k.startsWith(keyPrefix) }.takeWhile { case (k, _) => k.startsWith(keyPrefix) })

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

  private[ccfs] def toMap(hashes: List[Hash], mapKey: String): Properties =
    Properties(SortedMap[String, Hash](toMapEntries(hashes, mapKey): _*))
}
