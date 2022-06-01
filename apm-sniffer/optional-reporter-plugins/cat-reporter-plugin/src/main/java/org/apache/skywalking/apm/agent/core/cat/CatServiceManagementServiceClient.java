/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.cat;

import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.OverrideImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.remote.ServiceManagementClient;

/**
 * A service management data(Instance registering properties and Instance pinging) reporter.
 */
@OverrideImplementor(ServiceManagementClient.class)
public class CatServiceManagementServiceClient implements BootService, Runnable, CatConnectionStatusListener {
    private static final ILog LOGGER = LogManager.getLogger(CatServiceManagementServiceClient.class);





    @Override
    public void prepare() {
        CatProducerManager producerManager = ServiceManager.INSTANCE.findService(CatProducerManager.class);
        producerManager.addListener(this);
    }

    @Override
    public void boot() {

    }

    @Override
    public void run() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onStatusChanged(CatConnectionStatus status) {

    }

    @Override
    public void shutdown() {

    }
}
