/*
 * Copyright 2013 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc.async.internal.mysql

import com.github.mauricio.async.db._
import scala.concurrent._, duration.DurationInt
import scalikejdbc.async._, ShortenedNames._, internal._

/**
 * MySQL Connection Implementation
 */
trait MySQLConnectionImpl extends AsyncConnectionCommonImpl {

  override def toNonSharedConnection()(implicit cxt: EC = ECGlobal): Future[NonSharedAsyncConnection] = {

    if (this.isInstanceOf[PoolableAsyncConnection[_]]) {
      val pool = this.asInstanceOf[PoolableAsyncConnection[Connection]].pool
      pool.take.map(conn => new NonSharedAsyncConnectionImpl(conn, Some(pool)) with MySQLConnectionImpl)
    } else {
      Future(new SingleNonSharedAsyncConnectionImpl(underlying) with MySQLConnectionImpl)
    }
  }

  override protected def extractGeneratedKey(queryResult: QueryResult)(implicit cxt: EC = ECGlobal): Option[Long] = {
    if (!this.isInstanceOf[NonSharedAsyncConnection]) {
      throw new IllegalStateException("This asynchronous connection must be a non-shared connection.")
    }
    Await.result(underlying.sendQuery("SELECT LAST_INSERT_ID()").map { result =>
      result.rows.headOption.flatMap { rows =>
        rows.headOption.map { row => row(0).asInstanceOf[Long] }
      }
    }, 10.seconds)
  }

}
