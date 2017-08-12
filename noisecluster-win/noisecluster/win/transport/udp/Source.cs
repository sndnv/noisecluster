﻿/**
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
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using log4net;

namespace noisecluster.win.transport.udp
{
    public class Source : ISource
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(Source));
        private readonly UdpClient _client;
        private readonly List<IPEndPoint> _targets;
        private readonly int _localPort;
        private readonly bool _isMulticast;

        public Source(List<IPEndPoint> targets, int localPort)
        {
            _client = new UdpClient(localPort);
            _targets = targets;
            _localPort = localPort;
        }

        public Source(IPEndPoint multicastTarget, int localPort)
        {
            _client = new UdpClient(localPort);
            _client.JoinMulticastGroup(multicastTarget.Address);
            _targets = new List<IPEndPoint> {multicastTarget};
            _isMulticast = true;
            _localPort = localPort;
        }

        public Source(List<Tuple<string, int>> targets, int localPort)
            : this(targets.ConvertAll(e => new IPEndPoint(IPAddress.Parse(e.Item1), e.Item2)), localPort)
        {
        }

        public Source(string multicastTargetAddress, int multicastTargetPort, int localPort)
            : this(new IPEndPoint(IPAddress.Parse(multicastTargetAddress), multicastTargetPort), localPort)
        {
        }

        public bool IsActive()
        {
            return true;
        }

        //docs - 'offset' is unused
        public void Send(byte[] source, int offset, int length)
        {
            _targets.ForEach(target => _client.Send(source, length, target));
        }

        public void Send(byte[] source)
        {
            _targets.ForEach(target => _client.Send(source, source.Length, target));
        }

        public void Close()
        {
            _log.InfoFormat("Stopping transport for channel [{0}:{1}]", string.Join(", ", _targets), _localPort);

            if (_isMulticast)
            {
                _client.DropMulticastGroup(_targets[0].Address);
            }

            _client.Dispose();
            _log.InfoFormat("Stopped transport for channel [{0}:{1}]", string.Join(", ", _targets), _localPort);
        }

        public void Dispose()
        {
            Close();
        }
    }
}