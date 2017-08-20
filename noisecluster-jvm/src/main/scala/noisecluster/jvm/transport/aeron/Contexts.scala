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
package noisecluster.jvm.transport.aeron

import io.aeron.Aeron
import io.aeron.driver.{MediaDriver, ThreadingMode}
import org.agrona.concurrent.BusySpinIdleStrategy

/**
  * Container for various contexts used by the Aeron transport subsystem.
  */
object Contexts {

  /**
    * Aeron driver contexts
    */
  object Driver {
    /**
      * Creates a new low-latency media driver context based on
      * <a href='https://github.com/real-logic/aeron/blob/master/aeron-samples/src/main/java/io/aeron/samples/LowLatencyMediaDriver.java'>LowLatencyMediaDriver (Github link)</a>.
      *
      * @return a new low-latency driver context
      */
    def lowLatency: MediaDriver.Context = {
      new MediaDriver.Context()
        .termBufferSparseFile(false)
        .threadingMode(ThreadingMode.DEDICATED)
        .conductorIdleStrategy(new BusySpinIdleStrategy())
        .receiverIdleStrategy(new BusySpinIdleStrategy())
        .senderIdleStrategy(new BusySpinIdleStrategy())
        .dirsDeleteOnStart(true)
    }

    /**
      * Creates a new media driver context.
      *
      * @note Deletes the Aeron directories on startup.
      * @return a new default driver context
      * @see [[io.aeron.driver.MediaDriver.Context#dirsDeleteOnStart(boolean)]]
      */
    def default: MediaDriver.Context = {
      new MediaDriver.Context().dirsDeleteOnStart(true)
    }
  }

  /**
    * Aeron system contexts
    */
  object System {
    def default: Aeron.Context = {
      new Aeron.Context
    }
  }

}
