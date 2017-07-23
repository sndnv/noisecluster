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
package noisecluster_multi_jvm.test

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

package object utils {
  /**
    * Checks the result of the function 'f' and puts the current thread to sleep if it evaluates to false until either
    * the function returns true or the maximum number of allowed attempts is reached.
    *
    * Notes: The supplied function may be called more times that the set number of attempts.
    *
    * @param what         simple description of what is being waited for
    * @param waitTimeMs   the amount of time to wait for each attempt (in ms)
    * @param waitAttempts the maximum number of attempts to make while waiting
    * @param f            the function to be execution as part of each attempt
    * @throws RuntimeException if the wait fails
    */
  def waitUntil(what: String, waitTimeMs: Long, waitAttempts: Int)(f: => Boolean): Unit = {
    var remainingAttempts = waitAttempts
    while (!f && remainingAttempts > 1) {
      Thread.sleep(waitTimeMs)
      remainingAttempts -= 1
    }

    if (!f && remainingAttempts <= 1) {
      throw new RuntimeException(s"Waiting until [$what] failed after [$waitAttempts] attempts.")
    }
  }

  //enables implicit conversions
  import scala.language.implicitConversions

  class ExtendedAwaitable[T](val self: Future[T]) {
    /**
      * Waits for the future to complete and returns the result, if successful.
      *
      * @param waitDuration implicit wait duration (default is 15 seconds)
      * @param printTrace   if set to true and an exception is returned by the future,
      *                     the stack trace is printed on the console
      * @return the future's result, if successful
      */
    def await(implicit waitDuration: Duration = 15.seconds, printTrace: Boolean = true): T = {
      Await.ready(self, waitDuration)
      self.value match {
        case Some(Success(t)) => t
        case Some(Failure(e)) => if (printTrace) e.printStackTrace(); throw e
        case _ => throw new IllegalStateException(
          s"noisecluster_multi_jvm.test.utils.ExtendedAwaitable::await > Failed to wait for future [$self] with duration [$waitDuration]."
        )
      }
    }

    /**
      * Ignores any non-fatal exceptions that are returned by the future, rather than interrupting the tests.
      *
      * @param printTrace if set to true and an exception is returned by the future,
      *                   the stack trace is printed on the console
      * @return Some(result: T), if the future is successful or None if it is not
      */
    def ignoreFailure(implicit ec: ExecutionContext, printTrace: Boolean = true): Future[Option[T]] = {
      self
        .map(result => Some(result))
        .recover {
          case NonFatal(e) =>
            if (printTrace) e.printStackTrace()
            None
        }
    }
  }
}
