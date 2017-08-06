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
using System.Threading;
using Adaptive.Aeron;
using Adaptive.Agrona.Concurrent;
using log4net;

namespace noisecluster.win.transport.aeron
{
    public class AeronTarget : IDisposable
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(AeronTarget));
        private int _isRunning; //0 = false; 1 = true
        private readonly int _stream;
        private readonly string _channel;
        private readonly Subscription _subscription;
        private readonly IIdleStrategy _idleStrategy;
        private readonly int _fragmentLimit;

        public bool IsActive
        {
            get { return _isRunning == 1; }
        }

        public delegate void DataHandler(byte[] data, int length);

        public AeronTarget(
            Aeron aeron,
            int stream,
            string channel,
            IIdleStrategy idleStrategy,
            int fragmentLimit
        )
        {
            _stream = stream;
            _channel = channel;
            _subscription = aeron.AddSubscription(_channel, _stream);
            _idleStrategy = idleStrategy;
            _fragmentLimit = fragmentLimit;
        }

        //docs - warn about blocking
        public void Start(DataHandler dataHandler)
        {
            if (Interlocked.CompareExchange(ref _isRunning, 1, 0) == 0)
            {
                _log.InfoFormat("Starting transport for channel [{0}] and stream [{1}]", _channel, _stream);
                var fragmentAssembler = new FragmentAssembler(
                    (buffer, offset, length, _) =>
                    {
                        var data = new byte[length];
                        buffer.GetBytes(offset, data);
                        dataHandler(data, length);
                    }
                );

                //will block until stopped
                while (_isRunning == 1)
                {
                    var fragmentsRead = _subscription.Poll(fragmentAssembler.OnFragment, _fragmentLimit);
                    _idleStrategy.Idle(fragmentsRead);
                }

                _log.InfoFormat("Stopped transport for channel [{0}] and stream [{1}]", _channel, _stream);
            }
            else
            {
                var message = string.Format(
                    "Cannot start transport for channel [{0}] and stream [{1}]; transport is already active",
                    _channel,
                    _stream
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Stop()
        {
            if (Interlocked.CompareExchange(ref _isRunning, 0, 1) == 1)
            {
                _log.InfoFormat("Stopping transport for channel [{0}] and stream [{1}]", _channel, _stream);
            }
            else
            {
                var message = string.Format(
                    "Cannot stop transport for channel [{0}] and stream [{1}]; transport is not active",
                    _channel,
                    _stream
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Close()
        {
            if (_isRunning == 0)
            {
                _log.InfoFormat("Closing transport for channel [{0}] and stream [{1}]", _channel, _stream);
                _subscription.Dispose();
                _log.InfoFormat("Closed transport for channel [{0}] and stream [{1}]", _channel, _stream);
            }
            else
            {
                var message = string.Format(
                    "Cannot close transport for channel [{0}] and stream [{1}]; transport is still active",
                    _channel,
                    _stream
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Dispose()
        {
            _subscription.Dispose();
        }
    }
}