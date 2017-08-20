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
    /// 
    /// </summary>
    /// <param name="data"></param>
    /// <param name="length"></param>
    public delegate void DataHandler(byte[] data, int length);

    /// <summary>
    /// Base interface for transport targets.
    /// </summary>
    public interface ITarget : IDisposable
    {
        /// <summary>
        /// Begins accepting data, forwarding it via the supplied data handler.
        /// </summary>
        /// <param name="dataHandler">the handler to use for forwarding the received data</param>
        void Start(DataHandler dataHandler);

        /// <summary>
        /// Stops accepting data. Restarting data transmission can be done via a call to <see cref="Start"/>.
        /// </summary>
        void Stop();

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