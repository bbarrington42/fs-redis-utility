package ccfs.util

import redis.clients.jedis.{Jedis, ScanParams, ScanResult}

class ScanResultIterator(jedis: Jedis, pattern: String) extends Iterator[ScanResult[String]] {
  val params = new ScanParams
  params.`match`(pattern)

  private var result = jedis.scan(ScanParams.SCAN_POINTER_START, params)

  override def hasNext: Boolean = ScanParams.SCAN_POINTER_START != result.getStringCursor

  override def next(): ScanResult[String] = {
    val rv = result
    result = jedis.scan(rv.getStringCursor, params)
    rv
  }
}

object ScanResultIterator {
  def apply(jedis: Jedis, pattern: String): ScanResultIterator = new ScanResultIterator(jedis, pattern)
}
