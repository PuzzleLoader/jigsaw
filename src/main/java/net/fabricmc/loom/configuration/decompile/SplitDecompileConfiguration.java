/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.loom.configuration.decompile;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import net.fabricmc.loom.api.decompilers.DecompilerOptions;
import net.fabricmc.loom.configuration.providers.cosmicreach.CosmicReachJar;
import net.fabricmc.loom.configuration.providers.cosmicreach.mapped.MappedCosmicReachProvider;
import net.fabricmc.loom.task.GenerateSourcesTask;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.Strings;

public final class SplitDecompileConfiguration extends DecompileConfiguration<MappedCosmicReachProvider.Split> {
	public SplitDecompileConfiguration(Project project, MappedCosmicReachProvider.Split minecraftProvider) {
		super(project, minecraftProvider);
	}

	@Override
	public String getTaskName(CosmicReachJar.Type type) {
		return "gen%sSources".formatted(Strings.capitalize(type.toString()));
	}

	@Override
	public void afterEvaluation() {
		final CosmicReachJar commonJar = minecraftProvider.getCommonJar();
		final CosmicReachJar clientOnlyJar = minecraftProvider.getClientOnlyJar();

		final TaskProvider<Task> commonDecompileTask = createDecompileTasks("Common", task -> {
			task.getInputJarName().set(commonJar.getName());
			task.getSourcesOutputJar().fileValue(GenerateSourcesTask.getJarFileWithSuffix("-sources.jar", commonJar.getPath()));

			if (mappingConfiguration.hasUnpickDefinitions()) {
				File unpickJar = new File(extension.getMappingConfiguration().mappingsWorkingDir().toFile(), "minecraft-common-unpicked.jar");
				configureUnpick(task, unpickJar);
			}
		});

		final TaskProvider<Task> clientOnlyDecompileTask = createDecompileTasks("ClientOnly", task -> {
			task.getInputJarName().set(clientOnlyJar.getName());
			task.getSourcesOutputJar().fileValue(GenerateSourcesTask.getJarFileWithSuffix("-sources.jar", clientOnlyJar.getPath()));

			if (mappingConfiguration.hasUnpickDefinitions()) {
				File unpickJar = new File(extension.getMappingConfiguration().mappingsWorkingDir().toFile(), "minecraft-clientonly-unpicked.jar");
				configureUnpick(task, unpickJar);
			}

			// Don't allow them to run at the same time.
			task.mustRunAfter(commonDecompileTask);
		});

		for (DecompilerOptions options : extension.getDecompilerOptions()) {
			final String decompilerName = options.getFormattedName();

			project.getTasks().register("genSourcesWith" + decompilerName, task -> {
				task.setDescription("Decompile minecraft using %s.".formatted(decompilerName));
				task.setGroup(Constants.TaskGroup.PUZZLE);

				task.dependsOn(project.getTasks().named("gen%sSourcesWith%s".formatted("Common", decompilerName)));
				task.dependsOn(project.getTasks().named("gen%sSourcesWith%s".formatted("ClientOnly", decompilerName)));
			});
		}

		project.getTasks().register("genSources", task -> {
			task.setDescription("Decompile minecraft using the default decompiler.");
			task.setGroup(Constants.TaskGroup.PUZZLE);

			task.dependsOn(commonDecompileTask);
			task.dependsOn(clientOnlyDecompileTask);
		});
	}

	private TaskProvider<Task> createDecompileTasks(String name, Action<GenerateSourcesTask> configureAction) {
		extension.getDecompilerOptions().forEach(options -> {
			final String decompilerName = options.getFormattedName();
			final String taskName = "gen%sSourcesWith%s".formatted(name, decompilerName);

			project.getTasks().register(taskName, GenerateSourcesTask.class, options).configure(task -> {
				configureAction.execute(task);
				task.dependsOn(project.getTasks().named("validateAccessWidener"));
				task.setDescription("Decompile minecraft using %s.".formatted(decompilerName));
				task.setGroup(Constants.TaskGroup.PUZZLE);
			});
		});

		return project.getTasks().register("gen%sSources".formatted(name), task -> {
			task.setDescription("Decompile minecraft (%s) using the default decompiler.".formatted(name));
			task.setGroup(Constants.TaskGroup.PUZZLE);

			task.dependsOn(project.getTasks().named("gen%sSourcesWith%s".formatted(name, DecompileConfiguration.DEFAULT_DECOMPILER)));
		});
	}
}
