/*
 * Copyright (c) 2020, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.zxg.network.core;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class EventLoop implements Closeable {

    private final AsynchronousChannelGroup channelGroup;

    public EventLoop() throws IOException {
        this(Runtime.getRuntime().availableProcessors());
    }

    public EventLoop(int nThreads) throws IOException {
        this(nThreads, Executors.defaultThreadFactory());
    }

    public EventLoop(int nThreads, ThreadFactory threadFactory) throws IOException {
        this(AsynchronousChannelGroup.withFixedThreadPool(nThreads, threadFactory));
    }

    public EventLoop(AsynchronousChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    @Override
    public void close() {
        if (!channelGroup.isShutdown()) {
            this.channelGroup.shutdown();
        }
    }

    public AsynchronousChannelGroup channelGroup() {
        return channelGroup;
    }
}
