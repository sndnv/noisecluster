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
using Adaptive.Aeron;
using Adaptive.Agrona;
using Adaptive.Agrona.Concurrent;

namespace noisecluster.transport.aeron
{
    public class Source
    {
        private int _streamId;
        private string _channel;
        private UnsafeBuffer _buffer;
        private Publication _publication;

        public Source(Aeron aeron, int streamId, string channel, int bufferSize)
        {
            _streamId = streamId;
            _channel = channel;
            _buffer = new UnsafeBuffer(BufferUtil.AllocateDirectAligned(bufferSize, BitUtil.CACHE_LINE_LENGTH));
            _publication = aeron.AddPublication(_channel, _streamId);
        }

        public Source(Aeron aeron, int streamId, string address, int port, int bufferSize)
        {
            //TODO
        }
        
        public Source(Aeron aeron, int streamId, string address, int port, string _interface, int bufferSize)
        {
            //TODO
        }

        private long Offer(int messageSize)
        {
            return 0; //TODO
        }

        public long Send(IDirectBuffer source, int startIndex, int length)
        {
            _buffer.PutBytes(0, source, startIndex, length);
            return Offer(length);
        }

        public long Send(byte[] source, int offset, int length)
        {
            _buffer.PutBytes(0, source, offset, length);
            return Offer(length);
        }
        
        public long Send(byte[] source)
        {
            _buffer.PutBytes(0, source);
            return Offer(source.Length);
        }

        public void Close()
        {
            //TODO
        }
    }
}
