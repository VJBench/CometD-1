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
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CometDTransportNegotiationFailureTest extends AbstractCometDTransportsTest {
    @ParameterizedTest
    @MethodSource("transports")
    public void testTransportNegotiationFailureForClientLongPollingServerCallbackPolling(String transport) throws Exception {
        initCometDServer(transport);

        // Only callback-polling on server (via extension), only long-polling on client.
        bayeuxServer.setAllowedTransports("long-polling", "callback-polling");
        bayeuxServer.addExtension(new BayeuxServer.Extension() {
            @Override
            public boolean sendMeta(ServerSession to, ServerMessage.Mutable message) {
                if (Channel.META_HANDSHAKE.equals(message.getChannel())) {
                    message.put(Message.SUPPORTED_CONNECTION_TYPES_FIELD, new String[]{"callback-polling"});
                }
                return true;
            }
        });

        evaluateScript("keep_only_long_polling_transport",
                "cometd.unregisterTransports();" +
                        "cometd.registerTransport('long-polling', originalTransports['long-polling']);");

        evaluateScript("var failureLatch = new Latch(2);");
        Latch failureLatch = javaScript.get("failureLatch");
        evaluateScript("cometd.onTransportException = function(failure, oldTransport, newTransport) {" +
                "    failureLatch.countDown();" +
                "}");
        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");
        evaluateScript("cometd.handshake(function(message) {" +
                "    if (message.successful === false) { " +
                "        failureLatch.countDown(); " +
                "    }" +
                "});");

        Assertions.assertTrue(failureLatch.await(5000));

        disconnect();
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testTransportNegotiationFailureForClientLongPollingServerWebSocket(String transport) throws Exception {
        initCometDServer(transport);

        // Only websocket on server, only long-polling on client.
        bayeuxServer.setAllowedTransports("websocket");
        evaluateScript("keep_only_long_polling_transport",
                "cometd.unregisterTransports();" +
                        "cometd.registerTransport('long-polling', originalTransports['long-polling']);");

        evaluateScript("var failureLatch = new Latch(2);");
        Latch failureLatch = javaScript.get("failureLatch");
        evaluateScript("cometd.onTransportException = function(failure, oldTransport, newTransport) {" +
                "    failureLatch.countDown();" +
                "}");
        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");
        evaluateScript("cometd.handshake(function(message) {" +
                "    if (message.successful === false) { " +
                "        failureLatch.countDown(); " +
                "    }" +
                "});");

        Assertions.assertTrue(failureLatch.await(5000));

        disconnect();
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testTransportNegotiationFailureForClientWebSocketServerLongPolling(String transport) throws Exception {
        initCometDServer(transport);

        // Only long-polling on server, only websocket on client.
        bayeuxServer.setAllowedTransports("long-polling");
        evaluateScript("keep_only_websocket_transport",
                "cometd.unregisterTransports();" +
                        "cometd.registerTransport('websocket', originalTransports['websocket']);");

        evaluateScript("var failureLatch = new Latch(2);");
        Latch failureLatch = javaScript.get("failureLatch");
        evaluateScript("cometd.onTransportException = function(failure, oldTransport, newTransport) {" +
                "    failureLatch.countDown();" +
                "}");
        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");
        evaluateScript("cometd.handshake(function(message) {" +
                "    if (message.successful === false) {" +
                "        failureLatch.countDown(); " +
                "    }" +
                "});");

        Assertions.assertTrue(failureLatch.await(5000));
        boolean disconnected = evaluateScript("cometd.isDisconnected();");
        Assertions.assertTrue(disconnected);
    }
}
