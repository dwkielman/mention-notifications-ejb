/*
 * Copyright (c) 2016-2017, Mihai Emil Andronache
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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.json.JsonObject;

import org.junit.Test;

/**
 * Unit tests for {@link Mention}
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 1.0.0
 */
public final class MentionTestCase {

    /**
     * Mention can filter an empty list.
     * @throws IOException If something goes wrong.
     */
    @Test
    public void worksWithEmptyList() throws IOException {
        final List<JsonObject> filtered = new Mention().filter(
            new Notifications.FakeEmptyNotifications().fetch()
        );
        assertTrue(filtered.isEmpty());
    }

    /**
     * Mention can filter out all the notifications, if all of them are bad.
     * @throws IOException If something goes wrong.
     */
    @Test
    public void returnsEmptyList() throws IOException {
    	final List<JsonObject> filtered = new Mention().filter(
            new Notifications.FakeOtherNotifications().fetch()
        );
        assertTrue(filtered.isEmpty());
    }

    /**
     * Mention can filter notifications properly.
     * @throws IOException If something goes wrong.
     */
    @Test
    public void returnsGoodNotifications() throws IOException {
        final List<JsonObject> filtered = new Mention().filter(
            new Notifications.FakeNotificationsWithMentions().fetch()
        );
        assertTrue(filtered.size() == 2);
    }

}
