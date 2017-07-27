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
using CSCore;
using CSCore.SoundIn;
using CSCore.Streams;
using log4net;

namespace noisecluster.audio.capture
{
    public class WasapiRecorder
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(WasapiRecorder));
        private int _isRunning; //0 = false; 1 = true
        private bool _hasHandler;
        private readonly WasapiCapture _capture;
        private readonly SoundInSource _soundInSource;
        private readonly IWaveSource _convertedSource;

        public delegate void DataHandler(byte[] data, int length);

        public WasapiRecorder(DataHandler handler = null)
        {
            _capture = new WasapiLoopbackCapture();
            _capture.Initialize();

            _soundInSource = new SoundInSource(_capture) {FillWithZeros = false};

            _convertedSource = _soundInSource
                .ToSampleSource()
                .ToWaveSource(16)
                .ToStereo();

            WithDataHandler(handler);
        }

        public void WithDataHandler(DataHandler handler)
        {
            if (handler != null)
            {
                _soundInSource.DataAvailable += (s, e) =>
                {
                    var buffer = new byte[_convertedSource.WaveFormat.BytesPerSecond];
                    int bytesRead, bytesTotal = 0;

                    while ((bytesRead = _convertedSource.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        bytesTotal += bytesRead;
                    }

                    handler(buffer, bytesTotal);
                };

                _hasHandler = true;
            }
            else
            {
                throw new ArgumentException("Cannot attach null handler");
            }
        }

        public WaveFormat SourceFormat
        {
            get { return _soundInSource.WaveFormat; }
        }

        public WaveFormat TargetFormat
        {
            get { return _convertedSource.WaveFormat; }
        }

        public bool IsActive
        {
            get { return _isRunning == 1; }
        }

        public void Start()
        {
            if (_hasHandler)
            {
                if (Interlocked.CompareExchange(ref _isRunning, 1, 0) == 0)
                {
                    _log.InfoFormat(
                        "Starting audio capture with formats [{0}] -> [{1}]",
                        _soundInSource.WaveFormat,
                        _convertedSource.WaveFormat
                    );
                    _capture.Start();
                }
                else
                {
                    var message = string.Format(
                        "Cannot start audio capture with formats [{0}] -> [{1}]; capture is already active",
                        _soundInSource.WaveFormat,
                        _convertedSource.WaveFormat
                    );
                    _log.Warn(message);
                    throw new InvalidOperationException(message);
                }
            }
            else
            {
                throw new InvalidOperationException("Cannot start capture without at least one data handler");
            }
        }

        public void Stop()
        {
            if (Interlocked.CompareExchange(ref _isRunning, 0, 1) == 1)
            {
                _log.InfoFormat(
                    "Stopping audio capture with formats [{0}] -> [{1}]",
                    _soundInSource.WaveFormat,
                    _convertedSource.WaveFormat
                );
                _capture.Stop();
            }
            else
            {
                var message = string.Format(
                    "Cannot stop audio capture with formats [{0}] -> [{1}]; capture is already active",
                    _soundInSource.WaveFormat,
                    _convertedSource.WaveFormat
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }
    }
}