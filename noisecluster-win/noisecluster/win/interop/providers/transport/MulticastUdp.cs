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
using noisecluster.win.transport.udp;

namespace noisecluster.win.interop.providers.transport
{
    /// <summary>
    /// Multicast UDP source transport provider.
    /// </summary>
    public class MulticastUdp : ITransportProvider
    {
        private readonly string _multicastTargetAddress;
        private readonly int _multicastTargetPort;
        private readonly int _localPort;

        /// <summary>
        /// Creates a new instace of the provider with the supplied parameters.
        /// </summary>
        /// <param name="multicastTargetAddress">the multicast address to use</param>
        /// <param name="multicastTargetPort">the multicast port to use</param>
        /// <param name="localPort">the local port to bind to</param>
        public MulticastUdp(string multicastTargetAddress, int multicastTargetPort, int localPort)
        {
            _multicastTargetAddress = multicastTargetAddress;
            _multicastTargetPort = multicastTargetPort;
            _localPort = localPort;
        }

        /// <summary>
        /// Creates a new source. Responsibility for disposing of all sources lies with the caller.
        /// </summary>
        /// <returns>the new source</returns>
        public ISource CreateSource()
        {
            return new Source(_multicastTargetAddress, _multicastTargetPort, _localPort);
        }

        /// <summary>
        /// Does nothing. Any sources that were created need to disposed by their callers.
        /// </summary>
        public void Dispose()
        {
        }
    }
}