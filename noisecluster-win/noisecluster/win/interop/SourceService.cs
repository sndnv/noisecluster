using System;
using Adaptive.Aeron;
using noisecluster.win.audio.capture;
using noisecluster.win.transport.aeron;

namespace noisecluster.win.interop
{
    public class SourceService : IDisposable
    {
        private Aeron _aeron;
        private WasapiRecorder _audio;
        private Source _transport;

        public SourceService(Aeron.Context systemContext, int stream, string address, int port, int bufferSize, string @interface = null)
        {
            //TODO - setup logging
            _aeron = Aeron.Connect(systemContext);
            _audio = new WasapiRecorder();

            _transport = string.IsNullOrEmpty(@interface)
                ? new Source(_aeron, stream, address, port, bufferSize)
                : new Source(_aeron, stream, address, port, @interface, bufferSize);
        }

        public SourceService(int stream, string address, int port, int bufferSize, string @interface = null)
            : this(Defaults.GetNewSystemContext(), stream, address, port, bufferSize, @interface)
        {
        }

        public SourceService(int stream, string address, int port, string @interface = null)
            : this(stream, address, port, Defaults.BufferSize, @interface)
        {
        }

        public void StartAudio()
        {
            //TODO
        }

        public void StopAudio(bool restart)
        {
            //TODO
        }

        public void StartTransport()
        {
            //TODO
        }

        public void StopTransport(bool restart)
        {
            //TODO
        }

        public void StopApplication(bool restart)
        {
            //TODO
        }

        public void StopHost(bool restart)
        {
            //TODO
        }

        //TODO - more?

        public void Dispose()
        {
            //TODO
        }
    }
}