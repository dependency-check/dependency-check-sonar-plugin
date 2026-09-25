/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2025 dependency-check
 * philipp.dallig@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.dependencycheck.report.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemReportStoreTest {

    private static InputStream bytes(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * A report carries a private project's full vulnerability list. It is spooled through a
     * temporary file created by Files.createTempFile with mode 0600, and that mode has to survive
     * into the store - otherwise any local account on the SonarQube host can read it.
     */
    @Test
    void storedReportIsNotReadableByGroupOrOthers(@TempDir Path root) throws Exception {
        Path stored = root.resolve("p/main/report.html");
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p/main/report.html", bytes("<html>secret</html>"));
        }
        java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions;
        try {
            permissions = Files.getPosixFilePermissions(stored);
        } catch (UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "no POSIX file attributes on this filesystem");
            return;
        }
        assertFalse(permissions.contains(java.nio.file.attribute.PosixFilePermission.GROUP_READ),
                "the stored report must not be group readable");
        assertFalse(permissions.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_READ),
                "the stored report must not be world readable");
    }

    @Test
    void existsTellsAStoredReportFromAMissingOne(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertFalse(store.exists("p/main/report.html"));
            store.store("p/main/report.html", bytes("<html>hi</html>"));
            assertTrue(store.exists("p/main/report.html"));
            assertFalse(store.exists("p/main"));
        }
    }

    /**
     * An empty key resolves to the store root. Deleting it would hand the directory pruning a
     * parent outside the store and walk it upwards.
     */
    @Test
    void deletingTheStoreRootIsRefused(@TempDir Path root) throws Exception {
        Path store = root.resolve("reports");
        Files.createDirectories(store);
        try (FilesystemReportStore reportStore = new FilesystemReportStore(store)) {
            assertThrows(ReportStoreException.class, () -> reportStore.delete(""));
        }
        assertTrue(Files.isDirectory(store));
        assertTrue(Files.isDirectory(root));
    }

    @Test
    void storesAndReadsBack(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p/main/report.html", bytes("<html>hi</html>"));
            Optional<InputStream> report = store.read("p/main/report.html");
            assertTrue(report.isPresent());
            assertEquals("<html>hi</html>", read(report.get()));
        }
    }

    @Test
    void returnsLocationOfStoredFile(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            String location = store.store("p/main/report.html", bytes("x"));
            assertEquals(root.resolve("p/main/report.html").toString(), location);
        }
    }

    @Test
    void readReturnsEmptyForUnknownKey(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertFalse(store.read("p/main/report.html").isPresent());
        }
    }

    @Test
    void replacesExistingReport(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p/main/report.html", bytes("old"));
            store.store("p/main/report.html", bytes("new"));
            assertEquals("new", read(store.read("p/main/report.html").get()));
        }
    }

    @Test
    void leavesNoTemporaryFilesBehind(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p/main/report.html", bytes("x"));
        }
        try (java.util.stream.Stream<Path> files = Files.walk(root)) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void rejectsKeyEscapingTheRoot(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertThrows(ReportStoreException.class, () -> store.store("../escaped.html", bytes("x")));
        }
    }

    @Test
    void rejectsAbsoluteKey(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertThrows(ReportStoreException.class, () -> store.read("/etc/passwd"));
        }
    }

    @Test
    void rejectsKeyLeavingTheRootThroughASymlink(@TempDir Path root, @TempDir Path outside) throws Exception {
        Files.createSymbolicLink(root.resolve("escape"), outside);
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertThrows(ReportStoreException.class, () -> store.store("escape/report.html", bytes("x")));
        }
    }

    @Test
    void listReturnsStoredKeysUnderAPrefix(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p1/main/report.html", bytes("a"));
            store.store("p1/pr-1/report.html", bytes("b"));
            store.store("p2/main/report.html", bytes("c"));

            List<String> all = store.list("");
            assertEquals(3, all.size());
            assertTrue(all.contains("p1/main/report.html"));
            assertTrue(all.contains("p1/pr-1/report.html"));
            assertTrue(all.contains("p2/main/report.html"));
        }
    }

    @Test
    void listWithAProjectPrefixReturnsOnlyThatProjectsKeys(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p1/main/report.html", bytes("a"));
            store.store("p1/pr-1/report.html", bytes("b"));
            store.store("p2/main/report.html", bytes("c"));

            List<String> p1 = store.list("p1/");
            assertEquals(2, p1.size());
            assertTrue(p1.contains("p1/main/report.html"));
            assertTrue(p1.contains("p1/pr-1/report.html"));
        }
    }

    /**
     * A crashed upload can leave a spool file behind. It is not a report, so listing must not
     * offer it to the cleanup as a key.
     */
    @Test
    void listIgnoresLeftoverSpoolFiles(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p1/main/report.html", bytes("a"));
            java.nio.file.Files.write(root.resolve("p1/main/dependency-check-12345.tmp"),
                    "half written".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            List<String> all = store.list("");
            assertEquals(1, all.size(), "got " + all);
            assertTrue(all.contains("p1/main/report.html"));
        }
    }

    @Test
    void listingAnUnknownPrefixIsEmpty(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertTrue(store.list("does-not-exist/").isEmpty());
        }
    }

    @Test
    void deleteRemovesTheFileAndReturnsTrue(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p1/main/report.html", bytes("a"));
            assertTrue(store.delete("p1/main/report.html"));
            assertFalse(store.read("p1/main/report.html").isPresent());
        }
    }

    @Test
    void deletingAMissingKeyReturnsFalse(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertFalse(store.delete("p1/main/report.html"));
        }
    }

    @Test
    void deletePrunesEmptiedDirectoriesButNeverTheRoot(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p1/main/report.html", bytes("a"));
            assertTrue(store.delete("p1/main/report.html"));
            assertFalse(Files.exists(root.resolve("p1/main")));
            assertFalse(Files.exists(root.resolve("p1")));
            assertTrue(Files.exists(root));
        }
    }

    @Test
    void deleteDoesNotPruneADirectoryThatStillHasOtherReports(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            store.store("p1/main/report.html", bytes("a"));
            store.store("p1/pr-1/report.html", bytes("b"));
            assertTrue(store.delete("p1/main/report.html"));
            assertFalse(Files.exists(root.resolve("p1/main")));
            assertTrue(Files.exists(root.resolve("p1")));
            assertTrue(Files.exists(root.resolve("p1/pr-1/report.html")));
        }
    }

    @Test
    void rejectsPrefixEscapingTheRoot(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertThrows(ReportStoreException.class, () -> store.list("../"));
        }
    }

    @Test
    void rejectsKeyEscapingTheRootOnDelete(@TempDir Path root) throws Exception {
        try (FilesystemReportStore store = new FilesystemReportStore(root)) {
            assertThrows(ReportStoreException.class, () -> store.delete("../escaped.html"));
        }
    }
}
