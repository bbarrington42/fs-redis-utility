package ccfs.util

import ccfs.util.ScanResultIterator.Result
import redis.clients.jedis.{Jedis, ScanParams, ScanResult}

class ScanResultIterator(jedis: Jedis, pattern: String) extends Iterator[Result] {
  val params = new ScanParams
  params.`match`(pattern)
  params.count(5000)

  private var result: Result = jedis.scan(ScanParams.SCAN_POINTER_START, params)

  override def hasNext: Boolean = ScanParams.SCAN_POINTER_START != result.getStringCursor

  override def next(): Result = {
    val rv = result
    result = jedis.scan(result.getStringCursor, params)
    rv
  }

}

object ScanResultIterator {
  type Result = ScanResult[String]

  def apply(jedis: Jedis, pattern: String): ScanResultIterator = new ScanResultIterator(jedis, pattern)
}
