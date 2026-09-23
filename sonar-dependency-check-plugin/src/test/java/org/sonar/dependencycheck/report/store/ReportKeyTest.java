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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportKeyTest {

    @Test
    void fallsBackToTheDefaultScopeWithoutBranchOrPullRequest() {
        assertEquals("my-project/default/dependency-check-report.html", ReportKey.of("my-project", null, null));
    }

    @Test
    void usesBranchWhenGiven() {
        assertEquals("my-project/br-develop/dependency-check-report.html", ReportKey.of("my-project", "develop", null));
    }

    @Test
    void pullRequestWinsOverBranch() {
        assertEquals("my-project/pr-42/dependency-check-report.html", ReportKey.of("my-project", "develop", "42"));
    }

    /**
     * The whole point of the encoding: a user who may create a project must not be able to pick a
     * key that lands on another project's report.
     */
    @Test
    void projectKeysDifferingOnlyInSeparatorsDoNotCollide() {
        Set<String> keys = new HashSet<>();
        keys.add(ReportKey.of("com.acme:web", null, null));
        keys.add(ReportKey.of("com.acme/web", null, null));
        keys.add(ReportKey.of("com.acme_web", null, null));
        assertEquals(3, keys.size(), "every project key must map to its own storage key, got " + keys);
    }

    @Test
    void branchNamesDifferingOnlyInSeparatorsDoNotCollide() {
        assertNotEquals(ReportKey.of("p", "feature/SONAR-1", null), ReportKey.of("p", "feature_SONAR-1", null));
    }

    @Test
    void neverEmitsAnUnencodedDotOrParentDirectoryReference() {
        String key = ReportKey.of("../../etc", "..", null);
        assertFalse(key.contains(".."), "key must not contain a parent directory reference: " + key);
        assertEquals(1, key.split("/").length - 2, "key must have exactly three segments: " + key);
        // Only the fixed file name may carry a literal dot.
        String withoutFileName = key.substring(0, key.length() - ReportKey.FILE_NAME.length());
        assertFalse(withoutFileName.contains("."), "no encoded segment may contain a literal dot: " + key);
    }

    @Test
    void branchNamedDefaultDiffersFromNoBranch() {
        assertNotEquals(ReportKey.of("p", "default", null), ReportKey.of("p", null, null));
        assertEquals("p/br-default/dependency-check-report.html", ReportKey.of("p", "default", null));
    }

    @Test
    void pullRequestDoesNotCollideWithABranchNamedLikeIt() {
        assertNotEquals(ReportKey.of("p", null, "42"), ReportKey.of("p", "pr-42", null));
    }

    @Test
    void keepsOrdinaryNamesRecognisable() {
        String key = ReportKey.of("my-project", "release-1.2", null);
        assertTrue(key.startsWith("my-project/br-release-1"), "key should stay readable: " + key);
        assertEquals("my-project/br-release-1%2E2/dependency-check-report.html", key);
    }

    @Test
    void treatsBlankBranchAndPullRequestAsAbsent() {
        assertEquals("p/default/dependency-check-report.html", ReportKey.of("p", "   ", "  "));
    }

    @Test
    void emptyProjectKeyCollidesWithNothing() {
        String empty = ReportKey.of("", null, null);
        assertEquals("%/default/dependency-check-report.html", empty);
        assertNotEquals(empty, ReportKey.of("_", null, null));
        assertNotEquals(empty, ReportKey.of("%", null, null));
        assertNotEquals(empty, ReportKey.of(".", null, null));
    }

    @Test
    void encodesNonAsciiCharactersAsUtf8Bytes() {
        assertEquals("caf%C3%A9", ReportKey.encode("café"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.acme:web", "feature/x", "café", "a%2Fb", "..", ""})
    void decodeRoundTripsEncode(String value) {
        assertEquals(value, ReportKey.decode(ReportKey.encode(value)));
    }

    @Test
    void decodeThrowsOnAPercentNotFollowedByTwoHexDigits() {
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("%2"));
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("%2G"));
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("abc%"));
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("abc%2"));
    }

    @Test
    void decodeOfASingleLiteralPercentSignDecodesToTheEmptyString() {
        assertEquals("", ReportKey.decode("%"));
    }

    @Test
    void projectKeyOfReturnsTheDecodedFirstSegment() {
        String key = ReportKey.of("com.acme:web", "main", null);
        assertEquals(Optional.of("com.acme:web"), ReportKey.projectKeyOf(key));
    }

    @Test
    void projectKeyOfIsEmptyForAMalformedKey() {
        assertEquals(Optional.empty(), ReportKey.projectKeyOf("not-a-valid-key"));
        assertEquals(Optional.empty(), ReportKey.projectKeyOf("a/b/c/d"));
        assertEquals(Optional.empty(), ReportKey.projectKeyOf(""));
    }

    /**
     * Three segments alone are not a report key. The store directory may hold unrelated files, and
     * mistaking one for a report would make cleanup delete a stranger's file.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "logs/2026-09/sonar.log",
        "p/default/notes.txt",
        "p/main/dependency-check-report.html",
        "p/br/dependency-check-report.html",
        "p/br-/dependency-check-report.html",
        "p/pr/dependency-check-report.html",
        "p/Default/dependency-check-report.html",
        "p/default/dependency-check-report.html/extra",
        "p/dependency-check-report.html"
    })
    void isNotAReportKey(String storeKey) {
        assertFalse(ReportKey.isReportKey(storeKey), storeKey);
        assertEquals(Optional.empty(), ReportKey.projectKeyOf(storeKey));
        assertEquals(Optional.empty(), ReportKey.scopeOf(storeKey));
    }

    @Test
    void recognisesTheKeysItProducesItself() {
        assertTrue(ReportKey.isReportKey(ReportKey.of("com.acme:web", null, null)));
        assertTrue(ReportKey.isReportKey(ReportKey.of("com.acme:web", "feature/x", null)));
        assertTrue(ReportKey.isReportKey(ReportKey.of("com.acme:web", null, "42")));
    }

    @Test
    void scopeOfReturnsTheRawMiddleSegment() {
        String key = ReportKey.of("com.acme:web", "feature/x", null);
        assertEquals(Optional.of("br-feature%2Fx"), ReportKey.scopeOf(key));
    }

    @Test
    void scopeOfIsEmptyForAMalformedKey() {
        assertEquals(Optional.empty(), ReportKey.scopeOf("not-a-valid-key"));
        assertEquals(Optional.empty(), ReportKey.scopeOf("a/b/c/d"));
    }

    /**
     * The scope vocabulary has exactly one home. Anything else spelling out "br-", "pr-" or
     * "default" could drift from what {@link ReportKey#of} writes, and a drifted scope makes the
     * reconciliation classify every live report as an orphan.
     */
    @Test
    void scopeVocabularyMatchesTheKeysOfProduces() {
        assertEquals(ReportKey.of("p", null, null),
                ReportKey.prefixOf("p") + ReportKey.DEFAULT_SCOPE + "/" + ReportKey.FILE_NAME);
        assertEquals(ReportKey.of("p", "feature/x", null),
                ReportKey.prefixOf("p") + ReportKey.branchScope("feature/x") + "/" + ReportKey.FILE_NAME);
        assertEquals(ReportKey.of("p", null, "42"),
                ReportKey.prefixOf("p") + ReportKey.pullRequestScope("42") + "/" + ReportKey.FILE_NAME);
        assertEquals(Optional.of(ReportKey.branchScope("feature/x")),
                ReportKey.scopeOf(ReportKey.of("p", "feature/x", null)));
    }

    @Test
    void prefixOfMatchesTheFirstSegmentOfOf() {
        String projectKey = "com.acme:web";
        String key = ReportKey.of(projectKey, "main", null);
        String firstSegment = key.substring(0, key.indexOf('/') + 1);
        assertEquals(firstSegment, ReportKey.prefixOf(projectKey));
    }

    /**
     * decode() wrote every char through ByteArrayOutputStream.write(int), which truncates anything
     * above U+00FF - so "münchen" came back as "m?nchen" instead of being rejected.
     */
    @Test
    void decodeRefusesACharacterEncodeNeverEmits() {
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("münchen"));
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("中文"));
        assertThrows(IllegalArgumentException.class, () -> ReportKey.decode("a.b"));
    }

    /**
     * A directory named "münchen" holding the report file name has the shape of a report key.
     * Accepting it means the cleanup decodes a mangled project key, is told the project is gone,
     * and deletes a stranger's file - exactly what this class promises never to do.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "münchen/default/dependency-check-report.html",
            "中文/br-main/dependency-check-report.html",
            "p/br-münchen/dependency-check-report.html",
            "p/pr-ä/dependency-check-report.html",
            "com.acme/default/dependency-check-report.html",
            "p/br-%2e/dependency-check-report.html"
    })
    void keysNotSpelledTheWayEncodeSpellsThemAreNotReportKeys(String key) {
        assertFalse(ReportKey.isReportKey(key), key + " must not be recognised as a report key");
        assertFalse(ReportKey.projectKeyOf(key).isPresent());
        assertFalse(ReportKey.scopeOf(key).isPresent());
    }

    /**
     * A directory literally named "%" holding a file of the expected name has the shape of a key
     * of ours - "%" is what encode() produces for the empty string - but no project has an empty
     * key, so it is not one, and the cleanup must not be handed it.
     */
    @Test
    void aKeyWhoseProjectSegmentDecodesToNothingHasNoProjectKey() {
        assertFalse(ReportKey.projectKeyOf("%/default/dependency-check-report.html").isPresent(),
                "an empty project key is not a project key");
        assertFalse(ReportKey.projectKeyOf("%/br-main/dependency-check-report.html").isPresent());
    }

    /** The keys this class really produces still round-trip, non-ASCII project keys included. */
    @ParameterizedTest
    @ValueSource(strings = {"münchen", "com.acme:web", "中文", "p"})
    void keysProducedByOfStayRecognisable(String projectKey) {
        String key = ReportKey.of(projectKey, "feature/ü", null);
        assertTrue(ReportKey.isReportKey(key));
        assertEquals(Optional.of(projectKey), ReportKey.projectKeyOf(key));
    }
}
