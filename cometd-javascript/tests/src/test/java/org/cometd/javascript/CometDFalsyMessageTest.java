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

import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.ServerSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CometDFalsyMessageTest extends AbstractCometDTransportsTest {
    @ParameterizedTest
    @MethodSource("transports")
    public void testEmptyStringMessage(String transport) throws Exception {
        testFalsyMessage(transport, "");
    }

    @ParameterizedTest
    @MethodSource("transports")
    public void testZeroMessage(String transport) throws Exception {
        testFalsyMessage(transport, 0);
    }

    private void testFalsyMessage(String transport, Object content) throws Exception {
        initCometDServer(transport);

        String channelName = "/foo";
        evaluateScript("cometd.configure({url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "'});");

        evaluateScript("var messageLatch = new Latch(1);");
        Latch messageLatch = javaScript.get("messageLatch");
        evaluateScript("cometd.addListener('/meta/handshake', function(m) {" +
                "    if (m.successful) {" +
                "        cometd.subscribe('" + channelName + "', function() { messageLatch.countDown(); });" +
                "    }" +
                "});");

        evaluateScript("var subscribeLatch = new Latch(1);");
        Latch subscribeLatch = javaScript.get("subscribeLatch");
        evaluateScript("cometd.addListener('/meta/subscribe', function() { subscribeLatch.countDown(); });");

        evaluateScript("cometd.handshake();");
        Assertions.assertTrue(subscribeLatch.await(5000));

        String sessionId = evaluateScript("cometd.getClientId();");
        ServerSession session = bayeuxServer.getSession(sessionId);
        session.deliver(null, channelName, content, Promise.noop());

        Assertions.assertTrue(messageLatch.await(5000));
    }
}
