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

using System.Collections.Generic;
using System.Net;
using noisecluster.win.transport;
using noisecluster.win.transport.udp;

namespace noisecluster.win.interop.providers.transport
{
    /// <summary>
    /// Unicast UDP source transport provider.
    /// </summary>
    public class UnicastUdp : ITransportProvider
    {
        private readonly int _localPort;
        private readonly List<IPEndPoint> _targets;

        /// <summary>
        /// Creates a new instance of the provider with the specified local port to be used for transmission.
        /// </summary>
        /// <param name="localPort">the local port to bind to</param>
        public UnicastUdp(int localPort)
        {
            _localPort = localPort;
            _targets = new List<IPEndPoint>();
        }

        /// <summary>
        /// Adds the supplied address and port to the list of the provider's targets. Adding a target after a source
        /// has been created will NOT add it to that source's targets list.
        /// </summary>
        /// <param name="targetAddress">the new target address</param>
        /// <param name="targetPort">the new target port</param>
        public void AddTarget(string targetAddress, int targetPort)
        {
            _targets.Add(new IPEndPoint(IPAddress.Parse(targetAddress), targetPort));
        }

        /// <summary>
        /// Creates a new source with the current list of targets. Responsibility for disposing of all sources lies
        /// with the caller.
        /// </summary>
        /// <returns>the new source</returns>
        public ISource CreateSource()
        {
            return new Source(_targets, _localPort);
        }

        /// <summary>
        /// Does nothing. Any sources that were created need to disposed by their callers.
        /// </summary>
        public void Dispose()
        {
        }
    }
}