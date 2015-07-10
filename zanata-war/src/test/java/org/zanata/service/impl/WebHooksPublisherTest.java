/*
 * Copyright 2015, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.zanata.service.impl;

import com.google.common.base.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.zanata.events.WebhookEventType;
import org.zanata.util.HmacUtil;


import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpCallback.callback;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class WebHooksPublisherTest {

    private int port = 8180;
    private String postUrl = "http://localhost:" + port + WebHookPublisherCallback.LISTENER_PATH;
    private String serverUrl = "http://localhost/zanata";

    //mock server to listen to http POST
    private MockServerClient mockServerClient;

    @After
    public void stop() {
        mockServerClient.stop();
    }

    @Before
    public void init() {
        mockServerClient = startClientAndServer(port);
        mockServerClient
                .when(request().withPath(WebHookPublisherCallback.LISTENER_PATH))
                .callback(
                    callback()
                        .withCallbackClass(
                            "org.zanata.service.impl.WebHookPublisherCallback")
                );
    }

    @Test
    public void test1() {
        final String key = "secret_key";
        final String eventName = "org.zanata.events.TestEvent";
        final String json = "{data: 'testing', event, '" + eventName + "'}";

        WebhookEventType event = createEvent(eventName, json);

        Response res = WebHooksPublisher.publish(postUrl, event, Optional.of(key),
                serverUrl);

        String receivedSHA = res.getHeaderString(
            WebHooksPublisher.WEBHOOK_HEADER);

        String receivedBody = res.readEntity(String.class);
        String validateSHA =
                HmacUtil.hmacSha1(key, HmacUtil.hmacSha1(key, receivedBody + serverUrl));

        assertThat(validateSHA).isEqualTo(receivedSHA);

        mockServerClient.verify(
                request()
                        .withMethod("POST")
                        .withPath(WebHookPublisherCallback.LISTENER_PATH)
                        .withBody(json),
                exactly(1)
                );
    }

    private WebhookEventType createEvent(final String eventName,
            final String json) {
        return new WebhookEventType() {
            @Override
            public String getEventType() {
                return eventName;
            }

            @Override
            public String getJSON() {
                return json;
            }
        };
    }
}
