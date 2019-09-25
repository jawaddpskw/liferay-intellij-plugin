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

package com.liferay.ide.idea.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.impl.JavaHomeFinder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Terry Jia
 * @author Simon Jiang
 */
public class LiferayWorkspaceUtil {

	public static String getHomeDir(String location) {
		String result = _getGradleProperty(
			location, WorkspaceConstants.HOME_DIR_PROPERTY, WorkspaceConstants.HOME_DIR_DEFAULT);

		if ((result == null) || result.equals("")) {
			return WorkspaceConstants.HOME_DIR_DEFAULT;
		}

		return result;
	}

	public static boolean getIndexSources(Project project) {
		String result = "false";

		VirtualFile workspaceVirtualFile = getWorkspaceVirtualFile(project);

		if (workspaceVirtualFile != null) {
			VirtualFile gradlePropertiesVirtualFile = workspaceVirtualFile.findFileByRelativePath("/gradle.properties");

			if (gradlePropertiesVirtualFile != null) {
				Properties properties = new Properties();

				try {
					properties.load(gradlePropertiesVirtualFile.getInputStream());

					result = properties.getProperty(WorkspaceConstants.DEFAULT_TARGET_PLATFORM_INDEX_SOURCES_PROPERTY);
				}
				catch (IOException ioe) {
				}
			}
		}

		return Boolean.parseBoolean(result);
	}

	@Nullable
	public static String getLiferayVersion(Project project) {
		PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);

		String liferayVersion = propertiesComponent.getValue(WorkspaceConstants.WIZARD_LIFERAY_VERSION_FIELD);

		if (liferayVersion != null) {
			return liferayVersion;
		}

		VirtualFile projectRoot = getWorkspaceVirtualFile(project);

		if (projectRoot == null) {
			return null;
		}

		VirtualFile settingsVirtualFile = projectRoot.findFileByRelativePath("/.blade.properties");

		if (settingsVirtualFile != null) {
			Properties props = new Properties();

			try {
				props.load(settingsVirtualFile.getInputStream());

				liferayVersion = props.getProperty(WorkspaceConstants.BLADE_LIFERAY_VERSION_FIELD);
			}
			catch (IOException ioe) {
			}
		}

