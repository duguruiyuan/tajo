/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tajo.master.querymaster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.service.AbstractService;
import org.apache.hadoop.yarn.util.RackResolver;
import org.apache.tajo.QueryConf;
import org.apache.tajo.QueryId;
import org.apache.tajo.util.TajoIdUtils;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class QueryMasterRunner extends AbstractService {
  private static final Log LOG = LogFactory.getLog(QueryMasterRunner.class);
  private QueryConf queryConf;
  private QueryMaster queryMaster;
  private QueryId queryId;
  private long appSubmitTime;
  private String queryMasterManagerAddress;

  public QueryMasterRunner(QueryId queryId, long appSubmitTime, String queryMasterManagerAddress) {
    super(QueryMasterRunner.class.getName());
    this.queryId = queryId;
    this.appSubmitTime = appSubmitTime;
    this.queryMasterManagerAddress = queryMasterManagerAddress;
  }

  private class ShutdownHook implements Runnable {
    @Override
    public void run() {
      LOG.info("============================================");
      LOG.info("QueryMaster received SIGINT Signal");
      LOG.info("============================================");
      stop();
    }
  }

  @Override
  public void init(Configuration conf) {
    this.queryConf = (QueryConf)conf;
    RackResolver.init(queryConf);

    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));

    super.init(conf);
  }

  @Override
  public void start() {
    //create QueryMaster
    QueryMaster query = new QueryMaster(queryId, appSubmitTime, queryMasterManagerAddress);

    query.init(queryConf);
    query.start();
  }

  @Override
  public void stop() {
  }

  public static void main(String[] args) throws Exception {
    LOG.info("QueryMasterRunner started");

    final QueryConf conf = new QueryConf();
    conf.addResource(new Path(QueryConf.FILENAME));

    UserGroupInformation.setConfiguration(conf);

    final QueryId queryId = TajoIdUtils.createQueryId(args[0]);
    final long appSubmitTime = Long.parseLong(args[1]);
    final String queryMasterManagerAddr = args[2];

    LOG.info("Received QueryId:" + queryId);

    QueryMasterRunner queryMasterRunner = new QueryMasterRunner(queryId, appSubmitTime, queryMasterManagerAddr);
    queryMasterRunner.init(conf);
    queryMasterRunner.start();

    synchronized(queryId) {
      queryId.wait();
    }

    System.exit(0);
  }

  public static void printThreadInfo(PrintWriter stream, String title) {
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    final int STACK_DEPTH = 60;
    boolean contention = threadBean.isThreadContentionMonitoringEnabled();
    long[] threadIds = threadBean.getAllThreadIds();
    stream.println("Process Thread Dump: " + title);
    stream.println(threadIds.length + " active threads");
    for (long tid : threadIds) {
      ThreadInfo info = threadBean.getThreadInfo(tid, STACK_DEPTH);
      if (info == null) {
        stream.println("  Inactive");
        continue;
      }
      stream.println("Thread " + getTaskName(info.getThreadId(), info.getThreadName()) + ":");
      Thread.State state = info.getThreadState();
      stream.println("  State: " + state);
      stream.println("  Blocked count: " + info.getBlockedCount());
      stream.println("  Waited count: " + info.getWaitedCount());
      if (contention) {
        stream.println("  Blocked time: " + info.getBlockedTime());
        stream.println("  Waited time: " + info.getWaitedTime());
      }
      if (state == Thread.State.WAITING) {
        stream.println("  Waiting on " + info.getLockName());
      } else if (state == Thread.State.BLOCKED) {
        stream.println("  Blocked on " + info.getLockName());
        stream.println("  Blocked by " + getTaskName(info.getLockOwnerId(), info.getLockOwnerName()));
      }
      stream.println("  Stack:");
      for (StackTraceElement frame : info.getStackTrace()) {
        stream.println("    " + frame.toString());
      }
    }
    stream.flush();
  }

  private static String getTaskName(long id, String name) {
    if (name == null) {
      return Long.toString(id);
    }
    return id + " (" + name + ")";
  }
}
