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


import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Files} facilities
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
class FilesTest {
    @Test
    void createRemove() throws IOException {
        final File dir = Files.createTempDirectory();
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());

        final File data = new File(dir, "data");
        assertFalse(data.exists());
        assertTrue(data.createNewFile());
        assertTrue(data.isFile());

        Files.recursiveDelete(dir);
        assertFalse(dir.exists());
    }
}