		return liferayVersion;
	}

	@Nullable
	public static String getMavenProperty(Project project, String key, String defaultValue) {
		if (!isValidMavenWorkspaceLocation(project)) {
			return null;
		}

		MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);

		MavenProject mavenWorkspaceProject = mavenProjectsManager.findContainingProject(
			getWorkspaceVirtualFile(project));

		if (mavenWorkspaceProject == null) {
			return defaultValue;
		}

		Properties properties = mavenWorkspaceProject.getProperties();

		return properties.getProperty(key, defaultValue);
	}

	@NotNull
	public static String getModuleExtDir(Project project) {
		String retval = null;

		if (project != null) {
			String projectLocation = project.getBasePath();

			if (projectLocation != null) {
				retval = _getGradleProperty(
					projectLocation, WorkspaceConstants.DEFAULT_EXT_DIR_PROPERTY, WorkspaceConstants.DEFAULT_EXT_DIR);
			}
		}

		if (CoreUtil.isNullOrEmpty(retval)) {
			return WorkspaceConstants.DEFAULT_EXT_DIR;
		}

		return retval;
	}

	@Nullable
	public static VirtualFile getModuleExtDirFile(Project project) {
		if (project == null) {
			return null;
		}

		String moduleExtDir = getModuleExtDir(project);

		File file = new File(moduleExtDir);

		if (!file.isAbsolute()) {
			String projectBasePath = project.getBasePath();

			if (projectBasePath == null) {
				return null;
			}

			file = new File(projectBasePath, moduleExtDir);
		}

		LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

		return localFileSystem.findFileByPath(file.getPath());
	}

	@NotNull
	public static String getModulesDir(Project project) {
		String retval = null;

		if (project != null) {
			String projectLocation = project.getBasePath();

			if (projectLocation != null) {
				retval = _getGradleProperty(
					projectLocation, WorkspaceConstants.MODULES_DIR_PROPERTY, WorkspaceConstants.MODULES_DIR_DEFAULT);
			}
		}

		if (CoreUtil.isNullOrEmpty(retval)) {
			return WorkspaceConstants.MODULES_DIR_DEFAULT;
		}

		return retval;
	}

	public static List<LibraryData> getTargetPlatformArtifacts(Project project) {
		ProjectDataManager projectDataManager = ProjectDataManager.getInstance();

		Collection<ExternalProjectInfo> externalProjectInfos = projectDataManager.getExternalProjectsData(
			project, GradleConstants.SYSTEM_ID);

		for (ExternalProjectInfo externalProjectInfo : externalProjectInfos) {
			DataNode<ProjectData> projectData = externalProjectInfo.getExternalProjectStructure();

			if (projectData == null) {
				continue;
			}

			Collection<DataNode<?>> dataNodes = projectData.getChildren();

			List<LibraryData> libraryData = new ArrayList<>(dataNodes.size());

			for (DataNode<?> child : dataNodes) {
				if (!ProjectKeys.LIBRARY.equals(child.getKey())) {
					continue;
				}

				libraryData.add((LibraryData)child.getData());
			}

			libraryData.sort(
				Comparator.comparing(LibraryData::getArtifactId, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));

			return libraryData;
		}

		return Collections.emptyList();
	}

	public static List<String> getTargetPlatformDependencies(Project project) {
		String targetPlatformVersion = getTargetPlatformVersion(project);

		List<String> targetPlatformDependencyList = _targetPlatformDependenciesMap.get(targetPlatformVersion);

		if ((targetPlatformDependencyList != null) && !targetPlatformDependencyList.isEmpty()) {
			return targetPlatformDependencyList;
		}

		List<String> javaHomePaths = JavaHomeFinder.suggestHomePaths();

		if (javaHomePaths.isEmpty()) {
			return Collections.emptyList();
		}

		GradleConnector gradleConnector = GradleConnector.newConnector();

		File file = new File(project.getBasePath());

		gradleConnector = gradleConnector.forProjectDirectory(file);

		ProjectConnection projectConnection = gradleConnector.connect();

		BuildLauncher build = projectConnection.newBuild();

		OutputStream outputStream = new ByteArrayOutputStream();

		build = build.setJavaHome(new File(javaHomePaths.get(0)));

		build = build.forTasks("dependencyManagement");

		build = build.setStandardOutput(outputStream);

		build.run();

		String output = outputStream.toString();

		List<String> list = new ArrayList<>();

		if (!output.equals("")) {
			BufferedReader bufferedReader = new BufferedReader(new StringReader(output));

			String line;

			try {
				boolean start = false;

				while ((line = bufferedReader.readLine()) != null) {
					if (Objects.equals("compileOnly - Dependency management for the compileOnly configuration", line)) {
						start = true;

						continue;
					}

					if (start) {
						if (StringUtil.equals(line.trim(), "")) {
							break;
						}

						list.add(line.trim());
					}
				}
			}
			catch (IOException ioe) {
			}
		}

		_targetPlatformDependenciesMap.put(targetPlatformVersion, list);

		return list;
	}

	@Nullable
	public static String getTargetPlatformVersion(Project project) {
		String location = project.getBasePath();

		return _getGradleProperty(location, WorkspaceConstants.DEFAULT_TARGET_PLATFORM_VERSION_PROPERTY, null);
	}

	@NotNull
	public static String getWarsDir(Project project) {
		String retval = null;

		if (project != null) {
			String projectLocation = project.getBasePath();

			if (projectLocation != null) {
				retval = _getGradleProperty(
					projectLocation, WorkspaceConstants.DEFAULT_WARS_DIR_PROPERTY, WorkspaceConstants.DEFAULT_WARS_DIR);
			}
		}

		if (CoreUtil.isNullOrEmpty(retval)) {
			return WorkspaceConstants.DEFAULT_WARS_DIR;
		}

		return retval;
	}

	@Nullable
	public static VirtualFile getWorkspaceVirtualFile(@Nullable Project project) {
		if (project == null) {
			return null;
		}

		String projectBasePath = project.getBasePath();

		if (projectBasePath == null) {
			return null;
		}

		LocalFileSystem fileSystem = LocalFileSystem.getInstance();

		return fileSystem.findFileByPath(projectBasePath);
	}

	public static boolean isValidGradleWorkspaceLocation(@Nullable String location) {
		if (location == null) {
			return false;
		}

		File workspaceDir = new File(location);

		File buildGradle = new File(workspaceDir, _BUILD_GRADLE_FILE_NAME);
		File settingsGradle = new File(workspaceDir, _SETTINGS_GRADLE_FILE_NAME);
		File gradleProperties = new File(workspaceDir, _GRADLE_PROPERTIES_FILE_NAME);

		if (!(buildGradle.exists() && settingsGradle.exists() && gradleProperties.exists())) {
			return false;
		}

		String settingsContent = FileUtil.readContents(settingsGradle, true);

		Matcher matcher = _patternWorkspacePlugin.matcher(settingsContent);

		return matcher.matches();
	}

	public static boolean isValidGradleWorkspaceProject(Project project) {
		return isValidGradleWorkspaceLocation(project.getBasePath());
	}

	public static boolean isValidMavenWorkspaceLocation(Project project) {
		if (project == null) {
			return false;
		}

		try {
			MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);

			if (!mavenProjectsManager.isMavenizedProject()) {
				return false;
			}

			VirtualFile workspaceVirtualFile = getWorkspaceVirtualFile(project);

			if (workspaceVirtualFile == null) {
				return false;
			}

			MavenProject mavenWorkspaceProject = mavenProjectsManager.findContainingProject(workspaceVirtualFile);

			if (mavenWorkspaceProject == null) {
				return false;
			}

			MavenPlugin liferayWorkspacePlugin = mavenWorkspaceProject.findPlugin(
				"com.liferay", "com.liferay.portal.tools.bundle.support");

			if (liferayWorkspacePlugin != null) {
				return true;
			}
		}
		catch (Exception e) {
			return false;
		}

		return false;
	}

	public static boolean isValidWorkspaceLocation(Project project) {
		if ((project != null) &&
			(isValidGradleWorkspaceLocation(project.getBasePath()) || isValidMavenWorkspaceLocation(project))) {

			return true;
		}

		return false;
	}

	private static String _getGradleProperty(String projectLocation, String key, String defaultValue) {
		File gradleProperties = new File(projectLocation, "gradle.properties");

		if (gradleProperties.exists()) {
			Properties properties = PropertiesUtil.loadProperties(gradleProperties);

			if (properties == null) {
				return defaultValue;
			}

			return properties.getProperty(key, defaultValue);
		}

		return "";
	}

	private static final String _BUILD_GRADLE_FILE_NAME = "build.gradle";

	private static final String _GRADLE_PROPERTIES_FILE_NAME = "gradle.properties";

	private static final String _SETTINGS_GRADLE_FILE_NAME = "settings.gradle";

	private static final Pattern _patternWorkspacePlugin = Pattern.compile(
		".*apply.*plugin.*:.*[\'\"]com\\.liferay\\.workspace[\'\"].*", Pattern.MULTILINE | Pattern.DOTALL);
	private static Map<String, List<String>> _targetPlatformDependenciesMap = new HashMap<>();

}