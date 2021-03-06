﻿/**
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
using System.Threading.Tasks;
using log4net.Config;
using noisecluster.win.transport;
using noisecluster.win.transport.udp;
using NUnit.Framework;

namespace noisecluster.win.test.transport.udp
{
    [TestFixture]
    public class MulticastUdpSpec
    {
        private long _testDataSent;
        private long _testDataReceived;

        private readonly DataHandler _testDataHandler;

        private readonly Source _source;
        private readonly Target _target;

        private readonly int _testByteArraySize;

        private readonly Random _rnd;
        private Task _targetTask;

        public MulticastUdpSpec()
        {
            BasicConfigurator.Configure();
            _testDataSent = 0;
            _testDataReceived = 0;

            _testDataHandler = (data, length) => { _testDataReceived += length; };

            const string address = "225.100.50.25";
            const int sourcePort = 49042;
            const int targetPort = 49043;

            _source = new Source(address, targetPort, sourcePort);
            _target = new Target(targetPort, address);

            _testByteArraySize = 1000;

            _rnd = new Random();
        }

        [Test]
        public void T01_SourceAndTarget_should_ExchangeData()
        {
            _targetTask = new Task(() => { _target.Start(_testDataHandler); });
            _targetTask.Start();

            Utils.WaitUntil("target becomes active", 500, 10, () => _target.IsActive());

            var bytes = new byte[_testByteArraySize];
            _rnd.NextBytes(bytes);
            _source.Send(bytes);
            _testDataSent += _testByteArraySize;

            Utils.WaitUntil("data is received by target", 500, 10, () => _testDataSent == _testDataReceived);

            Assert.IsTrue(_testDataSent == _testDataReceived);
            Assert.IsTrue(_testDataReceived == _testByteArraySize);
        }

        [Test]
        public void T02_Target_should_StopAcceptingData()
        {
            Assert.IsTrue(_target.IsActive());
            _target.Stop();

            Utils.WaitUntil("target becomes inactive", 500, 10, () => !_target.IsActive());

            Assert.IsFalse(_target.IsActive());

            Assert.Throws<InvalidOperationException>(() => { _target.Stop(); });
        }

        [Test]
        public void T03_Target_should_RestartAcceptingData()
        {
            Assert.IsFalse(_target.IsActive());

            _targetTask = new Task(() => { _target.Start(_testDataHandler); });
            _targetTask.Start();

            Utils.WaitUntil("target becomes active", 500, 10, () => _target.IsActive());

            Assert.IsTrue(_target.IsActive());

            var bytes = new byte[_testByteArraySize];
            _rnd.NextBytes(bytes);
            _source.Send(bytes);
            _testDataSent += _testByteArraySize;

            Utils.WaitUntil("data is received by target", 500, 10, () => _testDataSent == _testDataReceived);

            Assert.IsTrue(_testDataSent == _testDataReceived);
            Assert.IsTrue(_testDataReceived == _testByteArraySize * 2);
        }

        [Test]
        public void T04_Target_should_FailToCloseActiveConnection()
        {
            Assert.Throws<InvalidOperationException>(() => { _target.Close(); });
        }

        [Test]
        public void T05_Target_should_StopAndCloseItsConnection()
        {
            Assert.IsTrue(_target.IsActive());
            _target.Stop();

            Utils.WaitUntil("target becomes inactive", 500, 10, () => !_target.IsActive());

            _target.Close();
            Assert.IsFalse(_target.IsActive());
        }
    }
}