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
package org.cometd.client.http;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.client.BayeuxClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SubscriptionTest extends ClientServerTest {
    @Test
    public void testSubscriptionToMetaChannelFails() throws Exception {
        start(null);

        BayeuxClient client = newBayeuxClient();
        client.handshake();
        Assertions.assertTrue(client.waitFor(5000, BayeuxClient.State.CONNECTED));

        CountDownLatch latch = new CountDownLatch(1);
        String channelName = Channel.META_CONNECT;
        ClientSessionChannel channel = client.getChannel(channelName);
        channel.subscribe((c, m) -> {
        }, message -> {
            Assertions.assertFalse(message.isSuccessful());
            latch.countDown();
        });

        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(0, channel.getSubscribers().size());
        Assertions.assertEquals(0, bayeux.getChannel(channelName).getSubscribers().size());

        disconnectBayeuxClient(client);
    }

    @Test
    public void testSubscriptionToServiceChannelIsANoOperation() throws Exception {
        start(null);

        BayeuxClient client = newBayeuxClient();
        client.handshake();
        Assertions.assertTrue(client.waitFor(5000, BayeuxClient.State.CONNECTED));

        String channelName = "/service/test";
        CountDownLatch subscribeLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        ClientSessionChannel channel = client.getChannel(channelName);
        ClientSessionChannel.MessageListener listener = (c, m) -> messageLatch.countDown();
        channel.subscribe(listener, message -> {
            Assertions.assertTrue(message.isSuccessful());
            subscribeLatch.countDown();
        });

        Assertions.assertTrue(subscribeLatch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(1, channel.getSubscribers().size());
        Assertions.assertEquals(0, bayeux.getChannel(channelName).getSubscribers().size());

        channel.publish("test");

        Assertions.assertFalse(messageLatch.await(1, TimeUnit.SECONDS));

        CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        channel.unsubscribe(listener, message -> {
            Assertions.assertTrue(message.isSuccessful());
            unsubscribeLatch.countDown();
        });

        Assertions.assertTrue(subscribeLatch.await(5, TimeUnit.SECONDS));
        Assertions.assertEquals(0, channel.getSubscribers().size());

        disconnectBayeuxClient(client);
    }

    @Test
    public void testSubscriptionUnsubscriptionToSameChannelSentOnlyOnce() throws Exception {
        start(null);

        AtomicReference<CountDownLatch> subscribeLatch = new AtomicReference<>(new CountDownLatch(1));
        bayeux.getChannel(Channel.META_SUBSCRIBE).addListener(new ServerChannel.MessageListener() {
            @Override
            public boolean onMessage(ServerSession from, ServerChannel channel, ServerMessage.Mutable message) {
                subscribeLatch.get().countDown();
                return true;
            }
        });
        AtomicReference<CountDownLatch> unsubscribeLatch = new AtomicReference<>(new CountDownLatch(1));
        bayeux.getChannel(Channel.META_UNSUBSCRIBE).addListener(new ServerChannel.MessageListener() {
            @Override
            public boolean onMessage(ServerSession from, ServerChannel channel, ServerMessage.Mutable message) {
                unsubscribeLatch.get().countDown();
                return true;
            }
        });

        BayeuxClient client = newBayeuxClient();
        client.handshake();
        Assertions.assertTrue(client.waitFor(5000, BayeuxClient.State.CONNECTED));

        String channelName = "/foo";
        AtomicReference<CountDownLatch> replyLatch = new AtomicReference<>(new CountDownLatch(1));
        ClientSessionChannel.MessageListener listener1 = (c, m) -> {};
        ClientSessionChannel channel = client.getChannel(channelName);
        boolean result = channel.subscribe(listener1, reply -> replyLatch.get().countDown());

        Assertions.assertTrue(result);
        Assertions.assertTrue(subscribeLatch.get().await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(replyLatch.get().await(5, TimeUnit.SECONDS));

        // Try the same listener.
        subscribeLatch.set(new CountDownLatch(1));
        replyLatch.set(new CountDownLatch(1));
        result = channel.subscribe(listener1, reply -> replyLatch.get().countDown());

        Assertions.assertFalse(result);
        Assertions.assertFalse(subscribeLatch.get().await(500, TimeUnit.MILLISECONDS));
        Assertions.assertTrue(replyLatch.get().await(500, TimeUnit.MILLISECONDS));

        // Try a different listener.
        ClientSessionChannel.MessageListener listener2 = (c, m) -> {};
        replyLatch.set(new CountDownLatch(1));
        result = channel.subscribe(listener2, reply -> replyLatch.get().countDown());

        Assertions.assertFalse(result);
        Assertions.assertFalse(subscribeLatch.get().await(500, TimeUnit.MILLISECONDS));
        Assertions.assertTrue(replyLatch.get().await(500, TimeUnit.MILLISECONDS));

        replyLatch.set(new CountDownLatch(1));
        result = channel.unsubscribe(listener2, reply -> replyLatch.get().countDown());

        Assertions.assertFalse(result);
        Assertions.assertFalse(unsubscribeLatch.get().await(500, TimeUnit.MILLISECONDS));
        Assertions.assertTrue(replyLatch.get().await(500, TimeUnit.MILLISECONDS));

        replyLatch.set(new CountDownLatch(1));
        result = channel.unsubscribe(listener1, reply -> replyLatch.get().countDown());

        Assertions.assertFalse(result);
        Assertions.assertFalse(unsubscribeLatch.get().await(500, TimeUnit.MILLISECONDS));
        Assertions.assertTrue(replyLatch.get().await(500, TimeUnit.MILLISECONDS));

        replyLatch.set(new CountDownLatch(1));
        result = channel.unsubscribe(listener1, reply -> replyLatch.get().countDown());

        Assertions.assertTrue(result);
        Assertions.assertTrue(unsubscribeLatch.get().await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(replyLatch.get().await(5, TimeUnit.SECONDS));

        disconnectBayeuxClient(client);
    }
}
