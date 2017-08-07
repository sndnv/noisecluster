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
    public class Udp : ITransportProvider
    {
        private readonly string _address;
        private readonly int _port;

        public Udp(string address, int port)
        {
            _address = address;
            _port = port;
        }

        public ISource CreateSource()
        {
            return new Source(_address, _port);
        }

        public void Dispose()
        {
        }
    }
}