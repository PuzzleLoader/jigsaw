/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020-2022 FabricMC
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

package net.fabricmc.loom.build.mixin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.tasks.SourceSet;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ide.idea.IdeaUtils;
import net.fabricmc.loom.configuration.providers.cosmicreach.CosmicReachSourceSets;
import net.fabricmc.loom.extension.MixinExtension;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.LoomVersions;

/**
 * Normally javac invokes annotation processors, but when the scala or kapt plugin are installed they will want to invoke
 * the annotation processor themselves.
 * See Java and Kapt implementations for a more deep understanding of the things passed by the children.
 */
public abstract class AnnotationProcessorInvoker<T extends Task> {
	public static final String JAVA = "java";
	public static final String SCALA = "scala";
	public static final String GROOVY = "groovy";

	private static final Pattern MSG_KEY_PATTERN = Pattern.compile("^[A-Z]+[A-Z_]+$");
	private static final Pattern MSG_VALUE_PATTERN = Pattern.compile("^(note|warning|error|disabled)$");

	protected final Project project;
	private final LoomGradleExtension loomExtension;
	protected final MixinExtension mixinExtension;
	protected final Map<SourceSet, T> invokerTasks;
	private final String name;
	private final Collection<Configuration> apConfigurations;

	protected AnnotationProcessorInvoker(Project project,
											Collection<Configuration> apConfigurations,
											Map<SourceSet, T> invokerTasks, String name) {
		this.project = project;
		this.loomExtension = LoomGradleExtension.get(project);
		this.mixinExtension = loomExtension.getMixin();
		this.apConfigurations = apConfigurations;
		this.invokerTasks = invokerTasks;
		this.name = name;
	}

	protected static Collection<Configuration> getApConfigurations(Project project, Function<SourceSet, String> getApConfigNameFunc) {
		MixinExtension mixin = LoomGradleExtension.get(project).getMixin();
		return mixin.getApConfigurationsStream(getApConfigNameFunc).collect(Collectors.toList());
	}

	protected abstract void passArgument(T compileTask, String key, String value);

	private void passMixinArguments(T task, SourceSet sourceSet) {
		Map<String, String> args = new HashMap<>() {{
				put(Constants.MixinArguments.QUIET, "true");
			}};

		if (mixinExtension.getShowMessageTypes().get()) {
			args.put(Constants.MixinArguments.SHOW_MESSAGE_TYPES, "true");
		}

		mixinExtension.getMessages().get().forEach((key, value) -> {
			checkPattern(key, MSG_KEY_PATTERN);
			checkPattern(value, MSG_VALUE_PATTERN);

			args.put("MSG_" + key, value);
		});

		args.forEach((k, v) -> passArgument(task, k, v));
	}

	public void configureMixin() {
		ConfigurationContainer configs = project.getConfigurations();

		if (!IdeaUtils.isIdeaSync()) {
			for (Configuration processorConfig : apConfigurations) {
				project.getLogger().info("Adding mixin to classpath of AP config: " + processorConfig.getName());
				// Pass named MC classpath to mixin AP classpath
				processorConfig.extendsFrom(
						configs.getByName(Constants.Configurations.LOADER_DEPENDENCIES)
				);

				// Add Mixin and mixin extensions (fabric-mixin-compile-extensions pulls mixin itself too)
//				project.getDependencies().add(processorConfig.getName(),
//						LoomVersions.MIXIN_COMPILE_EXTENSIONS.mavenNotation());
			}
		}

		for (Map.Entry<SourceSet, T> entry : invokerTasks.entrySet()) {
			passMixinArguments(entry.getValue(), entry.getKey());
		}
	}

	private static void checkPattern(String input, Pattern pattern) {
		final Matcher matcher = pattern.matcher(input);

		if (!matcher.find()) {
			throw new IllegalArgumentException("Mixin argument (%s) does not match pattern (%s)".formatted(input, pattern.toString()));
		}
	}

}
