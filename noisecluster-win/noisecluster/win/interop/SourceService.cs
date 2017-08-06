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
using log4net;
using log4net.Config;
using noisecluster.win.audio.capture;
using noisecluster.win.transport.aeron;

namespace noisecluster.win.interop
{
    public class SourceService : IDisposable
    {
        private readonly int _stream;
        private readonly string _address;
        private readonly int _port;
        private readonly int _bufferSize;
        private readonly string _interface;
        private readonly ILog _log = LogManager.GetLogger(typeof(SourceService));
        private readonly Aeron _aeron;
        private WasapiRecorder _audio;
        private Source _transport;
        private int _isTransportRunning; //0 = false; 1 = true
        private readonly WasapiRecorder.DataHandler _dataHandler;

        public SourceService(
            Aeron.Context systemContext,
            int stream,
            string address,
            int port,
            int bufferSize,
            string @interface = null,
            bool withDebugingHandler = false
        )
        {
            BasicConfigurator.Configure();
            _aeron = Aeron.Connect(systemContext);

            _stream = stream;
            _address = address;
            _port = port;
            _bufferSize = bufferSize;
            _interface = @interface;
            _isTransportRunning = 0;

            if (withDebugingHandler)
            {
                _dataHandler = (data, length) =>
                {
                    _log.DebugFormat(
                        "Captured [{0}] bytes of audio data; transport is [{1}], [{2}] and [{3}]; ",
                        length,
                        _isTransportRunning == 1 ? "running" : "not running",
                        _transport.IsConnected ? "connected" : "not connected",
                        _transport.IsClosed ? "closed" : "not closed"
                    );

                    if (_isTransportRunning == 1 && _transport.IsConnected)
                    {
                        var result = _transport.Send(data, 0, length);
                        _log.DebugFormat("Sent [{0}] bytes of audio data; result was [{1}]", length, result);
                    }
                };
            }
            else
            {
                _dataHandler = (data, length) =>
                {
                    if (_isTransportRunning == 1 && _transport.IsConnected)
                    {
                        _transport.Send(data, 0, length);
                    }
                };
            }
        }

        public SourceService(int stream, string address, int port, int bufferSize, string @interface = null,
            bool withDebugingHandler = false)
            : this(Defaults.GetNewSystemContext(), stream, address, port, bufferSize, @interface, withDebugingHandler)
        {
        }

        public SourceService(int stream, string address, int port, string @interface = null,
            bool withDebugingHandler = false)
            : this(stream, address, port, Defaults.BufferSize, @interface, withDebugingHandler)
        {
        }

        public bool IsAudioActive
        {
            get { return _audio != null && _audio.IsActive; }
        }

        public bool IsTransportActive
        {
            get { return _transport != null && _isTransportRunning == 1; }
        }

        public bool IsTransportConnected
        {
            get { return _transport != null && _transport.IsConnected; }
        }

        public bool IsTransportClosed
        {
            get { return _transport != null && _transport.IsClosed; }
        }

        public bool StartAudio()
        {
            if (_audio != null || _transport == null) return false;

            _audio = new WasapiRecorder(_dataHandler);
            _audio.Start();
            return true;
        }

        public bool StopAudio()
        {
            if (_audio == null) return false;

            _audio.Dispose();
            _audio = null;
            return true;
        }

        public bool StartTransport()
        {
            if (Interlocked.CompareExchange(ref _isTransportRunning, 1, 0) != 0) return false;

            _transport = string.IsNullOrEmpty(_interface)
                ? new Source(_aeron, _stream, _address, _port, _bufferSize)
                : new Source(_aeron, _stream, _address, _port, _interface, _bufferSize);
            return true;
        }

        public bool StopTransport()
        {
            if (Interlocked.CompareExchange(ref _isTransportRunning, 0, 1) != 1) return false;

            _transport.Dispose();
            _transport = null;
            return true;
        }

        public void Dispose()
        {
            StopAudio();
            StopTransport();
            _aeron.Dispose();
        }
    }
}