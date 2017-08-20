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
using noisecluster.win.interop.providers.transport;
using noisecluster.win.transport;
using noisecluster.win.transport.aeron;
using Aeron = Adaptive.Aeron.Aeron;

namespace noisecluster.win.interop
{
    /// <summary>
    /// Source service intended for JVM interoperability.
    /// </summary>
    public class SourceService : IDisposable
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(SourceService));
        private readonly ITransportProvider _transportProvider;
        private readonly WasapiRecorder.DataHandler _dataHandler;

        private WasapiRecorder _audio;
        private ISource _transport;
        private int _isTransportRunning; //0 = false; 1 = true

        /// <summary>
        /// (Internal) Creates a new instance of the service.
        /// </summary>
        /// <param name="transportProvider">the transport provider to use</param>
        /// <param name="withDebugingHandler">set to true to enable data handler debugging</param>
        private SourceService(ITransportProvider transportProvider, bool withDebugingHandler = false)
        {
            BasicConfigurator.Configure(); //enables the log4net basic config
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
                        (_transport != null && _transport.IsActive()) ? "active" : "not active"
                    );

                    if (_isTransportRunning == 1 && _transport != null && _transport.IsActive())
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
                    if (_isTransportRunning == 1 && _transport != null && _transport.IsActive())
                    {
                        _transport.Send(data, 0, length);
                    }
                };
            }
        }

        /// <summary>
        /// Creates a new instance of the service using Aeron for transport.
        /// </summary>
        /// <param name="systemContext">the Aeron system context to use</param>
        /// <param name="stream">the Aeron stream ID</param>
        /// <param name="address">the address to use for UDP transmission</param>
        /// <param name="port">the port to use for UDP transmission</param>
        /// <param name="bufferSize">the Aeron buffer size</param>
        /// <param name="interface">the local interface to bind to (optional)</param>
        /// <param name="withDebugingHandler">set to true to enable data handler debugging</param>
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

        /// <summary>
        /// Creates a new instance of the service using multicast UDP for transport.
        /// </summary>
        /// <param name="multicastTargetAddress">the multicast address to use</param>
        /// <param name="multicastTargetPort">the multicast port to use</param>
        /// <param name="localPort">the local port to bind to</param>
        /// <param name="withDebugingHandler">set to true to enable data handler debugging</param>
        public SourceService(
            string multicastTargetAddress,
            int multicastTargetPort,
            int localPort,
            bool withDebugingHandler = false
        ) : this(new MulticastUdp(multicastTargetAddress, multicastTargetPort, localPort),
            withDebugingHandler)
        {
        }

        /// <summary>
        /// Creates a new instance of the service using unicast UDP for transport. Targets must be added after creation
        /// via the <see cref="AddUnicastTarget"/> method.
        /// </summary>
        /// <param name="localPort">the local port to bind to</param>
        /// <param name="withDebugingHandler">set to true to enable data handler debugging</param>
        public SourceService(
            int localPort,
            bool withDebugingHandler = false
        ) : this(new UnicastUdp(localPort),
            withDebugingHandler)
        {
        }

        /// <summary>
        /// Creates a new instance of the service using Aeron for transport with the default Aeron system context.
        /// </summary>
        /// <param name="stream">the Aeron stream ID</param>
        /// <param name="address">the address to use for UDP transmission</param>
        /// <param name="port">the port to use for UDP transmission</param>
        /// <param name="bufferSize">the Aeron buffer size</param>
        /// <param name="interface">the local interface to bind to (optional)</param>
        /// <param name="withDebugingHandler">set to true to enable data handler debugging</param>
        /// <seealso cref="Defaults.GetNewSystemContext"/>
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

        /// <summary>
        /// Creates a new instance of the service using Aeron for transport with the default Aeron system context and
        /// buffer size.
        /// </summary>
        /// <param name="stream">the Aeron stream ID</param>
        /// <param name="address">the address to use for UDP transmission</param>
        /// <param name="port">the port to use for UDP transmission</param>
        /// <param name="interface">the local interface to bind to (optional)</param>
        /// <param name="withDebugingHandler">set to true to enable data handler debugging</param>
        /// <seealso cref="Defaults.GetNewSystemContext"/>
        /// <seealso cref="Defaults.BufferSize"/>
        public SourceService(
            int stream,
            string address,
            int port,
            string @interface = null,
            bool withDebugingHandler = false
        ) : this(stream, address, port, Defaults.BufferSize, @interface, withDebugingHandler)
        {
        }

        /// <summary>
        /// Adds a new unicast target, if UDP unicast is used for transport.
        /// </summary>
        /// <param name="targetAddress">the new target address</param>
        /// <param name="targetPort">the new target port</param>
        /// <returns>true, if the addition was successful</returns>
        /// <seealso cref="UnicastUdp.AddTarget"/>
        public bool AddUnicastTarget(string targetAddress, int targetPort)
        {
            var unicastTransportProvider = _transportProvider as UnicastUdp;
            if (unicastTransportProvider != null)
            {
                unicastTransportProvider.AddTarget(targetAddress, targetPort);
                return true;
            }
            else
            {
                return false;
            }
        }

        /// <summary>
        /// Creates the audio recorder and starts capture with the specified parameters.
        /// </summary>
        /// <param name="sampleRate">the target sample rate</param>
        /// <param name="bitsPerSample">the target bits per sample</param>
        /// <returns>true, if the operation was successful</returns>
        /// <seealso cref="WasapiRecorder"/>
        /// <seealso cref="WasapiRecorder.Start"/>
        public bool StartAudio(int sampleRate, int bitsPerSample)
        {
            if (_audio != null) return false;

            _audio = new WasapiRecorder(sampleRate, bitsPerSample, _dataHandler);
            _audio.Start();
            return true;
        }

        /// <summary>
        /// Stops the audio capture and disposes of the recorder.
        /// </summary>
        /// <returns>true, if the operation was successful</returns>
        /// <seealso cref="WasapiRecorder"/>
        /// <seealso cref="WasapiRecorder.Stop"/>
        public bool StopAudio()
        {
            if (_audio == null) return false;

            _audio.Dispose();
            _audio = null;
            return true;
        }

        /// <summary>
        /// Creates and starts the data transport.
        /// </summary>
        /// <returns>true, if the operation was successful</returns>
        public bool StartTransport()
        {
            if (Interlocked.CompareExchange(ref _isTransportRunning, 1, 0) != 0) return false;

            _transport = _transportProvider.CreateSource();
            return true;
        }

        /// <summary>
        /// Stops the data transport and disposes of the transport instance.
        /// </summary>
        /// <returns>true, if the operation was successful</returns>
        public bool StopTransport()
        {
            if (Interlocked.CompareExchange(ref _isTransportRunning, 0, 1) != 1) return false;

            _transport.Dispose();
            _transport = null;
            return true;
        }

        /// <summary>
        /// Sets the host's master volume to the specified level.
        /// </summary>
        /// <param name="level">the requested volume level (in %)</param>
        /// <returns>true, if the operation was successful</returns>
        public bool SetHostVolume(int level)
        {
            if (_audio == null) return false;
            _audio.Volume.SetMasterVolumeLevelScalar((float) level / 100, Guid.Empty);
            return true;
        }

        /// <summary>
        /// Mutes the host's master audio endpoint.
        /// </summary>
        /// <returns>true, if the operation was successful</returns>
        public bool MuteHost()
        {
            if (_audio == null) return false;
            _audio.Volume.SetMute(true, Guid.Empty);
            return true;
        }

        /// <summary>
        /// Unmutes the host's master audio endpoint.
        /// </summary>
        /// <returns>true, if the operation was successful</returns>
        public bool UnmuteHost()
        {
            if (_audio == null) return false;
            _audio.Volume.SetMute(false, Guid.Empty);
            return true;
        }

        /// <summary>
        /// Retrieves the host's master volume. <c>0</c> can be returned either because that is the actual volume level
        /// or if the audio endpoint has not been initialized.
        /// </summary>
        /// <returns>the host's master volume</returns>
        public int GetHostVolume()
        {
            if (_audio == null) return 0;
            return Convert.ToInt32(_audio.Volume.GetMasterVolumeLevelScalar() * 100);
        }

        /// <summary>
        /// Retrieves the host's master audio endpoint's mute state. <c>false</c> can be returned if the host's audio
        /// is not muted or if the audio endpoint has not been initialized.
        /// </summary>
        /// <returns>true, if the master audio is muted</returns>
        public bool IsHostMuted()
        {
            if (_audio == null) return false;
            return _audio.Volume.IsMuted;
        }

        /// <summary>
        /// Stops the audio, transport and disposes of all resources.
        /// </summary>
        public void Dispose()
        {
            StopAudio();
            StopTransport();
            _transportProvider.Dispose();
        }
    }
}