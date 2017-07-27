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
package noisecluster.test.specs.transport.aeron

import akka.actor.ActorSystem
import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import noisecluster.test.utils._
import noisecluster.transport.aeron.{Defaults, Source, Target}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class BasicSpec extends FlatSpec with Matchers {
  private var testDataSent = 0L
  private var testDataReceived = 0L

  private def testDataHandler: (Array[Byte], Int) => Unit = (_: Array[Byte], length: Int) => {
    testDataReceived += length
  }

  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private implicit val loggingSystem = ActorSystem("testLoggingSystem")
  private val channel = "aeron:ipc"
  private val stream = 42
  private val driver = MediaDriver.launch(Defaults.getNewDriverContext)
  private implicit val aeron = Aeron.connect(Defaults.getNewSystemContext)

  private val source: Source = Source(stream, channel, Defaults.BufferSize)
  private val target: Target = Target(stream, channel, testDataHandler, Defaults.IdleStrategy, Defaults.FragmentLimit)

  private val testByteArraySize = 1000

  private var targetFuture = Future {
    target.start()
  }

  waitUntil(what = "target becomes inactive", waitTimeMs = 500, waitAttempts = 10) {
    target.isActive
  }

  "A source and a target" should "successfully exchange data" in {
    val bytes = Array.ofDim[Byte](testByteArraySize)
    Random.nextBytes(bytes)
    source.send(bytes)
    testDataSent += testByteArraySize

    waitUntil(what = "data is received by target", waitTimeMs = 500, waitAttempts = 10) {
      testDataSent == testDataReceived
    }

    testDataSent should be(testDataReceived)
    testDataReceived should be(testByteArraySize)
  }

  "A target" should "successfully stop accepting data" in {
    target.isActive should be(true)
    target.stop()

    waitUntil(what = "target becomes inactive", waitTimeMs = 500, waitAttempts = 10) {
      !target.isActive
    }

    target.isActive should be(false)

    assertThrows[IllegalStateException] {
      target.stop()
    }
  }

  it should "successfully restart and accept data" in {
    target.isActive should be(false)
    targetFuture = Future {
      target.start()
    }

    waitUntil(what = "target becomes active", waitTimeMs = 500, waitAttempts = 10) {
      target.isActive
    }

    target.isActive should be(true)

    val bytes = Array.ofDim[Byte](testByteArraySize)
    Random.nextBytes(bytes)
    source.send(bytes)
    testDataSent += testByteArraySize

    waitUntil(what = "data is received by target", waitTimeMs = 500, waitAttempts = 10) {
      testDataSent == testDataReceived
    }

    testDataSent should be(testDataReceived)
    testDataReceived should be(testByteArraySize * 2)
  }

  it should "fail to close an active connection" in {
    assertThrows[IllegalStateException] {
      target.close()
    }
  }

  it should "successfully stop and close its connection" in {
    target.isActive should be(true)
    target.stop()

    waitUntil(what = "target becomes inactive", waitTimeMs = 500, waitAttempts = 10) {
      !target.isActive
    }

    target.close()
    target.isActive should be(false)
  }

  "A system" should "successfully terminate" in {
    aeron.close()
    driver.close()
  }
}
