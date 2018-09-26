/*
 * Copyright 2018 (c) Vadim Tsesko <incubos@yandex.com>
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

package ru.mail.polis;

import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Basic init/deinit test for {@link KVService} implementation
 *
 * @author Vadim Tsesko <incubos@yandex.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StartStopTest extends TestBase {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);
    private static int port;
    private static File data;
    private static KVDao dao;
    private static KVService storage;
    @Rule
    public final Timeout globalTimeout = Timeout.millis(TIMEOUT_MS * 2);

    @BeforeClass
    public static void beforeAll() throws IOException {
        port = randomPort();
        data = Files.createTempDirectory();
        dao = KVDaoFactory.create(data);
    }

    @AfterClass
    public static void afterAll() throws IOException {
        dao.close();
        Files.recursiveDelete(data);
    }

    private static int status() throws IOException {
        return Request.Get("http://localhost:" + port + "/v0/status")
                .connectTimeout((int) TIMEOUT_MS)
                .socketTimeout((int) TIMEOUT_MS)
                .execute()
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
    }

    @Test
    public void create() throws Exception {
        storage = KVServiceFactory.create(port, dao);
        try {
            // Should not respond before start
            status();
        } catch (SocketTimeoutException e) {
            // Do nothing
        }
    }

    @Test
    public void start() throws Exception {
        storage.start();
        assertEquals(200, status());
    }

    @Test
    public void stop() throws Exception {
        storage.stop();
        try {
            // Should not respond after stop
            status();
        } catch (SocketTimeoutException | HttpHostConnectException e) {
            // Do nothing
        }
    }
}
