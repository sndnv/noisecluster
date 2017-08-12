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
    public class Target : ITarget
    {
        private readonly IPAddress _address;
        private readonly int _localPort;
        private readonly ILog _log = LogManager.GetLogger(typeof(Target));
        private int _isRunning; //0 = false; 1 = true
        private readonly UdpClient _client;
        private IPEndPoint _endPoint;

        public Target(int localPort, string address = null)
        {
            _localPort = localPort;

            _endPoint = new IPEndPoint(IPAddress.Any, localPort);

            _client = new UdpClient {ExclusiveAddressUse = false};
            _client.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            _client.Client.Bind(_endPoint);

            if (!string.IsNullOrEmpty(address))
            {
                _address = IPAddress.Parse(address);
                //assumes that if an address is supplied then it is multicast, because
                //determining if it actually is multicast (in a sensible way) in C# is just... wow
                _client.JoinMulticastGroup(_address);
            }
        }

        public bool IsActive()
        {
            return _isRunning == 1;
        }

        public void Start(DataHandler dataHandler)
        {
            if (Interlocked.CompareExchange(ref _isRunning, 1, 0) == 0)
            {
                _log.InfoFormat("Starting transport for channel [{0}:{1}]", _address, _localPort);

                //will block until stopped
                while (_isRunning == 1)
                {
                    var buffer = _client.Receive(ref _endPoint);
                    dataHandler(buffer, buffer.Length);
                }

                _log.InfoFormat("Stopped transport for channel [{0}:{1}]", _address, _localPort);
            }
            else
            {
                var message = string.Format(
                    "Cannot start transport for channel [{0}:{1}]; transport is already active",
                    _address,
                    _localPort
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Stop()
        {
            if (Interlocked.CompareExchange(ref _isRunning, 0, 1) == 1)
            {
                _log.InfoFormat("Stopping transport for channel [{0}:{1}]", _address, _localPort);
            }
            else
            {
                var message = string.Format(
                    "Cannot stop transport for channel [{0}:{1}]; transport is not active",
                    _address,
                    _localPort
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Close()
        {
            if (_isRunning == 0)
            {
                _log.InfoFormat("Closing transport for channel [{0}:{1}]", _address, _localPort);

                if (_address != null)
                {
                    _client.DropMulticastGroup(_address);
                }

                _client.Dispose();
                _log.InfoFormat("Closed transport for channel [{0}:{1}]", _address, _localPort);
            }
            else
            {
                var message = string.Format(
                    "Cannot close transport for channel [{0}:{1}]; transport is still active",
                    _address,
                    _localPort
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