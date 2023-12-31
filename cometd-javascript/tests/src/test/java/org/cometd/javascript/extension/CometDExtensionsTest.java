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

import org.cometd.javascript.AbstractCometDTransportsTest;
import org.cometd.javascript.Latch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CometDExtensionsTest extends AbstractCometDTransportsTest {
    @ParameterizedTest
    @MethodSource("transports")
    public void testRegisterUnregister(String transport) throws Exception {
        initCometDServer(transport);

        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");
        evaluateScript("var inCount = 0;");
        evaluateScript("var outCount = 0;");
        evaluateScript("cometd.registerExtension('testin', {" +
                "incoming: function(message) { ++inCount; return message; }" +
                "});");
        evaluateScript("cometd.registerExtension('testout', {" +
                "outgoing: function(message) { ++outCount; return message; }" +
                "});");
        evaluateScript("cometd.registerExtension('testempty', {});");

        evaluateScript("var readyLatch = new Latch(1);");
        Latch readyLatch = javaScript.get("readyLatch");
        evaluateScript("cometd.addListener('/meta/connect', function() { readyLatch.countDown(); });");
        evaluateScript("cometd.handshake();");
        Assertions.assertTrue(readyLatch.await(5000));

        // Wait for the long poll to be established
        Thread.sleep(1000);

        Number inCount = javaScript.get("inCount");
        Number outCount = javaScript.get("outCount");
        Assertions.assertEquals(2, inCount.intValue()); // handshake, connect1
        Assertions.assertEquals(3, outCount.intValue()); // handshake, connect1, connect2

        Boolean unregistered = evaluateScript("cometd.unregisterExtension('testin');");
        Assertions.assertTrue(unregistered);
        unregistered = evaluateScript("cometd.unregisterExtension('testout');");
        Assertions.assertTrue(unregistered);

        evaluateScript("var publishLatch = new Latch(1);");
        Latch publishLatch = javaScript.get("publishLatch");
        evaluateScript("cometd.addListener('/meta/publish', function() { publishLatch.countDown(); });");
        evaluateScript("cometd.publish('/echo', 'ping');");
        Assertions.assertTrue(publishLatch.await(5000));

        inCount = javaScript.get("inCount");
        outCount = javaScript.get("outCount");
        Assertions.assertEquals(2, inCount.intValue());
        Assertions.assertEquals(3, outCount.intValue());

        disconnect();
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testExtensions(String transport) throws Exception {
        initCometDServer(transport);

        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");

        evaluateScript("" +
                "var Listener = Java.type('" + Listener.class.getName() + "');" +
                "var listener = new Listener();" +
                "cometd.registerExtension('testext', {" +
                "incoming: function(message) { listener.incoming(message); return message;}," +
                "outgoing: function(message) { listener.outgoing(message); return message;}" +
                "});");
        Listener listener = javaScript.get("listener");

        evaluateScript("var readyLatch = new Latch(1);");
        Latch readyLatch = javaScript.get("readyLatch");
        evaluateScript("cometd.addListener('/meta/connect', function() { readyLatch.countDown(); });");
        evaluateScript("cometd.handshake();");
        Assertions.assertTrue(readyLatch.await(5000));

        // Wait for the long poll to be established
        // Cannot rely on latches for this, since we need to intercept the connect2
        Thread.sleep(1000);

        Assertions.assertEquals(3, listener.getOutgoingMessageCount()); // handshake, connect1, connect2
        Assertions.assertEquals(2, listener.getIncomingMessageCount()); // handshake, connect1

        listener.reset();
        evaluateScript("var subscribeLatch = new Latch(1);");
        Latch subscribeLatch = javaScript.get("subscribeLatch");
        evaluateScript("cometd.addListener('/meta/subscribe', function() { subscribeLatch.countDown(); });");
        evaluateScript("var messageLatch = new Latch(1);");
        Latch messageLatch = javaScript.get("messageLatch");
        evaluateScript("var subscription = cometd.subscribe('/echo', function() { messageLatch.countDown(); });");
        Assertions.assertTrue(subscribeLatch.await(5000));
        Assertions.assertEquals(1, listener.getOutgoingMessageCount()); // subscribe
        Assertions.assertEquals(1, listener.getIncomingMessageCount()); // subscribe

        listener.reset();
        evaluateScript("var publishLatch = new Latch(1);");
        Latch publishLatch = javaScript.get("publishLatch");
        evaluateScript("cometd.addListener('/meta/publish', function() { publishLatch.countDown(); });");
        evaluateScript("cometd.publish('/echo', 'test');");
        Assertions.assertTrue(publishLatch.await(5000));
        Assertions.assertTrue(messageLatch.await(5000));
        Assertions.assertEquals(1, listener.getOutgoingMessageCount()); // publish
        Assertions.assertEquals(2, listener.getIncomingMessageCount()); // publish, message

        listener.reset();
        evaluateScript("var unsubscribeLatch = new Latch(1);");
        Latch unsubscribeLatch = javaScript.get("unsubscribeLatch");
        evaluateScript("cometd.addListener('/meta/unsubscribe', function() { unsubscribeLatch.countDown(); });");
        evaluateScript("cometd.unsubscribe(subscription);");
        Assertions.assertTrue(unsubscribeLatch.await(5000));
        Assertions.assertEquals(1, listener.getOutgoingMessageCount()); // unsubscribe
        Assertions.assertEquals(1, listener.getIncomingMessageCount()); // unsubscribe

        readyLatch.reset(1);
        listener.reset();
        disconnect();
        Assertions.assertTrue(readyLatch.await(5000));

        // Wait for the connect to return
        Thread.sleep(1000);

        Assertions.assertEquals(1, listener.getOutgoingMessageCount()); // disconnect
        Assertions.assertEquals(2, listener.getIncomingMessageCount()); // connect2, disconnect
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testExtensionOrder(String transport) throws Exception {
        initCometDServer(transport);

        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");

        String channelName = "/ext_order";

        evaluateScript("cometd.registerExtension('ext1', {" +
                "incoming: function(message) {" +
                "    if (message.channel === '" + channelName + "' && message.in_ext2 !== 1) {" +
                "        message.in_ext1 = 1;" +
                "    }" +
                "    return message;" +
                "}," +
                "outgoing: function(message) {" +
                "    if (message.channel === '" + channelName + "' && message.out_ext2 === 1) {" +
                "        message.out_ext1 = 1;" +
                "    }" +
                "}" +
                "});");
        evaluateScript("cometd.registerExtension('ext2', {" +
                "incoming: function(message) {" +
                "    if (message.channel === '" + channelName + "' && message.in_ext1 === 1) {" +
                "        message.in_ext2 = 1;" +
                "    }" +
                "    return message;" +
                "}," +
                "outgoing: function(message) {" +
                "    if (message.channel === '" + channelName + "' && message.out_ext1 !== 1) {" +
                "        message.out_ext2 = 1;" +
                "    }" +
                "}" +
                "});");

        evaluateScript("var latch = new Latch(1);");
        Latch latch = javaScript.get("latch");
        evaluateScript("cometd.handshake(function(handshakeReply) {" +
                "cometd.batch(function() {" +
                "    cometd.subscribe('" + channelName + "', function(m) {" +
                "        if (m.in_ext1 === 1 && m.in_ext2 === 1 && m.out_ext1 === 1 && m.out_ext2 === 1) {" +
                "            latch.countDown();" +
                "        } else {" +
                "            window.console.info('Wrong extension order', m);" +
                "        }" +
                "    });" +
                "    cometd.publish('" + channelName + "', 'wxyz');" +
                "});" +
                "});");

        Assertions.assertTrue(latch.await(5000));

        disconnect();
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testExtensionRegistrationCallbacks(String transport) throws Exception {
        initCometDServer(transport);

        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");
        evaluateScript("var n;");
        evaluateScript("var c;");
        evaluateScript("cometd.registerExtension('ext1', {" +
                "registered: function(name, cometd) {" +
                "    n = name;" +
                "    c = cometd;" +
                "}," +
                "unregistered: function() {" +
                "    n = null;" +
                "    c = null;" +
                "}" +
                "});");
        Object extName = javaScript.get("n");
        Assertions.assertNotNull(extName);
        Object extCometD = javaScript.get("c");
        Assertions.assertNotNull(extCometD);

        evaluateScript("cometd.unregisterExtension('ext1');");
        extName = javaScript.get("n");
        Assertions.assertNull(extName);
        extCometD = javaScript.get("c");
        Assertions.assertNull(extCometD);
    }

    public static class Listener {
        private int outgoing;
        private int incoming;

        public void outgoing(Object message) {
            ++outgoing;
        }

        public void incoming(Object message) {
            ++incoming;
        }

        public int getOutgoingMessageCount() {
            return outgoing;
        }

        public int getIncomingMessageCount() {
            return incoming;
        }

        public void reset() {
            incoming = 0;
            outgoing = 0;
        }
    }
}
