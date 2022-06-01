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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.skywalking.apm.agent.core.boot.BootService;
import org.apache.skywalking.apm.agent.core.boot.DefaultImplementor;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.plugin.loader.AgentClassLoader;
import org.apache.skywalking.apm.agent.core.remote.GRPCChannelManager;

/**
 * Configuring, initializing and holding a KafkaProducer instance for reporters.
 */
@DefaultImplementor
public class CatProducerManager implements BootService {

    private static final ILog LOGGER = LogManager.getLogger(CatProducerManager.class);

    private final Set<String> topics = new HashSet<>();
    private final List<CatConnectionStatusListener> listeners = new ArrayList<>();



    @Override
    public void prepare() {
    }

    @Override
    public void boot() {
        run();
    }


    public void addListener(CatConnectionStatusListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void onComplete() {
    }


    public void run() {
        Thread.currentThread().setContextClassLoader(AgentClassLoader.getDefault());
        notifyListeners(CatConnectionStatus.CONNECTED);
    }

    private void notifyListeners(CatConnectionStatus status) {
        for (CatConnectionStatusListener listener : listeners) {
            listener.onStatusChanged(status);
        }
    }



    /**
     * make kafka producer init later but before {@link GRPCChannelManager}
     *
     * @return priority value
     */
    @Override
    public int priority() {
        return ServiceManager.INSTANCE.findService(GRPCChannelManager.class).priority() - 1;
    }

    @Override
    public void shutdown() {

    }
}