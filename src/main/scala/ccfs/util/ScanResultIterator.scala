package ccfs.util

import ccfs.util.ScanResultIterator.Result
import redis.clients.jedis.{Jedis, ScanParams, ScanResult}

class ScanResultIterator(jedis: Jedis, pattern: String) extends Iterator[Result] {
  val params = new ScanParams
  params.`match`(pattern)

  private var result: Option[Result] = None

  override def hasNext: Boolean =
    result.map(res => ScanParams.SCAN_POINTER_START != res.getStringCursor).getOrElse(true)

  override def next(): Result = result match {
    case Some(res) =>
      result = Option(jedis.scan(res.getStringCursor, params))
      res

    case None =>
      val res = jedis.scan(ScanParams.SCAN_POINTER_START, params)
      result = Option(res)
      res
  }
}

object ScanResultIterator {
  type Result = ScanResult[String]

  def apply(jedis: Jedis, pattern: String): ScanResultIterator = new ScanResultIterator(jedis, pattern)
}
