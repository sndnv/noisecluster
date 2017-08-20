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

using noisecluster.win.transport;
using noisecluster.win.transport.aeron;

namespace noisecluster.win.interop.providers.transport
{
    /// <summary>
    /// Aeron UDP source transport provider.
    /// </summary>
    public class Aeron : ITransportProvider
    {
        private readonly int _stream;
        private readonly string _address;
        private readonly int _port;
        private readonly int _bufferSize;
        private readonly string _interface;
        private readonly Adaptive.Aeron.Aeron _aeron;

        /// <summary>
        /// Creates a new instance of the provider with the supplied parameters.
        /// </summary>
        /// <param name="systemContext">the Aeron system context to use</param>
        /// <param name="stream">the Aeron stream ID</param>
        /// <param name="address">the address to use for UDP transmission</param>
        /// <param name="port">the port to use for UDP transmission</param>
        /// <param name="bufferSize">the Aeron buffer size</param>
        /// <param name="interface">the local interface to bind to (optional)</param>
        public Aeron(
            Adaptive.Aeron.Aeron.Context systemContext,
            int stream,
            string address,
            int port,
            int bufferSize,
            string @interface
        )
        {
            _stream = stream;
            _address = address;
            _port = port;
            _bufferSize = bufferSize;
            _interface = @interface;
            _aeron = Adaptive.Aeron.Aeron.Connect(systemContext);
        }

        /// <summary>
        /// Creates a new source. Responsibility for disposing of all sources lies with the caller.
        /// </summary>
        /// <returns>the new source</returns>
        public ISource CreateSource()
        {
            return string.IsNullOrEmpty(_interface)
                ? new Source(_aeron, _stream, _address, _port, _bufferSize)
                : new Source(_aeron, _stream, _address, _port, _interface, _bufferSize);
        }

        /// <summary>
        /// Disposes of the internal Aeron connection. Any sources that were created need to disposed by their callers.
        /// </summary>
        public void Dispose()
        {
            _aeron.Dispose();
        }
    }
}