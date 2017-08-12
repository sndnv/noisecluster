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
package noisecluster.test.specs.transport.udp

import akka.actor.ActorSystem
import noisecluster.jvm.test.utils._
import noisecluster.jvm.transport.udp.{Defaults, Source, Target}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UnicastUdpSpec extends FlatSpec with Matchers {
  private var testDataSent = 0L
  private var testDataReceived = 0L

  private def testDataHandler: (Array[Byte], Int) => Unit = (_: Array[Byte], length: Int) => {
    testDataReceived += length
  }

  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private implicit val loggingSystem = ActorSystem("testLoggingSystem")
  private val address = "localhost"
  private val sourcePort = 49142
  private val targetPort_01 = 49143
  private val targetPort_02 = 49144
  private val targetPort_03 = 49145

  private val source: Source = Source(Seq((address, targetPort_01), (address, targetPort_02), (address, targetPort_03)), sourcePort)
  private val target_01: Target = Target(address, targetPort_01, Defaults.BufferSize)
  private val target_02: Target = Target(address, targetPort_02, Defaults.BufferSize)
  private val target_03: Target = Target(address, targetPort_03, Defaults.BufferSize)

  private val testByteArraySize = 1000

  private var targetFuture_01 = Future {
    target_01.start(testDataHandler)
  }

  private val targetFuture_02 = Future {
    target_02.start(testDataHandler)
  }

  private val targetFuture_03 = Future {
    target_03.start(testDataHandler)
  }

  waitUntil(what = "target becomes active", waitTimeMs = 500, waitAttempts = 10) {
    target_01.isActive && target_02.isActive && target_03.isActive
  }

  "A source and a target" should "successfully exchange data" in {
    val bytes = Array.ofDim[Byte](testByteArraySize)
    Random.nextBytes(bytes)
    source.send(bytes)
    testDataSent += testByteArraySize

    waitUntil(what = "data is received by target", waitTimeMs = 500, waitAttempts = 10) {
      testDataSent * 3 == testDataReceived
    }

    testDataSent * 3 should be(testDataReceived)
    testDataReceived should be(testByteArraySize * 3)
  }

  "A target" should "successfully stop accepting data" in {
    target_01.isActive should be(true)
    target_02.isActive should be(true)
    target_03.isActive should be(true)
    target_01.stop()

    waitUntil(what = "target becomes inactive", waitTimeMs = 500, waitAttempts = 10) {
      !target_01.isActive
    }

    target_01.isActive should be(false)

    assertThrows[IllegalStateException] {
      target_01.stop()
    }
  }

  it should "successfully restart and accept data" in {
    target_01.isActive should be(false)
    targetFuture_01 = Future {
      target_01.start(testDataHandler)
    }

    waitUntil(what = "target becomes active", waitTimeMs = 500, waitAttempts = 10) {
      target_01.isActive
    }

    target_01.isActive should be(true)

    val bytes = Array.ofDim[Byte](testByteArraySize)
    Random.nextBytes(bytes)
    source.send(bytes)
    testDataSent += testByteArraySize

    waitUntil(what = "data is received by target", waitTimeMs = 500, waitAttempts = 10) {
      testDataSent * 3 == testDataReceived
    }

    testDataSent * 3 should be(testDataReceived)
    testDataReceived should be(testByteArraySize * 6)
  }

  it should "fail to close an active connection" in {
    assertThrows[IllegalStateException] {
      target_01.close()
    }
  }

  it should "successfully stop and close its connection" in {
    target_01.isActive should be(true)
    target_02.isActive should be(true)
    target_03.isActive should be(true)
    target_01.stop()
    target_02.stop()
    target_03.stop()
    target_01.close()
    target_02.close()
    target_03.close()
    target_01.isActive should be(false)
    target_02.isActive should be(false)
    target_03.isActive should be(false)
  }
}
