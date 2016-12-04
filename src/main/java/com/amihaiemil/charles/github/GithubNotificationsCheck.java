/*
 * Copyright (c) 2016, Mihai Emil Andronache
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1)Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  2)Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *  3)Neither the name of mention-notifications-ejb nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.amihaiemil.charles.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerService;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcabi.http.Request;
import com.jcabi.http.request.ApacheRequest;
import com.jcabi.http.response.RestResponse;

/**
 * EJB that checks periodically for github notifications (mentions of the agent using @username).
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id: 3d9b4dcc6885f853200060cecd9ce7d0d33ed129 $
 * @since 1.0.0
 * 
 */
@Singleton
@Startup
public class GithubNotificationsCheck {

    /**
     * Logger. Assigned in ctor for leveraging unit testing.
     */
    private Logger log;

    /**
     * Java EE timer service.
     */
    @Resource
    private TimerService timerService;

    /**
     * Github notifications.
     */
    private Notifications notificationsApi;

    /**
     * Api token.
     */
    private String token;

    /**
     * Default Ctor.
     */
    public GithubNotificationsCheck() {
        this(
            "https://api.github.com/notifications",
            LoggerFactory.getLogger(GithubNotificationsCheck.class.getName())
        );
    }

    /**
     * Ctor.
     * @param notificationsEp - Endpoint for Github notifications' check
     * @param logger - for leveraging unit testing.
     */
    public GithubNotificationsCheck(String edp, Logger logger) {
        this.log = logger;
        this.token = System.getProperty("github.auth.token");
        if(token == null) {
        	throw new IllegalStateException("Missing github.auth.token sys property!");
        }
        this.notificationsApi = new RtNotifications(new Mention(), token, edp);
    }

    /**
     * After this bean is constructed the checks are scheduled at a given interval (minutes),
     * which defaults to 2.
     */
    @PostConstruct
    public void scheduleChecks() {
        String checksInterval = System.getProperty("checks.interval.minutes");
        int intervalMinutes = 2;
        if(checksInterval != null && !checksInterval.isEmpty()) {
            try {
                intervalMinutes = Integer.parseInt(checksInterval);
                log.info("The check for Github notifications will be performed every " + intervalMinutes + " minutes!");
            } catch (NumberFormatException ex) {
                log.error("NumberFormatException when parsing interval " + checksInterval, ex);
            }
        }
        timerService.createTimer(1000*60*intervalMinutes, 1000*60*intervalMinutes, null);
    }

    /**
     * Read notifications from the Github API when the scheduler timeout occurs.
     */
    @Timeout
    public void readNotifications() {
        String handlerEndpoint = System.getProperty("post.rest.endpoint");
        if(handlerEndpoint == null || handlerEndpoint.isEmpty()) {
            log.error("Missing charles.rest.roken system property! Please specify the REST endpoint where notifications are posted!");
        } else {
        	try {
        	    List<JsonObject> notifications = this.notificationsApi.fetch();
                log.info("POST-ing " + notifications.size() + " valid notifications!");
                boolean posted = this.postNotifications(handlerEndpoint, token, notifications);
                if(posted) {//if the notifications were successfully posted to the REST service, mark them as read.
                    log.info("POST successful, marking notifications as read...");
                    this.notificationsApi.request().uri()
                        .queryParam(
                            "last_read_at",
                            DateFormatUtils.formatUTC(
                                new Date(System.currentTimeMillis()),
                                "yyyy-MM-dd'T'HH:mm:ss'Z'"
                            )
                        ).back()
                         .method(Request.PUT).body().set("{}").back().fetch()
                         .as(RestResponse.class)
                         .assertStatus(
                             Matchers.isOneOf(
                                 HttpURLConnection.HTTP_OK,
                                 HttpURLConnection.HTTP_RESET
                             )
                         );
                    log.info("Notifications marked as read!");
                }
        	} catch (AssertionError aerr) {
                log.error("Unexpected HTTP status!", aerr);
            } catch (IOException e) {
                log.error("IOException when making HTTP call!", e);
            }
        }
    }

    /**
     * Sends simplified notifications to the REST endpoint.
     * <br><br>
     * Only send the repoFullName and issueNumber from each notification.
     * The following handling logic (finding the first mentioning comment etc) is done
     * on the other side.
     * @param endpoint REST endpoint.
     * @param token Authorization token between this agent and the handling REST endpoint.
     * @param notifications Github notifications.
     * @return true if the Noifications were successfully posted, false otherwise.
     */
    public boolean postNotifications(String endpoint, String token, List<JsonObject> notifications) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for(JsonObject notification : notifications) {
            JsonObject subject = notification.getJsonObject("subject");
            String issueUrl = subject.getString("url");
            arrayBuilder.add(
                Json.createObjectBuilder()
                    .add("repoFullName", notification.getJsonObject("repository").getString("full_name"))
                    .add("issueNumber", Integer.parseInt(issueUrl.substring(issueUrl.lastIndexOf("/") + 1)))
                    .build()
            );
        }
        Request postReq = new ApacheRequest(endpoint);
        postReq = postReq.header(
            HttpHeaders.AUTHORIZATION, token
        );
        try {
            int status = postReq.method(Request.POST).body().set(arrayBuilder.build()).back()
                .fetch()
                .as(RestResponse.class)
                .assertStatus(
                    Matchers.isOneOf(
                        HttpURLConnection.HTTP_OK,
                        HttpURLConnection.HTTP_UNAUTHORIZED
                    )
                ).status();
            if(status == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (AssertionError aerr) {
            log.error("Unexpected status from " + endpoint, aerr);
        } catch (IOException e) {
            log.error("IOException when calling " + endpoint, e);
        }
        return false;
    }
    
}
