/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.ide.idea.language.service;

import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlText;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dominik Marks
 */
public class LiferayServiceXMLDuplicateExceptionInspection extends AbstractLiferayServiceXMLDuplicateEntryInspection {

	@Nls
	@NotNull
	@Override
	public String getDisplayName() {
		return "check for duplicate exception entries";
	}

	@Nullable
	@Override
	public String getStaticDescription() {
		return "Check for duplicate exception entries in service.xml.";
	}

	@Override
	protected boolean isSuitableXmlAttributeValue(XmlAttributeValue xmlAttributeValue) {
		return false;
	}

	@Override
	protected boolean isSuitableXmlText(XmlText xmlText) {
		return LiferayServiceXMLUtil.isExceptionTag(xmlText);
	}

}