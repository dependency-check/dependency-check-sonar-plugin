/*
 * Dependency-Check Plugin for SonarQube
 * Copyright (C) 2015-2017 Steve Springett
 * steve.springett@owasp.org
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
package org.sonar.dependencycheck.rule;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.utils.ValidationMessages;

import java.io.Reader;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/**
 * @author Gregor Tudan, Cofinpro AG
 */
public class NeutralProfileTest {
    private XMLProfileParser profileParser;
    private NeutralProfile neutralProfile;

    @Before
    public void setUp() throws Exception {
        this.profileParser = mock(XMLProfileParser.class);
        this.neutralProfile = new NeutralProfile(profileParser);
    }

    @Test
    public void createProfile() throws Exception {
        final ValidationMessages messages = ValidationMessages.create();
        neutralProfile.createProfile(messages);

        final ArgumentCaptor<Reader> captor = ArgumentCaptor.forClass(Reader.class);

        verify(profileParser).parse(captor.capture(), same(messages));
        final Reader reader = captor.getValue();

        assertThat(reader).isNotNull();
        final String content = IOUtils.toString(reader);
        assertThat(content).contains("<profile>");
    }

}