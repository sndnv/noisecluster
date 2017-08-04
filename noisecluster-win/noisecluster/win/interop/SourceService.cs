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
using log4net.Config;
using noisecluster.win.audio;
using noisecluster.win.audio.capture;
using noisecluster.win.transport.aeron;

namespace noisecluster.win.interop
{
    public class SourceService : IDisposable
    {
        private readonly Aeron _aeron;
        private readonly WasapiRecorder _audio;
        private readonly Source _transport;
        private int _isTransportRunning; //0 = false; 1 = true

        public SourceService(Aeron.Context systemContext, int stream, string address, int port, int bufferSize,
            string @interface = null)
        {
            BasicConfigurator.Configure();
            _aeron = Aeron.Connect(systemContext);

            _transport = string.IsNullOrEmpty(@interface)
                ? new Source(_aeron, stream, address, port, bufferSize)
                : new Source(_aeron, stream, address, port, @interface, bufferSize);

            _isTransportRunning = 0;

            _audio = new WasapiRecorder(
                (data, length) =>
                {
                    if (_isTransportRunning == 1)
                    {
                        _transport.Send(data, 0, length);
                    }
                }
            );
        }

        public SourceService(int stream, string address, int port, int bufferSize, string @interface = null)
            : this(Defaults.GetNewSystemContext(), stream, address, port, bufferSize, @interface)
        {
        }

        public SourceService(int stream, string address, int port, string @interface = null)
            : this(stream, address, port, Defaults.BufferSize, @interface)
        {
        }

        public bool IsAudioActive
        {
            get { return _audio.IsActive; }
        }

        public bool IsTransportActive
        {
            get { return _isTransportRunning == 1; }
        }

        public void StartAudio()
        {
            _audio.Start();
        }

        public void StopAudio(bool restart)
        {
            if (_audio.IsActive)
            {
                _audio.Stop();
            }

            if (restart)
            {
                _audio.Start();
            }
        }

        public void StartTransport()
        {
            Interlocked.Exchange(ref _isTransportRunning, 1);
        }

        public void StopTransport(bool restart)
        {
            Interlocked.Exchange(ref _isTransportRunning, 0);
            StopAudio(restart);

            if (restart)
            {
                Interlocked.Exchange(ref _isTransportRunning, 1);
            }
        }

        public AudioFormatContainer SourceFormat
        {
            get
            {
                var format = _audio.SourceFormat;
                return new AudioFormatContainer(
                    format.WaveFormatTag.ToString(),
                    format.SampleRate,
                    format.BitsPerSample,
                    format.Channels,
                    ((format.BitsPerSample + 7) / 8) * format.Channels,
                    format.SampleRate,
                    false
                );
            }
        }

        public AudioFormatContainer TargetFormat
        {
            get
            {
                var format = _audio.TargetFormat;
                return new AudioFormatContainer(
                    format.WaveFormatTag.ToString(),
                    format.SampleRate,
                    format.BitsPerSample,
                    format.Channels,
                    ((format.BitsPerSample + 7) / 8) * format.Channels,
                    format.SampleRate,
                    false
                );
            }
        }

        public void Dispose()
        {
            StopAudio(false);
            StopTransport(false);
            _audio.Dispose();
            _transport.Dispose();
            _aeron.Dispose();
        }
    }
}