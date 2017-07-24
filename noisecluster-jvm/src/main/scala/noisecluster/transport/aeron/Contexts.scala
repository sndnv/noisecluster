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

import io.aeron.Aeron
import io.aeron.driver.{MediaDriver, ThreadingMode}
import org.agrona.concurrent.BusySpinIdleStrategy

object Contexts {

  object Driver {
    //docs - taken from https://github.com/real-logic/aeron/blob/master/aeron-samples/src/main/java/io/aeron/samples/LowLatencyMediaDriver.java
    def lowLatency: MediaDriver.Context = {
      new MediaDriver.Context()
        .termBufferSparseFile(false)
        .threadingMode(ThreadingMode.DEDICATED)
        .conductorIdleStrategy(new BusySpinIdleStrategy())
        .receiverIdleStrategy(new BusySpinIdleStrategy())
        .senderIdleStrategy(new BusySpinIdleStrategy())
        .dirsDeleteOnStart(true)
    }

    def default: MediaDriver.Context = {
      new MediaDriver.Context().dirsDeleteOnStart(true)
    }
  }

  object System {
    def default: Aeron.Context = {
      new Aeron.Context
    }
  }

}
