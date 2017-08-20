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
package noisecluster.jvm.transport

/**
  * Base trait for transport targets.
  */
trait Target {
  /**
    * Begins accepting data, forwarding it via the supplied data handler.
    *
    * @note Specific implementations may block on this call until the target is stopped.
    *
    * @param dataHandler the handler to use for forwarding the received data
    * @see [[noisecluster.jvm.transport.Target#stop]]
    */
  def start(dataHandler: (Array[Byte], Int) => Unit): Unit

  /**
    * Stops accepting data.
    *
    * @note The target can begin accepting data via a call to [[noisecluster.jvm.transport.Target#start]].
    *
    * @see [[noisecluster.jvm.transport.Target#start]]
    * @see [[noisecluster.jvm.transport.Target#close]]
    */
  def stop(): Unit

  /**
    * Closes the transport and makes it unavailable for further use.
    *
    * @note Only a transport that is not running can be closed.
    *
    * @see [[noisecluster.jvm.transport.Target#stop]]
    */
  def close(): Unit

  /**
    * Checks if the transport is active.
    *
    * @return true, if the transport is active
    */
  def isActive: Boolean
}
