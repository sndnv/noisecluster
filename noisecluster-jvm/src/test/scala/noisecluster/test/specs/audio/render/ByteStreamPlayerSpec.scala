/**
  * Copyright 2017 https://github.com/sndnv
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package noisecluster.test.specs.audio.render

import akka.actor.ActorSystem
import noisecluster.audio.Defaults
import noisecluster.audio.render.ByteStreamPlayer
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.util.Random

class ByteStreamPlayerSpec extends FlatSpec with Matchers {
  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private implicit val loggingSystem = ActorSystem("testLoggingSystem")
  private val format = Defaults.WasapiAudioFormat
  private val player = ByteStreamPlayer(format)
  private val testByteArraySize = 256 * format.getFrameSize

  "A ByteStreamPlayer" should "successfully start and render audio data" in {
    player.isActive should be(false)
    player.start()
    player.isActive should be(true)

    val bytes = Array.ofDim[Byte](testByteArraySize)
    Random.nextBytes(bytes)
    player.write(bytes, testByteArraySize)
  }

  it should "stop and restart rendering data" in {
    player.isActive should be(true)
    assertThrows[IllegalStateException] {
      player.start()
    }

    player.stop()
    player.isActive should be(false)

    assertThrows[IllegalStateException] {
      player.stop()
    }

    player.start()
    player.isActive should be(true)

    val bytes = Array.ofDim[Byte](testByteArraySize)
    Random.nextBytes(bytes)
    player.write(bytes, testByteArraySize)
  }

  it should "fail to close and active line" in {
    player.isActive should be(true)
    assertThrows[IllegalStateException] {
      player.close()
    }
    player.isActive should be(true)
  }

  it should "stop and close its line" in {
    player.isActive should be(true)
    player.stop()
    player.close()
    player.isActive should be(false)
  }
}
