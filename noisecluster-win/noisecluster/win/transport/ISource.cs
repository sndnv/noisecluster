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

using System;

namespace noisecluster.win.transport
{
    /// <summary>
    /// Base interface for transport sources.
    /// </summary>
    public interface ISource : IDisposable
    {
        /// <summary>
        /// Sends the specified number of bytes starting from the specified offset.
        /// </summary>
        /// <param name="source">the data to send</param>
        /// <param name="offset">the offset to start from</param>
        /// <param name="length">the number of bytes to send</param>
        void Send(byte[] source, int offset, int length);

        /// <summary>
        /// Sends all of the supplied data.
        /// </summary>
        /// <param name="source">the data to send</param>
        void Send(byte[] source);

        /// <summary>
        /// Closes the transport and makes it unavailable for further use.
        /// </summary>
        void Close();

        /// <summary>
        /// Retrieves the state of the transport.
        /// </summary>
        /// <returns>true, if the transport is active</returns>
        bool IsActive();
    }
}