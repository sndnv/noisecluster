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
using CSCore;
using CSCore.SoundIn;
using CSCore.Streams;

namespace noisecluster.audio.capture
{
    public class WasapiRecorder //TODO - make disposable?
    {
        private bool isRunning = false; //TODO - atomic?
        private WasapiCapture capture;
        private SoundInSource soundInSource;
        private IWaveSource convertedSource;
        private WasapiDataHandler dataHandler;
        private byte[] dataBuffer;

        public WasapiRecorder(WasapiDataHandler handler)
        {
            capture = new WasapiLoopbackCapture();
            capture.Initialize();

            soundInSource = new SoundInSource(capture) {FillWithZeros = false};

            convertedSource = soundInSource
                //.ChangeSampleRate(48000) //TODO - ?
                .ToSampleSource()
                .ToWaveSource(16) //TODO - ?
                .ToStereo();

            dataHandler = handler;

            dataBuffer = new byte[convertedSource.WaveFormat.BytesPerSecond];

            soundInSource.DataAvailable += (sender, e) =>
            {
                int read, total = 0;

                while ((read = convertedSource.Read(dataBuffer, 0, dataBuffer.Length)) > 0)
                {
                    total += read;
                }

                dataHandler(dataBuffer, total); //TODO - safe to pass byte[] ?
            };
        }

        public bool IsActive
        {
            get { return true; } //TODO
        }

        public void Start()
        {
            //TODO - log
            capture.Start();
        }

        public void Stop()
        {
            //TODO - log
            capture.Stop();
        }

        public delegate void WasapiDataHandler(byte[] data, int length);
    }
}
