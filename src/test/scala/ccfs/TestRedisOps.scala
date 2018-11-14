package ccfs

import ccfs.RedisOps.{Hash, _}
import org.scalacheck.{Gen, Prop, Properties}
import scalaz.std.list._
import scalaz.{Applicative, Traverse}

import scala.collection.immutable.SortedMap

object TestRedisOps extends Properties("RedisOps") {

  implicit val applicativeGen = new Applicative[Gen] {
    override def point[A](a: => A): Gen[A] = Gen.const(a)

    override def ap[A, B](fa: => Gen[A])(f: => Gen[A => B]): Gen[B] = f.flatMap(fa.map)
  }

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

  // Obtain a ZPLMap Generator that generates entries with the given prefix with length given by the passed Generator
  // Return a tuple of those values
  private def withPrefix(prefix: String, len: Gen[Int]): (String, Gen[ZPLMap]) =
    prefix -> len.flatMap(n => Gen.listOfN(n, hash(prefix))).map(hashes => toMap(hashes, "zpl"))

  // A list of the above
  private def withPrefixes(prefixes: List[String], len: Gen[Int]): List[(String, Gen[ZPLMap])] =
    prefixes.map(p => withPrefix(p, len))

  // Pull the Gen out from the ZPLMap having it apply to the containing List instead
  private def sequence(tuples: List[(String, Gen[ZPLMap])]): Gen[List[(String, ZPLMap)]] =
    Traverse[List].sequence(tuples.map { case (p, g) => g.flatMap(map => Gen.const(p -> map)) })

  // Finally, combine methods above to give us a Generator for testing
  def genTuples(prefixes: List[String], len: Gen[Int]): Gen[List[(String, ZPLMap)]] =
    sequence(withPrefixes(prefixes, len))


  property("valid hashes should convert properly to map entries") =
    Prop.forAll(hashes(20))(hashes => {
    val entries = toMapEntries(hashes, "zpl")
    entries.length == hashes.length
  })

  property("hash entries should be sorted by keys") =
    Prop.forAll(hashes(20))(hashes => {
    val map = toMap(hashes, "zpl")
    val keys = map.keySet.toList
    keys == map.keys.toList.sorted
  })

  property("matches should return the correct number of results") =
    Prop.forAll(genTuples(zplPrefixes, Gen.choose(1, 30)))(tuples => {
      // Combine all the maps into one
      val map = tuples.foldLeft(SortedMap.empty[String, Hash])((acc, tuple) => tuple._2 ++ acc)

      // Collect our test criteria. Namely, the prefix and how many instances should be found with each
      val crit = tuples.map { case (p, m) => p -> m.size }

      crit.forall { case (p, n) => matches(map, p).size == n }
    })
}
