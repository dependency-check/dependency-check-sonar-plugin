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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.dependencycheck.base.DependencyCheckConstants;

class ReportStoresTest {

    @Test
    void emptyWhenStoreIsNotConfigured() throws Exception {
        assertFalse(ReportStores.create(new MapSettings().asConfig()).isPresent());
    }

    @Test
    void emptyForStoreNone() throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "none");
        assertFalse(ReportStores.create(settings.asConfig()).isPresent());
    }

    @Test
    void createsFilesystemStore() throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "filesystem");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY, "/tmp/dc-reports");
        Optional<ReportStore> store = ReportStores.create(settings.asConfig());
        assertTrue(store.isPresent());
        assertTrue(store.get() instanceof FilesystemReportStore);
        store.get().close();
    }

    @Test
    void filesystemStoreWithoutPathIsRejected() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "filesystem");
        assertThrows(ReportStoreException.class, () -> ReportStores.create(settings.asConfig()));
    }

    /**
     * A blank value is not a configured directory: Paths.get("") is the SonarQube working
     * directory, so the store would be rooted in the installation and a cleanup with dryRun=false
     * would walk it.
     */
    @Test
    void filesystemStoreWithABlankPathIsRejected() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "filesystem");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY, "   ");
        ReportStoreException e = assertThrows(ReportStoreException.class, () -> ReportStores.create(settings.asConfig()));
        assertTrue(e.getMessage().contains(DependencyCheckConstants.HTML_REPORT_FILESYSTEM_PATH_PROPERTY),
                "the message must name the property to fix: " + e.getMessage());
    }

    @Test
    void s3StoreWithABlankBucketIsRejected() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "s3");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY, "  ");
        ReportStoreException e = assertThrows(ReportStoreException.class, () -> ReportStores.create(settings.asConfig()));
        assertTrue(e.getMessage().contains(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY),
                "the message must name the property to fix: " + e.getMessage());
    }

    /** Half a key pair used to reach the SDK and leave an unguarded NullPointerException. */
    @Test
    void s3StoreWithABlankSecretAccessKeyIsRejected() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "s3");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_BUCKET_PROPERTY, "reports");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_ACCESS_KEY_ID_PROPERTY, "key");
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_S3_SECRET_ACCESS_KEY_PROPERTY, "  ");
        ReportStoreException e = assertThrows(ReportStoreException.class, () -> ReportStores.create(settings.asConfig()));
        assertTrue(e.getMessage().contains(DependencyCheckConstants.HTML_REPORT_S3_SECRET_ACCESS_KEY_PROPERTY),
                "the message must name the property to fix: " + e.getMessage());
    }

    /**
     * A blank value is how the store is switched off again. Passing it on reached fromKey("") and
     * threw, which showed up as a 500 on every upload and every page view.
     */
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", " ", "   "})
    void aBlankStoreTypeMeansNoStore(String configured) throws Exception {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, configured);
        assertFalse(ReportStores.create(settings.asConfig()).isPresent(),
                "a blank store property must mean 'none', exactly like an absent one");
    }

    @Test
    void unknownStoreTypeIsRejected() {
        MapSettings settings = new MapSettings();
        settings.setProperty(DependencyCheckConstants.HTML_REPORT_STORE_PROPERTY, "ftp");
        assertThrows(ReportStoreException.class, () -> ReportStores.create(settings.asConfig()));
    }
}
