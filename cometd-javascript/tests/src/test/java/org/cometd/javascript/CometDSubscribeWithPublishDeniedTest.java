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

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.DefaultSecurityPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CometDSubscribeWithPublishDeniedTest extends AbstractCometDTransportsTest {
    @ParameterizedTest
    @MethodSource("transports")
    public void testSubscribeWithPublishDenied(String transport) throws Exception {
        initCometDServer(transport);

        bayeuxServer.setSecurityPolicy(new Policy());

        evaluateScript("var subscribeLatch = new Latch(1);");
        Latch subscribeLatch = javaScript.get("subscribeLatch");
        evaluateScript("var publishLatch = new Latch(1);");
        Latch publishLatch = javaScript.get("publishLatch");
        evaluateScript("" +
                "var subscribeFailed = false;" +
                "var publishFailed = false;" +
                "" +
                "var channelName = '/foo';" +
                "cometd.addListener('/meta/handshake', function() {" +
                "    cometd.subscribe(channelName, function() {});" +
                "});" +
                "" +
                "cometd.addListener('/meta/subscribe', function(message) {" +
                "    if (!message.successful) {" +
                "        subscribeFailed = true;" +
                "    }" +
                "    subscribeLatch.countDown();" +
                "});" +
                "" +
                "cometd.addListener('/meta/publish', function(message) {" +
                "    if (!message.successful) {" +
                "        publishFailed = true;" +
                "    }" +
                "    publishLatch.countDown();" +
                "});" +
                "" +
                "cometd.init({ url: '" + cometdURL + "', logLevel: '" + getLogLevel() + "' });" +
                "");
        Assertions.assertTrue(subscribeLatch.await(5000));
        boolean subscribeFailed = javaScript.get("subscribeFailed");
        Assertions.assertFalse(subscribeFailed);

        evaluateScript("" +
                "cometd.publish(channelName, {});" +
                "");

        Assertions.assertTrue(publishLatch.await(5000));
        boolean publishFailed = javaScript.get("publishFailed");
        // Denied by policy
        Assertions.assertTrue(publishFailed);

        disconnect();
    }

    private static class Policy extends DefaultSecurityPolicy {
        @Override
        public boolean canPublish(BayeuxServer server, ServerSession session, ServerChannel channel, ServerMessage message) {
            return false;
        }
    }
}
