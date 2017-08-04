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

namespace noisecluster.win.audio
{
    public class AudioFormatContainer
    {
        public string Encoding { get; private set; }
        public float SampleRate { get; private set; }
        public int SampleSizeInBits { get; private set; }
        public int Channels { get; private set; }
        public int FrameSize { get; private set; }
        public int FrameRate { get; private set; }
        public bool BigEndian { get; private set; }

        public AudioFormatContainer(
            string encoding,
            float sampleRate,
            int sampleSizeInBits,
            int channels,
            int frameSize,
            int frameRate,
            bool bigEndian
        )
        {
            Encoding = encoding;
            SampleRate = sampleRate;
            SampleSizeInBits = sampleSizeInBits;
            Channels = channels;
            FrameSize = frameSize;
            FrameRate = frameRate;
            BigEndian = bigEndian;
        }
    }
}