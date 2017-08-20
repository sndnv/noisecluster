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
  * Base trait for transport sources.
  */
trait Source {
  /**
    * Sends all of the supplied data.
    *
    * @param source the data to send
    */
  def send(source: Array[Byte]): Unit

  /**
    * Sends the specified number of bytes starting from the specified offset.
    *
    * @param source the data to send
    * @param offset the offset to start from
    * @param length the number of bytes to send
    */
  def send(source: Array[Byte], offset: Int, length: Int): Unit

  /**
    * Closes the transport and makes it unavailable for further use.
    */
  def close(): Unit
}
