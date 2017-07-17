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
package noisecluster.transport.aeron

import java.util.concurrent.atomic.AtomicBoolean

import io.aeron._
import io.aeron.logbuffer._
import org.agrona.DirectBuffer
import org.agrona.concurrent._

//TODO - logging
class Target(
  private val stream: Int,
  private val aeronChannel: String,
  private val context: Aeron.Context,
  private val dataHandler: (Array[Byte], Int) => Unit,
  private val idleStrategy: IdleStrategy,
  private val fragmentLimit: Int
) {
  private val isRunning = new AtomicBoolean(false)

  private val fragmentHandler = new FragmentHandler {
    override def onFragment(buffer: DirectBuffer, offset: Int, length: Int, header: Header): Unit = {
      val data = new Array[Byte](length)
      buffer.getBytes(offset, data)
      dataHandler(data, length)
    }
  }

  private val fragmentAssembler = new FragmentAssembler(fragmentHandler)

  private val aeron = Aeron.connect(context)
  private val subscription = aeron.addSubscription(aeronChannel, stream)

  //TODO - warn about blocking
  def start(): Unit = {
    if(isRunning.compareAndSet(false, true)) {
      //TODO - log
      while(isRunning.get) {
        val fragmentsRead = subscription.poll(fragmentAssembler, fragmentLimit)
        idleStrategy.idle(fragmentsRead)
      }
      //TODO - log
    } else {
      //TODO - log warning - is already running
    }
  }

  def stop(): Unit = {
    //TODO
    if(isRunning.compareAndSet(true, false)) {
      //TODO - ?
    } else {
      //TODO - log warning - is not running
    }
  }

  def close(): Unit = {
    if(isRunning.get) {
      subscription.close()
      aeron.close()
    } else {
      //TODO - log error - is running
    }
  }
}

object Target {
  def apply(
    stream: Int,
    address: String,
    port: Int,
    context: Aeron.Context,
    dataHandler: (Array[Byte], Int) => Unit,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  ): Target = new Target(stream, s"aeron:udp?endpoint=$address:$port", context, dataHandler, idleStrategy, fragmentLimit)

  def apply(
    stream: Int,
    address: String,
    port: Int,
    interface: String,
    context: Aeron.Context,
    dataHandler: (Array[Byte], Int) => Unit,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  ): Target = new Target(stream, s"aeron:udp?endpoint=$address:$port|interface=$interface", context, dataHandler, idleStrategy, fragmentLimit)

  def apply(
    stream: Int,
    aeronChannel: String,
    context: Aeron.Context,
    dataHandler: (Array[Byte], Int) => Unit,
    idleStrategy: IdleStrategy,
    fragmentLimit: Int
  ): Target = new Target(stream, aeronChannel, context, dataHandler, idleStrategy, fragmentLimit)

  def apply(stream: Int, address: String, port: Int, dataHandler: (Array[Byte], Int) => Unit): Target =
    Target(
      stream,
      address,
      port,
      Contexts.System.default,
      dataHandler,
      IdleStrategies.default,
      FragmentLimits.default
    )
}
