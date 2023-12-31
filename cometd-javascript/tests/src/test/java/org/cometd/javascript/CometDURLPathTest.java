/*
 * Copyright (c) 2008-2022 the original author or authors.
 *
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
package org.cometd.javascript;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.server.BayeuxServer.Extension;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CometDURLPathTest extends AbstractCometDLongPollingTest {
    @BeforeEach
    public void initExtension() {
        bayeuxServer.addExtension(new BayeuxURLExtension());
    }

    @Test
    public void testURLPath() throws Exception {
        evaluateScript("var connectLatch = new Latch(1);");
        Latch connectLatch = javaScript.get("connectLatch");
        evaluateScript("var handshake = undefined;");
        evaluateScript("var connect = undefined;");
        evaluateScript("cometd.addListener('/meta/handshake', function(message) { handshake = message; });");
        evaluateScript("cometd.addListener('/meta/connect', function(message) { connect = message; connectLatch.countDown(); });");
        evaluateScript("cometd.init({url: '" + cometdURL + "/', logLevel: '" + getLogLevel() + "'})");
        Assertions.assertTrue(connectLatch.await(5000));

        evaluateScript("window.assert(handshake !== undefined, 'handshake is undefined');");
        evaluateScript("window.assert(handshake.ext !== undefined, 'handshake without ext');");
        String handshakeURI = evaluateScript("handshake.ext.uri");
        Assertions.assertTrue(handshakeURI.endsWith("/handshake"));

        evaluateScript("window.assert(connect !== undefined, 'connect is undefined');");
        evaluateScript("window.assert(connect.ext !== undefined, 'connect without ext');");
        String connectURI = evaluateScript("connect.ext.uri");
        Assertions.assertTrue(connectURI.endsWith("/connect"));

        evaluateScript("var disconnectLatch = new Latch(1);");
        Latch disconnectLatch = javaScript.get("disconnectLatch");
        evaluateScript("var disconnect = undefined;");
        evaluateScript("cometd.addListener('/meta/disconnect', function(message) { disconnect = message; disconnectLatch.countDown(); });");
        evaluateScript("cometd.disconnect();");
        Assertions.assertTrue(disconnectLatch.await(5000));

        evaluateScript("window.assert(disconnect !== undefined, 'disconnect is undefined');");
        evaluateScript("window.assert(disconnect.ext !== undefined, 'disconnect without ext');");
        String disconnectURI = evaluateScript("disconnect.ext.uri");
        Assertions.assertTrue(disconnectURI.endsWith("/disconnect"));
    }

    @Test
    public void testURLPathWithFile() throws Exception {
        evaluateScript("var connectLatch = new Latch(1);");
        Latch connectLatch = javaScript.get("connectLatch");
        evaluateScript("var handshake = undefined;");
        evaluateScript("var connect = undefined;");
        evaluateScript("cometd.addListener('/meta/handshake', function(message) { handshake = message; });");
        evaluateScript("cometd.addListener('/meta/connect', function(message) { connect = message; connectLatch.countDown(); });");
        evaluateScript("cometd.init({url: '" + cometdURL + "/target.cometd', logLevel: '" + getLogLevel() + "'})");
        Assertions.assertTrue(connectLatch.await(5000));

        evaluateScript("window.assert(handshake !== undefined, 'handshake is undefined');");
        evaluateScript("window.assert(handshake.ext !== undefined, 'handshake without ext');");
        String handshakeURI = evaluateScript("handshake.ext.uri");
        Assertions.assertFalse(handshakeURI.endsWith("/handshake"));

        evaluateScript("window.assert(connect !== undefined, 'connect is undefined');");
        evaluateScript("window.assert(connect.ext !== undefined, 'connect without ext');");
        String connectURI = evaluateScript("connect.ext.uri");
        Assertions.assertFalse(connectURI.endsWith("/connect"));

        evaluateScript("var disconnectLatch = new Latch(1);");
        Latch disconnectLatch = javaScript.get("disconnectLatch");
        evaluateScript("var disconnect = undefined;");
        evaluateScript("cometd.addListener('/meta/disconnect', function(message) { disconnect = message; disconnectLatch.countDown(); });");
        evaluateScript("cometd.disconnect();");
        Assertions.assertTrue(disconnectLatch.await(5000));

        evaluateScript("window.assert(disconnect !== undefined, 'disconnect is undefined');");
        evaluateScript("window.assert(disconnect.ext !== undefined, 'disconnect without ext');");
        String disconnectURI = evaluateScript("disconnect.ext.uri");
        Assertions.assertFalse(disconnectURI.endsWith("/disconnect"));
    }

    @Test
    public void testURLPathWithParameters() throws Exception {
        evaluateScript("var connectLatch = new Latch(1);");
        Latch connectLatch = javaScript.get("connectLatch");
        evaluateScript("var handshake = undefined;");
        evaluateScript("var connect = undefined;");
        evaluateScript("cometd.addListener('/meta/handshake', function(message) { handshake = message; });");
        evaluateScript("cometd.addListener('/meta/connect', function(message) { connect = message; connectLatch.countDown(); });");
        evaluateScript("cometd.init({url: '" + cometdURL + "/?param=1', logLevel: '" + getLogLevel() + "'})");
        Assertions.assertTrue(connectLatch.await(5000));

        evaluateScript("window.assert(handshake !== undefined, 'handshake is undefined');");
        evaluateScript("window.assert(handshake.ext !== undefined, 'handshake without ext');");
        String handshakeURI = evaluateScript("handshake.ext.uri");
        Assertions.assertFalse(handshakeURI.endsWith("/handshake"));

        evaluateScript("window.assert(connect !== undefined, 'connect is undefined');");
        evaluateScript("window.assert(connect.ext !== undefined, 'connect without ext');");
        String connectURI = evaluateScript("connect.ext.uri");
        Assertions.assertFalse(connectURI.endsWith("/connect"));

        evaluateScript("var disconnectLatch = new Latch(1);");
        Latch disconnectLatch = javaScript.get("disconnectLatch");
        evaluateScript("var disconnect = undefined;");
        evaluateScript("cometd.addListener('/meta/disconnect', function(message) { disconnect = message; disconnectLatch.countDown(); });");
        evaluateScript("cometd.disconnect();");
        Assertions.assertTrue(disconnectLatch.await(5000));

        evaluateScript("window.assert(disconnect !== undefined, 'disconnect is undefined');");
        evaluateScript("window.assert(disconnect.ext !== undefined, 'disconnect without ext');");
        String disconnectURI = evaluateScript("disconnect.ext.uri");
        Assertions.assertFalse(disconnectURI.endsWith("/disconnect"));
    }

    @Test
    public void testURLPathDisabled() throws Exception {
        evaluateScript("var connectLatch = new Latch(1);");
        Latch connectLatch = javaScript.get("connectLatch");
        evaluateScript("var handshake = undefined;");
        evaluateScript("var connect = undefined;");
        evaluateScript("cometd.addListener('/meta/handshake', function(message) { handshake = message; });");
        evaluateScript("cometd.addListener('/meta/connect', function(message) { connect = message; connectLatch.countDown(); });");
        evaluateScript("cometd.init({url: '" + cometdURL + "/', logLevel: '" + getLogLevel() + "', appendMessageTypeToURL: false})");
        Assertions.assertTrue(connectLatch.await(5000));

        evaluateScript("window.assert(handshake !== undefined, 'handshake is undefined');");
        evaluateScript("window.assert(handshake.ext !== undefined, 'handshake without ext');");
        String handshakeURI = evaluateScript("handshake.ext.uri");
        Assertions.assertFalse(handshakeURI.endsWith("/handshake"));

        evaluateScript("window.assert(connect !== undefined, 'connect is undefined');");
        evaluateScript("window.assert(connect.ext !== undefined, 'connect without ext');");
        String connectURI = evaluateScript("connect.ext.uri");
        Assertions.assertFalse(connectURI.endsWith("/connect"));

        evaluateScript("var disconnectLatch = new Latch(1);");
        Latch disconnectLatch = javaScript.get("disconnectLatch");
        evaluateScript("var disconnect = undefined;");
        evaluateScript("cometd.addListener('/meta/disconnect', function(message) { disconnect = message; disconnectLatch.countDown(); });");
        evaluateScript("cometd.disconnect();");
        Assertions.assertTrue(disconnectLatch.await(5000));

        evaluateScript("window.assert(disconnect !== undefined, 'disconnect is undefined');");
        evaluateScript("window.assert(disconnect.ext !== undefined, 'disconnect without ext');");
        String disconnectURI = evaluateScript("disconnect.ext.uri");
        Assertions.assertFalse(disconnectURI.endsWith("/disconnect"));
    }

    public static class BayeuxURLExtension implements Extension {
        @Override
        public boolean sendMeta(ServerSession to, Mutable message) {
            if (Channel.META_HANDSHAKE.equals(message.getChannel()) ||
                    Channel.META_CONNECT.equals(message.getChannel()) ||
                    Channel.META_DISCONNECT.equals(message.getChannel())) {
                String uri = message.getBayeuxContext().getURL();
                message.getExt(true).put("uri", uri);
            }
            return true;
        }
    }
}
