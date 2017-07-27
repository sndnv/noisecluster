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
using System.Threading;

namespace noisecluster.win.test
{
    /// <summary>
    /// Various functions to aid in testing.
    /// </summary>
    public static class Utils
    {
        /// <summary>
        /// Checks the result of the function 'f' and puts the current thread to sleep if it evaluates to false until either
        /// the function returns true or the maximum number of allowed attempts is reached.
        /// 
        /// <remarks>The supplied function may be called more times that the set number of attempts.</remarks>
        /// </summary>
        /// <param name="what">simple description of what is being waited for</param>
        /// <param name="waitTimeMs">the amount of time to wait for each attempt (in ms)</param>
        /// <param name="waitAttempts">the maximum number of attempts to make while waiting</param>
        /// <param name="f">the function to be execution as part of each attempt</param>
        /// <exception cref="SystemException">if the wait fails</exception>
        public static void WaitUntil(string what, int waitTimeMs, int waitAttempts, Func<bool> f)
        {
            var remainingAttempts = waitAttempts;
            while (!f() && remainingAttempts > 1)
            {
                Thread.Sleep(waitTimeMs);
                remainingAttempts -= 1;
            }

            if (!f() && remainingAttempts <= 1)
            {
                throw new SystemException(
                    string.Format("Waiting until [{0}] failed after [{1}] attempts.", what, waitAttempts)
                );
            }
        }
    }
}