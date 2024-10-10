/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
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

package net.fabricmc.loom.configuration.providers.cosmicreach;

import java.util.List;

import org.gradle.api.Project;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.decompile.DecompileConfiguration;
import net.fabricmc.loom.configuration.decompile.SingleJarDecompileConfiguration;
import net.fabricmc.loom.configuration.decompile.SplitDecompileConfiguration;
import net.fabricmc.loom.configuration.processors.CosmicReachJarProcessorManager;
import net.fabricmc.loom.configuration.providers.cosmicreach.mapped.IntermediaryCosmicReachProvider;
import net.fabricmc.loom.configuration.providers.cosmicreach.mapped.MappedCosmicReachProvider;
import net.fabricmc.loom.configuration.providers.cosmicreach.mapped.NamedCosmicReachProvider;
import net.fabricmc.loom.configuration.providers.cosmicreach.mapped.ProcessedNamedCosmicReachProvider;

public record CosmicReachJarConfiguration<
		M extends CosmicReachProvider,
		N extends NamedCosmicReachProvider<M>,
		Q extends MappedCosmicReachProvider>(
				MinecraftProviderFactory<M> cosmicReachProviderFactory,
				IntermediaryCosmicReachProviderFactory<M> intermediaryCosmicReachProviderFactory,
				NamedCosmicReachProviderFactory<M> namedCosmicReachProviderFactory,
				ProcessedNamedMinecraftProviderFactory<M, N> processedNamedCosmicReachProviderFactory,
				DecompileConfigurationFactory<Q> decompileConfigurationFactory,
				List<String> supportedEnvironments) {
	public static final CosmicReachJarConfiguration<
				MergedCosmicReachProvider,
				NamedCosmicReachProvider.MergedImpl,
			MappedCosmicReachProvider> MERGED = new CosmicReachJarConfiguration<>(
				MergedCosmicReachProvider::new,
				IntermediaryCosmicReachProvider.MergedImpl::new,
				NamedCosmicReachProvider.MergedImpl::new,
				ProcessedNamedCosmicReachProvider.MergedImpl::new,
				SingleJarDecompileConfiguration::new,
				List.of("client", "server")
			);
	public static final CosmicReachJarConfiguration<
			SingleJarCosmicReachProvider,
				NamedCosmicReachProvider.SingleJarImpl,
			MappedCosmicReachProvider> SERVER_ONLY = new CosmicReachJarConfiguration<>(
				SingleJarCosmicReachProvider::server,
				IntermediaryCosmicReachProvider.SingleJarImpl::server,
				NamedCosmicReachProvider.SingleJarImpl::server,
				ProcessedNamedCosmicReachProvider.SingleJarImpl::server,
				SingleJarDecompileConfiguration::new,
				List.of("server")
			);
	public static final CosmicReachJarConfiguration<
			SingleJarCosmicReachProvider,
				NamedCosmicReachProvider.SingleJarImpl,
			MappedCosmicReachProvider> CLIENT_ONLY = new CosmicReachJarConfiguration<>(
				SingleJarCosmicReachProvider::client,
				IntermediaryCosmicReachProvider.SingleJarImpl::client,
				NamedCosmicReachProvider.SingleJarImpl::client,
				ProcessedNamedCosmicReachProvider.SingleJarImpl::client,
				SingleJarDecompileConfiguration::new,
				List.of("client")
			);
	public static final CosmicReachJarConfiguration<
			SplitCosmicReachProvider,
				NamedCosmicReachProvider.SplitImpl,
				MappedCosmicReachProvider.Split> SPLIT = new CosmicReachJarConfiguration<>(
				SplitCosmicReachProvider::new,
				IntermediaryCosmicReachProvider.SplitImpl::new,
				NamedCosmicReachProvider.SplitImpl::new,
				ProcessedNamedCosmicReachProvider.SplitImpl::new,
				SplitDecompileConfiguration::new,
				List.of("client", "server")
			);

	public CosmicReachProvider createMinecraftProvider(CosmicReachMetadataProvider metadataProvider, ConfigContext context) {
		return cosmicReachProviderFactory.create(metadataProvider, context);
	}

	public IntermediaryCosmicReachProvider<M> createIntermediaryMinecraftProvider(Project project) {
		return intermediaryCosmicReachProviderFactory.create(project, getCosmicReachProvider(project));
	}

	public NamedCosmicReachProvider<M> createNamedMinecraftProvider(Project project) {
		return namedCosmicReachProviderFactory.create(project, getCosmicReachProvider(project));
	}

	public ProcessedNamedCosmicReachProvider<M, N> createProcessedNamedMinecraftProvider(NamedCosmicReachProvider<?> namedCosmicReachProvider, CosmicReachJarProcessorManager jarProcessorManager) {
		return processedNamedCosmicReachProviderFactory.create((N) namedCosmicReachProvider, jarProcessorManager);
	}

	public DecompileConfiguration<Q> createDecompileConfiguration(Project project) {
		return decompileConfigurationFactory.create(project, getMappedCosmicReachProvider(project));
	}

	private M getCosmicReachProvider(Project project) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		//noinspection unchecked
		return (M) extension.getCosmicReachProvider();
	}

	private Q getMappedCosmicReachProvider(Project project) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		//noinspection unchecked
		return (Q) extension.getNamedCosmicReachProvider();
	}

	// Factory interfaces:
	private interface MinecraftProviderFactory<M extends CosmicReachProvider> {
		M create(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext);
	}

	private interface IntermediaryCosmicReachProviderFactory<M extends CosmicReachProvider> {
		IntermediaryCosmicReachProvider<M> create(Project project, M cosmicReachProvider);
	}

	private interface NamedCosmicReachProviderFactory<M extends CosmicReachProvider> {
		NamedCosmicReachProvider<M> create(Project project, M cosmicReachProvider);
	}

	private interface ProcessedNamedMinecraftProviderFactory<M extends CosmicReachProvider, N extends NamedCosmicReachProvider<M>> {
		ProcessedNamedCosmicReachProvider<M, N> create(N namedMinecraftProvider, CosmicReachJarProcessorManager jarProcessorManager);
	}

	private interface DecompileConfigurationFactory<M extends MappedCosmicReachProvider> {
		DecompileConfiguration<M> create(Project project, M minecraftProvider);
	}
}
