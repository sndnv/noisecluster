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

class MulticastUdpSpec extends FlatSpec with Matchers {
  private var testDataSent = 0L
  private var testDataReceived = 0L

  private def testDataHandler: (Array[Byte], Int) => Unit = (_: Array[Byte], length: Int) => {
    testDataReceived += length
  }

  private implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private implicit val loggingSystem = ActorSystem("testLoggingSystem")
  private val address = "225.100.50.25"
  private val sourcePort = 49042
  private val targetPort = 49043

  private val source: Source = Source(address, targetPort, sourcePort)
  private val target: Target = Target(address, targetPort, Defaults.BufferSize)

  private val testByteArraySize = 1000

  private var targetFuture = Future {
    target.start(testDataHandler)
  }

  waitUntil(what = "target becomes active", waitTimeMs = 500, waitAttempts = 10) {
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
      target.start(testDataHandler)
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
    target.close()
    target.isActive should be(false)
  }
}
