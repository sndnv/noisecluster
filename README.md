# noisecluster
Near real-time audio streaming to multiple devices over a local (wireless) network.

## Getting Started
[noisecluster Wiki](https://github.com/sndnv/noisecluster/wiki/Getting-Started)

## Security Warnings
- There is (currently) absolutely **NO security**!
- Audio and control data is sent **unencrypted**!
- All components/devices should be running on a properly secured local network!

## Components
- [noisecluster-jvm](https://github.com/sndnv/noisecluster/tree/master/noisecluster-jvm) - Core library (Scala) 
- [noisecluster-win](https://github.com/sndnv/noisecluster/tree/master/noisecluster-win) - Core library (C#)
- [ve](https://github.com/sndnv/noisecluster/tree/master/ve) - Linux rendering service (Scala)
- [vili](https://github.com/sndnv/noisecluster/tree/master/vili) - Windows source and management UI (Scala & C#)

## Built With
- Scala 2.12.2
- sbt 0.13.15
- C# / .NET 4.5
- [Aeron 1.3.x](https://github.com/real-logic/aeron) - transport (JVM)
- [Aeron.NET 1.3.x](https://github.com/AdaptiveConsulting/Aeron.NET) - transport (.NET)
- [Play 2.6.x](https://github.com/playframework/playframework) - vili UI
- [core3](https://github.com/Interel-Group/core3) - vili UI
- [Akka 2.5.x](https://github.com/akka/akka) - actors and clustering
- [CSCore](https://github.com/filoe/cscore) - Windows audio capture (via WASAPI)
- [jni4net](https://github.com/jni4net/jni4net) - JVM <-> .NET bridge

## Versioning
We use [SemVer](http://semver.org/) for versioning.

## Future Goals
- [ ] Encrypted communication
- [ ] Management authentication and authorization
- [ ] Full support for Windows and Linux targets and sources
- [ ] Multi-source mixing support
- [ ] Selection of capture and rendering devices
- [ ] Source and target auto-discovery
- [ ] Quick audio out-of-sync detection and recovery
- [ ] TCP transport

## License
This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details

> Copyright 2017 https://github.com/sndnv
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
> http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.