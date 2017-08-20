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
using Adaptive.Aeron;
using Adaptive.Agrona;
using Adaptive.Agrona.Concurrent;
using log4net;

namespace noisecluster.win.transport.aeron
{
    /// <summary>
    /// Source using Aeron for transport.
    /// See https://github.com/real-logic/aeron for more information on configuration and usage.
    /// </summary>
    public class Source : ISource
    {
        private readonly ILog _log = LogManager.GetLogger(typeof(Source));
        private readonly int _stream;
        private readonly string _channel;
        private readonly UnsafeBuffer _buffer;
        private readonly Publication _publication;

        /// <summary>
        ///  Creates a new Aeron source with the specified parameters.
        /// </summary>
        /// <param name="aeron">the Aeron connection to use</param>
        /// <param name="stream">the Aeron stream ID to use</param>
        /// <param name="channel">the Aeron channel to use</param>
        /// <param name="bufferSize">the data buffer size to use (in bytes)</param>
        public Source(Aeron aeron, int stream, string channel, int bufferSize)
        {
            _stream = stream;
            _channel = channel;
            _buffer = new UnsafeBuffer(BufferUtil.AllocateDirectAligned(bufferSize, BitUtil.CACHE_LINE_LENGTH));
            _publication = aeron.AddPublication(_channel, _stream);
        }

        /// <summary>
        /// Creates a new Aeron source using UDP with the specified parameters.
        /// </summary>
        /// <param name="aeron">the Aeron connection to use</param>
        /// <param name="stream">the Aeron stream ID to use</param>
        /// <param name="address">the UDP address to use</param>
        /// <param name="port">the UDP port to use</param>
        /// <param name="bufferSize">the data buffer size to use (in bytes)</param>
        public Source(Aeron aeron, int stream, string address, int port, int bufferSize)
            : this(
                aeron,
                stream,
                string.Format("aeron:udp?endpoint={0}:{1}", address, port),
                bufferSize
            )
        {
        }

        /// <summary>
        /// Creates a new Aeron source using UDP with the specified parameters.
        /// </summary>
        /// <param name="aeron">the Aeron connection to use</param>
        /// <param name="stream">the Aeron stream ID to use</param>
        /// <param name="address">the UDP address to use</param>
        /// <param name="port">the UDP port to use</param>
        /// <param name="interface">the local interface to bind to</param>
        /// <param name="bufferSize">the data buffer size to use (in bytes)</param>
        public Source(Aeron aeron, int stream, string address, int port, string @interface, int bufferSize)
            : this(
                aeron,
                stream,
                string.Format("aeron:udp?endpoint={0}:{1}|interface={2}", address, port, @interface),
                bufferSize
            )
        {
        }

        public bool IsActive()
        {
            return IsConnected;
        }

        /// <summary>
        /// Checks if the Aeron publication is connected.
        /// </summary>
        public bool IsConnected
        {
            get { return _publication.IsConnected; }
        }

        /// <summary>
        /// Checks if the Aeron publication is closed.
        /// </summary>
        public bool IsClosed
        {
            get { return _publication.IsClosed; }
        }

        /// <summary>
        /// Offers the buffered data to the subscribers and logs any issues.
        /// </summary>
        /// <param name="messageSize">the amount of data to offer/publish</param>
        /// <returns>the new stream position</returns>
        /// <exception cref="InvalidOperationException">if the transport is closed or not connected</exception>
        /// <exception cref="SystemException">if an unknown error occurs</exception>
        private long Offer(int messageSize)
        {
            var result = _publication.Offer(_buffer, 0, messageSize);

            if (result >= 0)
            {
                _log.DebugFormat("Offered [{0}] bytes on channel [{1}] with stream [{2}]", messageSize, _channel,
                    _stream);
            }
            else
            {
                switch (result)
                {
                    case Publication.BACK_PRESSURED:
                    {
                        _log.WarnFormat(
                            "Transport for channel [{0}] and stream [{1}] failed due to back pressure",
                            _channel,
                            _stream
                        );
                        break;
                    }

                    case Publication.NOT_CONNECTED:
                    {
                        var message =
                            string.Format(
                                "Cannot use transport for channel [{0}] with stream [{1}]; transport is not connected",
                                _channel,
                                _stream
                            );
                        _log.Error(message);
                        throw new InvalidOperationException(message);
                    }

                    case Publication.ADMIN_ACTION:
                    {
                        _log.WarnFormat("Transport for channel [{0}] and stream [{1}] failed due to admin action",
                            _channel,
                            _stream
                        );
                        break;
                    }

                    case Publication.CLOSED:
                    {
                        var message =
                            string.Format(
                                "Cannot use transport for channel [{0}] and stream [{1}]; transport is already closed",
                                _channel,
                                _stream
                            );
                        _log.Error(message);
                        throw new InvalidOperationException(message);
                    }

                    default:
                    {
                        var message =
                            string.Format(
                                "Transport failed for channel [{0}] and stream [{1}]; unknown offer result encountered: [{2}]",
                                _channel,
                                _stream,
                                result
                            );
                        _log.Error(message);
                        throw new SystemException(message);
                    }
                }
            }

            return result;
        }

        public long Send(IDirectBuffer source, int startIndex, int length)
        {
            _buffer.PutBytes(0, source, startIndex, length);
            return Offer(length);
        }

        public void Send(byte[] source, int offset, int length)
        {
            _buffer.PutBytes(0, source, offset, length);
            Offer(length);
        }

        public void Send(byte[] source)
        {
            _buffer.PutBytes(0, source);
            Offer(source.Length);
        }

        public void Close()
        {
            if (!_publication.IsClosed)
            {
                _log.InfoFormat("Closing transport for channel [{0}] and stream [{1}]", _channel, _stream);
                _publication.Dispose();
                _buffer.Dispose();
                _log.InfoFormat("Closed transport for channel [{0}] and stream [{1}]", _channel, _stream);
            }
            else
            {
                var message = string.Format(
                    "Cannot close transport for channel [{0}] and stream [{1}]; transport is already closed",
                    _channel,
                    _stream
                );

                _log.Warn(message);
                throw new InvalidOperationException(message);
            }
        }

        public void Dispose()
        {
            if (!_publication.IsClosed)
            {
                _publication.Dispose();
                _buffer.Dispose();
            }
        }
    }
}