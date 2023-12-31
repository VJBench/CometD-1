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
package org.cometd.javascript.extension;

import org.cometd.bayeux.Promise;
import org.cometd.javascript.AbstractCometDTransportsTest;
import org.cometd.javascript.Latch;
import org.cometd.server.AbstractService;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.ext.AcknowledgedMessagesExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CometDAckExtensionTest extends AbstractCometDTransportsTest {
    private AckService ackService;

    @Override
    public void initCometDServer(String transport) throws Exception {
        super.initCometDServer(transport);
        bayeuxServer.addExtension(new AcknowledgedMessagesExtension());
        ackService = new AckService(bayeuxServer);
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testClientSupportsAckExtension(String transport) throws Exception {
        initCometDServer(transport);

        evaluateScript("var cometd = cometd;");
        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");

        // Check that during handshake the ack extension capability is sent to server
        evaluateScript("var clientSupportsAck = false;");
        evaluateScript("cometd.registerExtension('test', {" +
                "outgoing: function(message) {" +
                "   if (message.channel == '/meta/handshake') {" +
                "       clientSupportsAck = message.ext && message.ext.ack;" +
                "   }" +
                "   return message;" +
                "}" +
                "});");
        provideMessageAcknowledgeExtension();

        evaluateScript("var readyLatch = new Latch(1);");
        Latch readyLatch = javaScript.get("readyLatch");
        evaluateScript("cometd.addListener('/meta/handshake', function() { readyLatch.countDown(); });");
        evaluateScript("cometd.handshake();");

        Assertions.assertTrue(readyLatch.await(5000));

        Boolean clientSupportsAck = javaScript.get("clientSupportsAck");
        Assertions.assertTrue(clientSupportsAck);
        evaluateScript("cometd.unregisterExtension('test');");

        disconnect();
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testAcknowledgement(String transport) throws Exception {
        initCometDServer(transport);

        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");

        evaluateScript("var inAckId = undefined;");
        evaluateScript("var outAckId = undefined;");
        evaluateScript("cometd.registerExtension('test', {" +
                "incoming: function(message) {" +
                "   if (message.channel == '/meta/connect') {" +
                "       inAckId = message.ext && message.ext.ack;" +
                "   }" +
                "   return message;" +
                "}," +
                "outgoing: function(message) {" +
                "   if (message.channel == '/meta/connect') {" +
                "       outAckId = message.ext && message.ext.ack;" +
                "   }" +
                "   return message;" +
                "}" +
                "});");
        provideMessageAcknowledgeExtension();

        evaluateScript("var readyLatch = new Latch(1);");
        Latch readyLatch = javaScript.get("readyLatch");
        evaluateScript("cometd.addListener('/meta/connect', function() { readyLatch.countDown(); });");
        evaluateScript("cometd.handshake();");

        Assertions.assertTrue(readyLatch.await(5000));

        Number inAckId = javaScript.get("inAckId");
        // The server should have returned a non-negative value during the first connect call
        Assertions.assertTrue(inAckId.intValue() >= 0);

        // Subscribe to receive server events
        evaluateScript("var msgCount = 0;");
        evaluateScript("var subscribeLatch = new Latch(1);");
        Latch subscribeLatch = javaScript.get("subscribeLatch");
        evaluateScript("var publishLatch = new Latch(1);");
        Latch publishLatch = javaScript.get("publishLatch");
        evaluateScript("cometd.addListener('/meta/subscribe', function() { subscribeLatch.countDown(); });");
        evaluateScript("cometd.subscribe('/echo', function() { ++msgCount; publishLatch.countDown(); });");
        Assertions.assertTrue(subscribeLatch.await(5000));

        // The server receives an event and sends it to the client via the long poll
        ackService.emit("test acknowledgement");
        Assertions.assertTrue(publishLatch.await(5000));

        inAckId = javaScript.get("inAckId");
        Number outAckId = javaScript.get("outAckId");
        Assertions.assertTrue(inAckId.intValue() >= outAckId.intValue());
        Number msgCount = javaScript.get("msgCount");
        Assertions.assertEquals(1, msgCount.intValue());

        evaluateScript("cometd.unregisterExtension('test');");

        disconnect();
    }

    public static class AckService extends AbstractService {
        private AckService(BayeuxServerImpl bayeux) {
            super(bayeux, "ack-test");
        }

        public void emit(String content) {
            getBayeux().getChannel("/echo").publish(getServerSession(), content, Promise.noop());
        }
    }
}
