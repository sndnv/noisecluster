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
using System.Collections.Generic;
using System.Threading.Tasks;
using log4net.Config;
using noisecluster.win.transport;
using noisecluster.win.transport.udp;
using NUnit.Framework;

namespace noisecluster.win.test.transport.udp
{
    [TestFixture]
    public class UnicastUdpSpec
    {
        private long _testDataSent;
        private long _testDataReceived;

        private readonly DataHandler _testDataHandler;

        private readonly Source _source;
        private readonly Target _target01;
        private readonly Target _target02;
        private readonly Target _target03;

        private readonly int _testByteArraySize;

        private readonly Random _rnd;
        private Task _targetTask01;
        private Task _targetTask02;
        private Task _targetTask03;

        public UnicastUdpSpec()
        {
            BasicConfigurator.Configure();
            _testDataSent = 0;
            _testDataReceived = 0;

            _testDataHandler = (data, length) => { _testDataReceived += length; };

            const string address = "127.0.0.1";
            const int sourcePort = 49142;
            const int targetPort01 = 49143;
            const int targetPort02 = 49144;
            const int targetPort03 = 49145;

            _source = new Source(
                new List<Tuple<string, int>>
                {
                    Tuple.Create(address, targetPort01),
                    Tuple.Create(address, targetPort02),
                    Tuple.Create(address, targetPort03)
                },
                sourcePort
            );

            _target01 = new Target(targetPort01);
            _target02 = new Target(targetPort02);
            _target03 = new Target(targetPort03);

            _testByteArraySize = 1000;

            _rnd = new Random();
        }

        [Test]
        public void T01_SourceAndTarget_should_ExchangeData()
        {
            _targetTask01 = new Task(() => { _target01.Start(_testDataHandler); });
            _targetTask02 = new Task(() => { _target02.Start(_testDataHandler); });
            _targetTask03 = new Task(() => { _target03.Start(_testDataHandler); });
            _targetTask01.Start();
            _targetTask02.Start();
            _targetTask03.Start();

            Utils.WaitUntil("target becomes active", 500, 10, () => _target01.IsActive());
            Utils.WaitUntil("target becomes active", 500, 10, () => _target02.IsActive());
            Utils.WaitUntil("target becomes active", 500, 10, () => _target03.IsActive());

            var bytes = new byte[_testByteArraySize];
            _rnd.NextBytes(bytes);
            _source.Send(bytes);
            _testDataSent += _testByteArraySize;

            Utils.WaitUntil("data is received by target", 500, 10, () => _testDataSent * 3 == _testDataReceived);

            Assert.IsTrue(_testDataSent * 3 == _testDataReceived);
            Assert.IsTrue(_testDataReceived == _testByteArraySize * 3);
        }

        [Test]
        public void T02_Target_should_StopAcceptingData()
        {
            Assert.IsTrue(_target01.IsActive());
            Assert.IsTrue(_target02.IsActive());
            Assert.IsTrue(_target03.IsActive());
            _target01.Stop();

            Utils.WaitUntil("target becomes inactive", 500, 10, () => !_target01.IsActive());

            Assert.IsFalse(_target01.IsActive());

            Assert.Throws<InvalidOperationException>(() => { _target01.Stop(); });
        }

        [Test]
        public void T03_Target_should_RestartAcceptingData()
        {
            Assert.IsFalse(_target01.IsActive());

            _targetTask01 = new Task(() => { _target01.Start(_testDataHandler); });
            _targetTask01.Start();

            Utils.WaitUntil("target becomes active", 500, 10, () => _target01.IsActive());

            Assert.IsTrue(_target01.IsActive());

            var bytes = new byte[_testByteArraySize];
            _rnd.NextBytes(bytes);
            _source.Send(bytes);
            _testDataSent += _testByteArraySize;

            Utils.WaitUntil("data is received by target", 500, 10, () => _testDataSent * 3 == _testDataReceived);

            Assert.IsTrue(_testDataSent * 3 == _testDataReceived);
            Assert.IsTrue(_testDataReceived == _testByteArraySize * 6);
        }

        [Test]
        public void T04_Target_should_FailToCloseActiveConnection()
        {
            Assert.Throws<InvalidOperationException>(() => { _target01.Close(); });
        }

        [Test]
        public void T05_Target_should_StopAndCloseItsConnection()
        {
            Assert.IsTrue(_target01.IsActive());
            Assert.IsTrue(_target02.IsActive());
            Assert.IsTrue(_target03.IsActive());

            _target01.Stop();
            _target02.Stop();
            _target03.Stop();

            Utils.WaitUntil("target becomes inactive", 500, 10, () => !_target01.IsActive());
            Utils.WaitUntil("target becomes inactive", 500, 10, () => !_target02.IsActive());
            Utils.WaitUntil("target becomes inactive", 500, 10, () => !_target03.IsActive());

            _target01.Close();
            _target02.Close();
            _target03.Close();

            Assert.IsFalse(_target01.IsActive());
            Assert.IsFalse(_target02.IsActive());
            Assert.IsFalse(_target03.IsActive());
        }
    }
}