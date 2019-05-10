/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.idea.language.service;

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

/**
 * @author Dominik Marks
 */
public class LiferayServiceXMLPrimaryKeyColumnInspectionTest extends LightCodeInsightFixtureTestCase {

    public void testInvalidPrimaryKeyInspection() {
        myFixture.configureByFiles("service_invalid.xml");

        myFixture.checkHighlighting();
    }

    public void testValidPrimaryKeyInspection() {
        myFixture.configureByFiles("service_valid.xml");

        myFixture.checkHighlighting();
    }

    @Override
    protected String getTestDataPath() {
        return "testdata/com/liferay/ide/idea/language/service/LiferayServiceXMLPrimaryKeyColumnInspectionTest";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        myFixture.enableInspections(new LiferayServiceXMLPrimaryKeyColumnInspection());
    }

}
