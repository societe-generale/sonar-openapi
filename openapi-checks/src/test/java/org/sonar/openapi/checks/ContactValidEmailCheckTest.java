/*
 * SonarQube OpenAPI Plugin
 * Copyright (C) 2018-2019 Societe Generale
 * vincent.girard-reydet AT socgen DOT com
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
package org.sonar.openapi.checks;

import org.junit.Test;
import org.sonar.openapi.OpenApiCheckVerifier;

public class ContactValidEmailCheckTest {
    @Test
    public void verify_contact_emailFormat_in_v2() {
        OpenApiCheckVerifier.verify("src/test/resources/checks/v2/contact-validemail.yaml", new ContactValidEmailCheck(), true);
    }

    @Test
    public void verify_contact_emailFormat_in_v3() {
        OpenApiCheckVerifier.verify("src/test/resources/checks/v3/contact-validemail.yaml", new ContactValidEmailCheck(), false);
    }

    @Test
    public void verify_contact_emailFormat_in_v2_2() {
        OpenApiCheckVerifier.verify("src/test/resources/checks/v2/contact-validemail_2.yaml", new ContactValidEmailCheck(), true);
    }

    @Test
    public void verify_contact_emailFormat_in_v3_2() {
        OpenApiCheckVerifier.verify("src/test/resources/checks/v3/contact-validemail_2.yaml", new ContactValidEmailCheck(), false);
    }
}
