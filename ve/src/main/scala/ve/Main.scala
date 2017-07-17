package ve

import io.aeron.driver.MediaDriver
import noisecluster.audio.render.ByteStreamPlayer
import noisecluster.transport.aeron.{Contexts, Target}

import scala.util.control.NonFatal

object Main {
  def main(args: Array[String]): Unit = {
    val driver = MediaDriver.launch(Contexts.Driver.default)
    val player = ByteStreamPlayer()
    val target = Target(stream = 42, address = "???", port = 42123, player.enqueueData)

    try {
      target.start()
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        println(e)
    } finally {
      target.stop()
      target.close()
      player.stop()
      driver.close()
    }
  }
}
