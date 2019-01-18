package ccfs.util

import java.io._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Exec {

  private def readStream(is: InputStream): Future[String] = Future {
    val reader = new BufferedReader(new InputStreamReader(is))

    def loop(line: String): List[String] =
      if (null == line) Nil else line :: loop(reader.readLine())

    loop(reader.readLine()).mkString("\n")
  }

  // Returns exit code, stdout, stderr
  def exec(cmd: String): (Int, String, String) = {
    val process = Runtime.getRuntime.exec(cmd)
    try {
      val fin = readStream(process.getInputStream)
      val fer = readStream(process.getErrorStream)

      val (out, err) = (Await.result(fin, Duration.Inf),
        Await.result(fer, Duration.Inf))

      (process.waitFor(), out, err)
    } finally {
      process.getErrorStream.close()
      process.getInputStream.close()
      process.getOutputStream.close()
      process.destroy()
    }
  }

  def main(args: Array[String]): Unit = {
    //val cmd = "netstat -a | grep ESTABLISHED | grep 34-206"

    val cmd = "netstat"

    val (ret, out, error) = exec(cmd)

    println(s"errors: $error")

    println(s"output: $out")

    println(s"return code: $ret")

  }
}
