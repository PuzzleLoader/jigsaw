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

package net.fabricmc.loom.configuration.providers.cosmicreach.mapped;

import java.util.List;

import org.gradle.api.Project;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.providers.cosmicreach.MergedCosmicReachProvider;
import net.fabricmc.loom.configuration.providers.cosmicreach.CosmicReachProvider;
import net.fabricmc.loom.configuration.providers.cosmicreach.SingleJarEnvType;
import net.fabricmc.loom.configuration.providers.cosmicreach.SingleJarCosmicReachProvider;
import net.fabricmc.loom.configuration.providers.cosmicreach.SplitCosmicReachProvider;
import net.fabricmc.tinyremapper.TinyRemapper;

public abstract sealed class IntermediaryCosmicReachProvider<M extends CosmicReachProvider> extends AbstractMappedCosmicReachProvider<M> permits IntermediaryCosmicReachProvider.MergedImpl, IntermediaryCosmicReachProvider.SingleJarImpl, IntermediaryCosmicReachProvider.SplitImpl {
	public IntermediaryCosmicReachProvider(Project project, M minecraftProvider) {
		super(project, minecraftProvider);
	}

	@Override
	public final MappingsNamespace getTargetNamespace() {
		return MappingsNamespace.INTERMEDIARY;
	}

	@Override
	public MavenScope getMavenScope() {
		return MavenScope.GLOBAL;
	}

	public static final class MergedImpl extends IntermediaryCosmicReachProvider<MergedCosmicReachProvider> implements Merged {
		public MergedImpl(Project project, MergedCosmicReachProvider minecraftProvider) {
			super(project, minecraftProvider);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(coosmicReachProvider.getMergedJar(), getMergedJar(), coosmicReachProvider.getOfficialNamespace())
			);
		}
	}

	public static final class SplitImpl extends IntermediaryCosmicReachProvider<SplitCosmicReachProvider> implements Split {
		public SplitImpl(Project project, SplitCosmicReachProvider minecraftProvider) {
			super(project, minecraftProvider);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(coosmicReachProvider.getCosmicReachCommonJar(), getCommonJar(), coosmicReachProvider.getOfficialNamespace()),
				new RemappedJars(coosmicReachProvider.getCosmicReachClientOnlyJar(), getClientOnlyJar(), coosmicReachProvider.getOfficialNamespace(), coosmicReachProvider.getCosmicReachCommonJar())
			);
		}

		@Override
		protected void configureRemapper(RemappedJars remappedJars, TinyRemapper.Builder tinyRemapperBuilder) {
			configureSplitRemapper(remappedJars, tinyRemapperBuilder);
		}
	}

	public static final class SingleJarImpl extends IntermediaryCosmicReachProvider<SingleJarCosmicReachProvider> implements SingleJar {
		private final SingleJarEnvType env;

		private SingleJarImpl(Project project, SingleJarCosmicReachProvider minecraftProvider, SingleJarEnvType env) {
			super(project, minecraftProvider);
			this.env = env;
		}

		public static SingleJarImpl server(Project project, SingleJarCosmicReachProvider minecraftProvider) {
			return new SingleJarImpl(project, minecraftProvider, SingleJarEnvType.SERVER);
		}

		public static SingleJarImpl client(Project project, SingleJarCosmicReachProvider minecraftProvider) {
			return new SingleJarImpl(project, minecraftProvider, SingleJarEnvType.CLIENT);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
				new RemappedJars(coosmicReachProvider.getCosmicReachEnvOnlyJar(), getEnvOnlyJar(), coosmicReachProvider.getOfficialNamespace())
			);
		}

		@Override
		public SingleJarEnvType env() {
			return env;
		}
	}
}
