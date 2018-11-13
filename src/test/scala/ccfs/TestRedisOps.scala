package ccfs

import ccfs.RedisOps.{Hash, _}
import org.scalacheck.{Gen, Prop, Properties}

object TestRedisOps extends Properties("RedisOps") {

  val zplPrefixes: List[String] = List("ZPL", "ZSA")

  def session(): Gen[String] = Gen.uuid.map(_.toString.toUpperCase)

  def zpl(prefix: String): Gen[String] = for {
    p <- Gen.const(prefix)
    num <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
    char <- Gen.alphaUpperChar
  } yield p + num + char


  def zpl(): Gen[String] = Gen.oneOf(zplPrefixes).flatMap(p => zpl(p))


  def hash(prefix: String): Gen[Hash] = for {
    zpl <- zpl(prefix).map(s => "zpl" -> s)
    session <- session().map(s => "sessionId" -> s)
    iv <- Gen.const("interfaceVersion").map(s => "interfaceVersion" -> s)
    ct <- Gen.const("clientType").map(s => "clientType" -> s)
  } yield Map(zpl, session, iv, ct)

  def hash(): Gen[Hash] = Gen.oneOf(zplPrefixes).flatMap(p => hash(p))

  def hashes(prefix: String, maxLen: Int): Gen[List[Hash]] = Gen.choose(1, maxLen).flatMap(n => Gen.listOfN(n, hash(prefix)))

  def hashes(maxLen: Int): Gen[List[Hash]] = Gen.oneOf(zplPrefixes).flatMap(p => hashes(p, maxLen))

  private def hashesWithCounts(prefixes: List[String], max: Int): Gen[List[(Hash, Int)]] = {
    val prefixesWithCounts = prefixes.map(p => Gen.const(p).flatMap(s => Gen.choose(1, max).map(i => s -> i)))
    Gen.sequence(prefixesWithCounts.map(g => g.flatMap { case (p, n) => Gen.listOfN(n, hash(p).map(h => h -> n)) }))
  }


  property("valid hashes should convert properly to map entries") = Prop.forAll(hashes(20))(hashes => {
    val entries = toMapEntries(hashes, "zpl")
    entries.length == hashes.length
  })

  property("hash entries should be sorted by keys") = Prop.forAll(hashes(20))(hashes => {
    val map = keysToMap(hashes, "zpl")
    val keys = map.keySet.toList
    keys == map.keys.toList.sorted
  })

  property("matches should return the correct number of results") =
    Prop.forAll(prefixWithCount(zplPrefixes, 20))(tuples => {
      val maps = tuples.map { case (pre, count) => hashes() }
    })
}
