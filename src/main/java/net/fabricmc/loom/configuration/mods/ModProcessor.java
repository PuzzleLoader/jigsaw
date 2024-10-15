/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2023 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.configuration.mods;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.RemapConfigurationSettings;
import net.fabricmc.loom.configuration.mods.dependency.ModDependency;
import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.kotlin.KotlinClasspathService;
import net.fabricmc.loom.util.kotlin.KotlinRemapperClassloader;
import net.fabricmc.loom.util.service.ServiceFactory;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

public class ModProcessor {
	private static final Pattern COPY_CONFIGURATION_PATTERN = Pattern.compile("^(.+)Copy[0-9]*$");

	private final Project project;
	private final Configuration sourceConfiguration;
	private final ServiceFactory serviceFactory;

	public ModProcessor(Project project, Configuration sourceConfiguration, ServiceFactory serviceFactory) {
		this.project = project;
		this.sourceConfiguration = sourceConfiguration;
		this.serviceFactory = serviceFactory;
	}

	public void processMods(List<ModDependency> remapList) throws IOException {
		try {
			project.getLogger().lifecycle(":remapping {} mods from {}", remapList.size(), describeConfiguration(sourceConfiguration));
		} catch (Exception e) {
			throw new RuntimeException(String.format(Locale.ENGLISH, "Failed to remap %d mods", remapList.size()), e);
		}
	}

	// Creates a human-readable descriptive string for the configuration.
	// This consists primarily of the name with any copy suffixes stripped
	// (they're not informative), and the usage attribute if present.
	private String describeConfiguration(Configuration configuration) {
		String description = configuration.getName();
		final Matcher copyMatcher = COPY_CONFIGURATION_PATTERN.matcher(description);

		// If we find a copy suffix, remove it.
		if (copyMatcher.matches()) {
			final String realName = copyMatcher.group(1);

			// It's only a copy if we find a non-copy version.
			if (project.getConfigurations().findByName(realName) != null) {
				description = realName;
			}
		}

		// Add the usage if present, e.g. "modImplementation (java-api)"
		final Usage usage = configuration.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE);

		if (usage != null) {
			description += " (" + usage.getName() + ")";
		}

		return description;
	}

	private void stripNestedJars(Path path) {
		// Strip out all contained jar info as we dont want loader to try and load the jars contained in dev.
		try {
			ZipUtils.transformJson(JsonObject.class, path, Map.of("fabric.mod.json", json -> {
				json.remove("jars");
				return json;
			}));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to strip nested jars from %s".formatted(path), e);
		}
	}

}
