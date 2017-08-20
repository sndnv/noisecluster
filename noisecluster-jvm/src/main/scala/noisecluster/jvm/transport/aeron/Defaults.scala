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

import java.util.concurrent.TimeUnit

import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import org.agrona.concurrent.{BackoffIdleStrategy, IdleStrategy}

/**
  * Container for various default values used by the Aeron transport subsystem.
  */
object Defaults {
  val IdleStrategy: IdleStrategy = new BackoffIdleStrategy(
    100, //max spins
    10, //max yields
    TimeUnit.MICROSECONDS.toNanos(1), //min park period (ns)
    TimeUnit.MICROSECONDS.toNanos(100) //max park period (ns)
  )

  val FragmentLimit: Int = 10

  val BufferSize: Int = 4096

  val getNewDriverContext: MediaDriver.Context = Contexts.Driver.default

  val getNewSystemContext: Aeron.Context = Contexts.System.default
}
