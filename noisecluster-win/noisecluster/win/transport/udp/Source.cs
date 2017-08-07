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

using System.Net;
using System.Net.Sockets;
using log4net;

namespace noisecluster.win.transport.udp
{
    public class Source : ISource
    {
        private readonly string _address;
        private readonly int _port;
        private readonly ILog _log = LogManager.GetLogger(typeof(Source));
        private readonly UdpClient _client;
        private readonly IPAddress _group;
        private readonly IPEndPoint _endPoint;

        public Source(string address, int port)
        {
            _address = address;
            _port = port;

            _client = new UdpClient();
            _group = IPAddress.Parse(address);
            _client.JoinMulticastGroup(_group);
            _endPoint = new IPEndPoint(_group, port);
        }

        public bool IsActive()
        {
            return true;
        }

        //docs - 'offset' is unused
        public void Send(byte[] source, int offset, int length)
        {
            _client.Send(source, length, _endPoint);
        }

        public void Send(byte[] source)
        {
            _client.Send(source, source.Length, _endPoint);
        }

        public void Close()
        {
            _log.InfoFormat("Stopping transport for channel [{0}:{1}]", _address, _port);
            _client.DropMulticastGroup(_group);
            _client.Dispose();
            _log.InfoFormat("Stopped transport for channel [{0}:{1}]", _address, _port);
        }

        public void Dispose()
        {
            Close();
        }
    }
}