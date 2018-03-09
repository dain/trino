/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.connector.thrift.server;

import com.facebook.swift.codec.guice.ThriftCodecModule;
import com.facebook.swift.service.guice.ThriftServerModule;
import com.facebook.swift.service.guice.ThriftServerStatsModule;
import com.google.inject.Scopes;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.log.Logger;

import static com.facebook.swift.service.guice.ThriftServiceExporter.thriftServerBinder;

public final class ThriftReverseServer
{
    private ThriftReverseServer() {}

    public static void main(String[] args)
    {
        Logger log = Logger.get(ThriftReverseServer.class);
        try {
            Bootstrap app = new Bootstrap(
                    new ThriftCodecModule(),
                    new ThriftServerModule(),
                    new ThriftServerStatsModule(),
                    binder -> {
                        binder.bind(ThriftReverseService.class).in(Scopes.SINGLETON);
                        thriftServerBinder(binder).exportThriftService(ThriftReverseService.class);
                    });
            app.strictConfig().initialize();
            log.info("======== SERVER STARTED ========");
        }
        catch (Throwable t) {
            log.error(t);
            System.exit(1);
        }
    }
}
