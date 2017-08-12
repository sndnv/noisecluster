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
    public class UnicastUdp : ITransportProvider
    {
        private readonly int _localPort;
        private List<IPEndPoint> _targets;

        public UnicastUdp(int localPort)
        {
            _localPort = localPort;
            _targets = new List<IPEndPoint>();
        }

        public void AddTarget(string targetAddress, int targetPort)
        {
            _targets.Add(new IPEndPoint(IPAddress.Parse(targetAddress), targetPort));
        }

        public ISource CreateSource()
        {
            return new Source(_targets, _localPort);
        }

        public void Dispose()
        {
        }
    }
}