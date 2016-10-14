/*
 * Copyright 1998-2016 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.util

import akka.actor.ActorSystem
import akka.pattern.after
import org.springframework.web.context.request.async.DeferredResult

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

object RichFuture {
  implicit class RichFuture[T](val future:Future[T]) extends AnyVal {
    def toDeferredResult(implicit executor : ExecutionContext):DeferredResult[T] = {
      val result = new DeferredResult[T]()

      future.onComplete {
        case Success(r) => result.setResult(r)
        case Failure(t) =>
          result.setErrorResult(t)
      }

      result
    }

    def withTimeout(duration: FiniteDuration)(implicit system: ActorSystem, executor : ExecutionContext): Future[T] = {
      if (future.isCompleted) {
        future
      } else {
        Future firstCompletedOf Seq(
          future,
          after(duration, system.scheduler)(Future.failed(new TimeoutException(s"Timed out after ${duration.toMillis}ms"))))
      }
    }
  }
}
