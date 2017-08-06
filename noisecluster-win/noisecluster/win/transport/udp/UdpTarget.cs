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
using System.Net;
using System.Net.Sockets;
using System.Threading;
using log4net;

namespace noisecluster.win.transport.udp
{
    public class UdpTarget : ITarget
    {
        private readonly string _address;
        private readonly int _port;
        private readonly ILog _log = LogManager.GetLogger(typeof(UdpSource));
        private int _isRunning; //0 = false; 1 = true
        private readonly UdpClient _client;
        private readonly IPAddress _group;
        private IPEndPoint _endPoint;

        public UdpTarget(string address, int port)
        {
            _address = address;
            _port = port;

            _endPoint = new IPEndPoint(IPAddress.Any, port);

            _client = new UdpClient {ExclusiveAddressUse = false};
            _client.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            _client.Client.Bind(_endPoint);

            _group = IPAddress.Parse(address);
            _client.JoinMulticastGroup(_group);
        }

        public bool IsActive()
        {
            return _isRunning == 1;
        }

        public void Start(DataHandler dataHandler)
        {
            if (Interlocked.CompareExchange(ref _isRunning, 1, 0) == 0)
            {
                _log.InfoFormat("Starting transport for channel [{0}:{1}]", _address, _port);

                //will block until stopped
                while (_isRunning == 1)
                {
                    var buffer = _client.Receive(ref _endPoint);
                    dataHandler(buffer, buffer.Length);
                }

                _log.InfoFormat("Stopped transport for channel [{0}:{1}]", _address, _port);
            }
            else
            {
                var message = string.Format(
                    "Cannot start transport for channel [{0}:{1}]; transport is already active",
                    _address,
                    _port
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Stop()
        {
            if (Interlocked.CompareExchange(ref _isRunning, 0, 1) == 1)
            {
                _log.InfoFormat("Stopping transport for channel [{0}:{1}]", _address, _port);
            }
            else
            {
                var message = string.Format(
                    "Cannot stop transport for channel [{0}:{1}]; transport is not active",
                    _address,
                    _port
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Close()
        {
            if (_isRunning == 0)
            {
                _log.InfoFormat("Closing transport for channel [{0}:{1}]", _address, _port);
                _client.DropMulticastGroup(_group);
                _client.Dispose();
                _log.InfoFormat("Closed transport for channel [{0}:{1}]", _address, _port);
            }
            else
            {
                var message = string.Format(
                    "Cannot close transport for channel [{0}:{1}]; transport is still active",
                    _address,
                    _port
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Dispose()
        {
            Close();
        }
    }
}