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
using CSCore.CoreAudioAPI;
using CSCore.SoundIn;
using CSCore.Streams;
using log4net;

namespace noisecluster.win.audio.capture
{
    /// <summary>
    /// Allows audio capture via the Windows Audio Session API (WASAPI).
    /// </summary>
    public class WasapiRecorder : IDisposable
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(WasapiRecorder));
        private int _isRunning; //0 = false; 1 = true
        private bool _hasHandler;
        private readonly WasapiCapture _capture;
        private readonly AudioEndpointVolume _volume;
        private readonly SoundInSource _soundInSource;
        private readonly IWaveSource _convertedSource;

        /// <summary>
        /// WASAPI data capture handler.
        /// </summary>
        /// <param name="data">the data that was captured</param>
        /// <param name="length">the length of the captured data</param>
        public delegate void DataHandler(byte[] data, int length);

        /// <summary>
        /// Creates a new instance of the recorder. Captured data will be forwarded via the supplied data handler, if any.
        /// </summary>
        /// <param name="sampleRate">the target sample rate</param>
        /// <param name="bitsPerSample">the target bits per sample</param>
        /// <param name="handler">captured data handler (optional)</param>
        /// <see cref="DataHandler"/>
        public WasapiRecorder(int sampleRate, int bitsPerSample, DataHandler handler = null)
        {
            _capture = new WasapiLoopbackCapture();
            _capture.Initialize();
            _volume = AudioEndpointVolume.FromDevice(_capture.Device);

            _soundInSource = new SoundInSource(_capture) {FillWithZeros = false};

            //defines the source conversion
            _convertedSource = _soundInSource
                .ChangeSampleRate(sampleRate)
                .ToSampleSource()
                .ToWaveSource(bitsPerSample)
                .ToStereo();

            WithDataHandler(handler);
        }

        /// <summary>
        /// Attaches the supplied data handler to the source.
        /// </summary>
        /// <param name="handler">the handler to attach</param>
        /// <exception cref="ArgumentException">if no handler is supplied</exception>
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

        /// <summary>
        /// Retrieves the capture endpoint's volume object.
        /// </summary>
        public AudioEndpointVolume Volume
        {
            get { return _volume; }
        }

        /// <summary>
        /// Retrieves the capture source's original wave format.
        /// </summary>
        public WaveFormat SourceFormat
        {
            get { return _soundInSource.WaveFormat; }
        }

        /// <summary>
        /// Retrieves the capture source's converted (target) wave format.
        /// </summary>
        public WaveFormat TargetFormat
        {
            get { return _convertedSource.WaveFormat; }
        }

        /// <summary>
        /// Retrieves the current state of the recorder.
        /// </summary>
        public bool IsActive
        {
            get { return _isRunning == 1; }
        }

        /// <summary>
        /// Starts the audio capture.
        /// </summary>
        /// <exception cref="InvalidOperationException">if the audio capture was already started or if no data handlers were supplied</exception>
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

        /// <summary>
        /// Stops the audio capture.
        /// </summary>
        /// <exception cref="InvalidOperationException">if the audio capture is not active</exception>
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
                    "Cannot stop audio capture with formats [{0}] -> [{1}]; capture is not active",
                    _soundInSource.WaveFormat,
                    _convertedSource.WaveFormat
                );
                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        /// <summary>
        /// Stops the audio capture and disposes of the resources used by the recorder.
        /// </summary>
        public void Dispose()
        {
            Interlocked.Exchange(ref _isRunning, 0);
            _capture.Stop();
            _convertedSource.Dispose();
            _soundInSource.Dispose();
            _capture.Dispose();
        }
    }
}