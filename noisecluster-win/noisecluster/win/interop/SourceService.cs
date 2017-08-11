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
using log4net;
using log4net.Config;
using noisecluster.win.audio.capture;
using noisecluster.win.interop.providers;
using noisecluster.win.transport;
using noisecluster.win.transport.aeron;
using Adaptive.Aeron;

namespace noisecluster.win.interop
{
    public class SourceService : IDisposable
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(SourceService));
        private readonly ITransportProvider _transportProvider;
        private readonly WasapiRecorder.DataHandler _dataHandler;

        private WasapiRecorder _audio;
        private ISource _transport;
        private int _isTransportRunning; //0 = false; 1 = true

        private SourceService(ITransportProvider transportProvider, bool withDebugingHandler = false)
        {
            BasicConfigurator.Configure();
            _isTransportRunning = 0;
            _transportProvider = transportProvider;

            if (withDebugingHandler)
            {
                _dataHandler = (data, length) =>
                {
                    _log.DebugFormat(
                        "Captured [{0}] bytes of audio data; transport is [{1}] and [{2}]; ",
                        length,
                        _isTransportRunning == 1 ? "running" : "not running",
                        _transport.IsActive() ? "active" : "not active"
                    );

                    if (_isTransportRunning == 1 && _transport.IsActive())
                    {
                        _transport.Send(data, 0, length);
                        _log.DebugFormat("Sent [{0}] bytes of audio data", length);
                    }
                };
            }
            else
            {
                _dataHandler = (data, length) =>
                {
                    if (_isTransportRunning == 1 && _transport.IsActive())
                    {
                        _transport.Send(data, 0, length);
                    }
                };
            }
        }

        public SourceService(
            Aeron.Context systemContext,
            int stream,
            string address,
            int port,
            int bufferSize,
            string @interface = null,
            bool withDebugingHandler = false
        ) : this(new providers.transport.Aeron(systemContext, stream, address, port, bufferSize, @interface),
            withDebugingHandler)
        {
        }

        public SourceService(
            string address,
            int port,
            bool withDebugingHandler = false
        ) : this(new providers.transport.Udp(address, port), withDebugingHandler)
        {
        }

        public SourceService(
            int stream,
            string address,
            int port,
            int bufferSize,
            string @interface = null,
            bool withDebugingHandler = false
        ) : this(Defaults.GetNewSystemContext(), stream, address, port, bufferSize, @interface, withDebugingHandler)
        {
        }

        public SourceService(
            int stream,
            string address,
            int port,
            string @interface = null,
            bool withDebugingHandler = false
        ) : this(stream, address, port, Defaults.BufferSize, @interface, withDebugingHandler)
        {
        }

        public bool StartAudio(int sampleRate, int bitsPerSample)
        {
            if (_audio != null || _transport == null) return false;

            _audio = new WasapiRecorder(sampleRate, bitsPerSample, _dataHandler);
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

            _transport = _transportProvider.CreateSource();
            return true;
        }

        public bool StopTransport()
        {
            if (Interlocked.CompareExchange(ref _isTransportRunning, 0, 1) != 1) return false;

            _transport.Dispose();
            _transport = null;
            return true;
        }

        public bool SetHostVolume(int level)
        {
            if (_audio == null) return false;
            _audio.Volume.SetMasterVolumeLevelScalar((float) level / 100, Guid.Empty);
            return true;
        }

        public bool MuteHost()
        {
            if (_audio == null) return false;
            _audio.Volume.SetMute(true, Guid.Empty);
            return true;
        }

        public bool UnmuteHost()
        {
            if (_audio == null) return false;
            _audio.Volume.SetMute(false, Guid.Empty);
            return true;
        }

        public void Dispose()
        {
            StopAudio();
            StopTransport();
            _transportProvider.Dispose();
        }
    }
}