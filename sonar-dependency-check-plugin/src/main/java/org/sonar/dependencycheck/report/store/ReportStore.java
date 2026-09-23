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

import java.io.Closeable;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Where published Dependency-Check HTML reports are kept. Implementations are created per request
 * and closed afterwards.
 *
 * <p>Reports are passed as streams, not byte arrays: they reach tens of megabytes on large
 * projects and the web server should never hold one completely in memory.
 */
public interface ReportStore extends Closeable {

    /**
     * Stores the report under the given key, replacing any report stored under it before. The
     * caller keeps ownership of the stream and closes it; the store only reads from it.
     *
     * @return a human readable location for logging, for example an absolute path or an s3 URI
     */
    String store(String key, InputStream report) throws ReportStoreException;

    /**
     * Reads the report stored under the given key, or an empty optional if there is none. The
     * caller closes the returned stream.
     */
    Optional<InputStream> read(String key) throws ReportStoreException;

    /**
     * Whether a report is stored under the given key, without reading it. A HEAD probe only needs
     * this much, and reading a report of tens of megabytes to answer it is pure waste.
     */
    boolean exists(String key) throws ReportStoreException;

    /**
     * Lists the full store keys of every report stored under the given prefix. An empty prefix
     * lists the whole store. A prefix that matches nothing yields an empty list, not an error.
     */
    List<String> list(String prefix) throws ReportStoreException;

    /**
     * Deletes the report stored under the given key.
     *
     * @return true if a report was removed, false if there was nothing stored under the key
     */
    boolean delete(String key) throws ReportStoreException;

    @Override
    void close();
}
