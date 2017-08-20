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

namespace noisecluster.win.transport.udp
{
    /// <summary>
    /// Container for various default values used by the UDP transport subsystem.
    /// </summary>
    public static class Defaults
    {
        public static int BufferSizeSmall = 4 * 1024;
        public static int BufferSize = 16 * 1024;
        public static int BufferSizeLarge = 64 * 1024;
    }
}