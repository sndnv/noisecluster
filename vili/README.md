# vili
Windows source and management UI

## Getting Started
[noisecluster Wiki](https://github.com/sndnv/noisecluster/wiki/Getting-Started)

## Functionality
- Audio
  - [x] Capture
  - [ ] Rendering
  - [x] Adjust Host Volume
  - [x] Mute/Unmute Host
- Transport
  - [x] Aeron
  - [x] UDP
  - [ ] TCP
- Other
  - [ ] Stop Host
  - [ ] Restart Host
  - [x] Stop Self
  - [ ] Restart Self

## Built With
- Scala 2.12.2
- sbt 0.13.15
- C# / .NET 4.5
- [noisecluster-jvm](https://github.com/sndnv/noisecluster/tree/master/noisecluster-jvm) - Core library (Scala) 
- [noisecluster-win](https://github.com/sndnv/noisecluster/tree/master/noisecluster-win) - Core library (C#)
- [Play 2.6.x](https://github.com/playframework/playframework) - UI
- [core3](https://github.com/Interel-Group/core3) - UI
- [jni4net](https://github.com/jni4net/jni4net) - JVM <-> .NET bridge

## Tested On
- Windows 7 and 10

## Versioning
We use [SemVer](http://semver.org/) for versioning.

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