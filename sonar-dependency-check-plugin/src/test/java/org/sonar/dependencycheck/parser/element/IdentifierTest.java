package org.sonar.dependencycheck.parser.element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdentifierTest {

    @Test
    void testMaven() {
        Identifier a = new Identifier("pkg:maven/struts/struts@1.2.8", Confidence.HIGH);
        assertTrue(Identifier.isMavenPackage(a));
        assertFalse(Identifier.isNPMPackage(a));
        assertEquals("struts/struts@1.2.8", Identifier.getPackageArtefact(a).get());
        assertEquals("maven", Identifier.getPackageType(a).get());
    }

    @Test
    void testNode() {
        Identifier a = new Identifier("pkg:npm/braces@1.8.5", Confidence.HIGHEST);
        assertFalse(Identifier.isMavenPackage(a));
        assertTrue(Identifier.isNPMPackage(a));
        assertEquals("braces@1.8.5", Identifier.getPackageArtefact(a).get());
        assertEquals("npm", Identifier.getPackageType(a).get());
    }

}
