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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps reports in a directory. The directory is read and written by the SonarQube server only, so
 * a local disk is enough for a single node and a shared volume for a cluster.
 */
public class FilesystemReportStore implements ReportStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemReportStore.class);
    private static final String TEMP_PREFIX = "dependency-check-";
    private static final String TEMP_SUFFIX = ".tmp";

    private final Path root;

    public FilesystemReportStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, InputStream report) throws ReportStoreException {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            // Write beside the target and move it into place, so that a concurrent reader never
            // sees a half written report.
            Path temporary = Files.createTempFile(target.getParent(), TEMP_PREFIX, TEMP_SUFFIX);
            try {
                // Written through an OutputStream, not with Files.copy(REPLACE_EXISTING): that
                // deletes the 0600 file createTempFile just made and re-creates it with the
                // process umask, which the move would then carry into the store - leaving a
                // private project's vulnerability report readable by every local account.
                try (OutputStream out = Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    copy(report, out);
                }
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.deleteIfExists(temporary);
                throw e;
            }
            return target.toString();
        } catch (IOException e) {
            throw new ReportStoreException("Could not write the report to " + target, e);
        }
    }

    /** Copies {@code in} to {@code out} without holding the report in memory. */
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    @Override
    public Optional<InputStream> read(String key) throws ReportStoreException {
        Path source = resolve(key);
        if (!Files.isRegularFile(source)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.newInputStream(source));
        } catch (IOException e) {
            throw new ReportStoreException("Could not read the report from " + source, e);
        }
    }

    @Override
    public boolean exists(String key) throws ReportStoreException {
        return Files.isRegularFile(resolve(key));
    }

    @Override
    public List<String> list(String prefix) throws ReportStoreException {
        Path base = resolve(prefix);
        if (!Files.isDirectory(base)) {
            return new ArrayList<>();
        }
        List<String> keys = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(base)) {
            paths.filter(Files::isRegularFile)
                    .filter(file -> !isSpoolFile(file))
                    .forEach(file -> keys.add(toKey(file)));
        } catch (IOException e) {
            throw new ReportStoreException("Could not list the reports under " + base, e);
        }
        return keys;
    }

    @Override
    public boolean delete(String key) throws ReportStoreException {
        Path target = resolve(key);
        if (target.equals(root)) {
            // An empty key resolves to the root itself. Deleting it would hand the pruning below a
            // parent outside the store and walk it upwards.
            throw new ReportStoreException("Refusing to delete the report store directory itself: " + root);
        }
        boolean deleted;
        try {
            deleted = Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new ReportStoreException("Could not delete the report at " + target, e);
        }
        if (deleted) {
            // Deliberately outside the try above: the report is gone at this point, so the return
            // value must say so. Pruning the directories it left behind is cosmetic housekeeping,
            // and reporting its failure as a failed delete would claim the report is still there.
            try {
                pruneEmptyDirectories(target.getParent());
            } catch (IOException e) {
                LOGGER.debug("Could not prune the emptied directories below {}: {}", target.getParent(), e.getMessage());
            }
        }
        return deleted;
    }

    @Override
    public void close() {
        // nothing to release
    }

    /**
     * Whether {@code file} is a leftover of an interrupted {@link #store}. Those spool files are
     * named by {@link Files#createTempFile} with the prefix and suffix used there; they are not
     * reports, so listing must not hand them out as keys.
     */
    private static boolean isSpoolFile(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith(TEMP_PREFIX) && name.endsWith(TEMP_SUFFIX);
    }

    /** The key of a file below the root, with '/' as separator regardless of platform. */
    private String toKey(Path file) {
        Path relative = root.relativize(file);
        StringBuilder key = new StringBuilder();
        for (Path part : relative) {
            if (key.length() > 0) {
                key.append('/');
            }
            key.append(part.toString());
        }
        return key.toString();
    }

    /**
     * Removes {@code directory} and each of its ancestors that became empty, stopping at - and
     * never removing - the store root.
     */
    private void pruneEmptyDirectories(Path directory) throws IOException {
        Path current = directory;
        while (current != null && !current.equals(root) && current.startsWith(root) && isEmptyDirectory(current)) {
            Files.delete(current);
            current = current.getParent();
        }
    }

    private boolean isEmptyDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(directory)) {
            return !entries.findAny().isPresent();
        }
    }

    /**
     * Resolves the key (or prefix) below the root and refuses anything that would leave it.
     * {@link ReportKey} already normalises the key, but the store is the last line of defence and
     * does not rely on its caller. An empty string resolves to the root itself.
     */
    private Path resolve(String key) throws ReportStoreException {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new ReportStoreException("Report key points outside of the store directory: " + key);
        }
        // normalize() is purely lexical. If a component of the path is a symlink, that check passes
        // while the real target sits outside the root, so the nearest existing ancestor is resolved
        // and checked again. The target itself usually does not exist yet on a write.
        try {
            if (!Files.exists(root)) {
                return resolved;
            }
            Path realRoot = root.toRealPath();
            Path existing = resolved;
            while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                existing = existing.getParent();
            }
            if (existing != null && !existing.toRealPath().startsWith(realRoot)) {
                throw new ReportStoreException("Report key leaves the store directory through a link: " + key);
            }
        } catch (IOException e) {
            throw new ReportStoreException("Could not verify that the report key stays inside the store directory: " + key, e);
        }
        return resolved;
    }
}
