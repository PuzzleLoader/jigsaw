/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 FabricMC
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

package net.fabricmc.loom.extension;

import java.util.Objects;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.util.PatternSet;

import net.fabricmc.loom.api.MixinExtensionAPI;

public abstract class MixinExtensionApiImpl implements MixinExtensionAPI {
	protected final Project project;
	protected final Property<Boolean> useMixinAp;
	private final MapProperty<String, String> messages;
	private final Property<Boolean> showMessageTypes;

	public MixinExtensionApiImpl(Project project) {
		this.project = Objects.requireNonNull(project);
		this.useMixinAp = project.getObjects().property(Boolean.class)
				.convention(true);

		this.messages = project.getObjects().mapProperty(String.class, String.class);
		this.messages.finalizeValueOnRead();

		this.showMessageTypes = project.getObjects().property(Boolean.class);
		this.showMessageTypes.convention(false).finalizeValueOnRead();
	}

	protected abstract PatternSet add0(SourceSet sourceSet);

	@Override
	public Property<Boolean> getUseLegacyMixinAp() {
		return useMixinAp;
	}

	public void add(SourceSet sourceSet, Action<PatternSet> action) {
		PatternSet pattern = add0(sourceSet);
		action.execute(pattern);
	}

	@Override
	public void add(SourceSet sourceSet) {
		add(sourceSet, x -> { });
	}

	@Override
	public void add(String sourceSetName, Action<PatternSet> action) {
		add(sourceSetName, action);
	}

	@Override
	public void add(String sourceSetName) {
		add(sourceSetName, x -> { });
	}

	@Override
	public MapProperty<String, String> getMessages() {
		return messages;
	}

	@Override
	public Property<Boolean> getShowMessageTypes() {
		return showMessageTypes;
	}

	@Override
	public void messages(Action<MapProperty<String, String>> action) {
		action.execute(messages);
	}

	private SourceSet resolveSourceSet(String sourceSetName) {
		// try to find sourceSet with name sourceSetName in this project
		SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().findByName(sourceSetName);

		if (sourceSet == null) {
			throw new InvalidUserDataException("No sourceSet " + sourceSetName + " was found");
		}

		return sourceSet;
	}

}
