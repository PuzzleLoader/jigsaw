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

import net.fabricmc.loom.configuration.processors.CosmicReachJarProcessorManager;

import org.gradle.api.Project;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ConfigContext;

public record CosmicReachJarConfiguration<
		M extends CosmicReachProvider,
		N extends FinalizedCosmicReachProvider<M>
		>(
			MinecraftProviderFactory<M> cosmicReachProviderFactory,
			FinalizedCRProviderFactory<M> finalizedCRProviderFactory,
			ProcessedNamedCRProviderFactory<M, N> processedNamedCosmicReachProviderFactory,
			List<String> supportedEnvironments
		) {

	public static final CosmicReachJarConfiguration<
			MergedCosmicReachProvider,
			FinalizedCosmicReachProvider.MergedImpl
			> MERGED = new CosmicReachJarConfiguration<>(
				MergedCosmicReachProvider::new,
				FinalizedCosmicReachProvider.MergedImpl::new,
				ProcessedNamedCosmicReachProvider.MergedImpl::new,
				List.of("client", "server")
			);
	public static final CosmicReachJarConfiguration<
			SingleJarCosmicReachProvider,
			FinalizedCosmicReachProvider.SingleJarImpl
			> SERVER_ONLY = new CosmicReachJarConfiguration<>(
				SingleJarCosmicReachProvider::server,
				FinalizedCosmicReachProvider.SingleJarImpl::server,
				ProcessedNamedCosmicReachProvider.SingleJarImpl::server,
				List.of("server")
			);
	public static final CosmicReachJarConfiguration<
			SingleJarCosmicReachProvider,
			FinalizedCosmicReachProvider.SingleJarImpl
			> CLIENT_ONLY = new CosmicReachJarConfiguration<>(
				SingleJarCosmicReachProvider::client,
				FinalizedCosmicReachProvider.SingleJarImpl::client,
				ProcessedNamedCosmicReachProvider.SingleJarImpl::client,
				List.of("client")
			);
	public static final CosmicReachJarConfiguration<
			SplitCosmicReachProvider,
			FinalizedCosmicReachProvider.SplitImpl
			> SPLIT = new CosmicReachJarConfiguration<>(
				SplitCosmicReachProvider::new,
				FinalizedCosmicReachProvider.SplitImpl::new,
				ProcessedNamedCosmicReachProvider.SplitImpl::new,
				List.of("client", "server")
			);

	public CosmicReachProvider createMinecraftProvider(CosmicReachMetadataProvider metadataProvider, ConfigContext context) {
		return cosmicReachProviderFactory.create(metadataProvider, context);
	}

	private M getCosmicReachProvider(Project project) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		//noinspection unchecked
		return (M) extension.getCosmicReachProvider();
	}

	public FinalizedCosmicReachProvider<M> createFinalizedCosmicReachProvider(Project project) {
		return finalizedCRProviderFactory.create(project, getCosmicReachProvider(project));
	}

	public ProcessedNamedCosmicReachProvider<M, N> createProcessedNamedCosmicReachProvider(FinalizedCosmicReachProvider<?> namedMinecraftProvider, CosmicReachJarProcessorManager jarProcessorManager) {
		return processedNamedCosmicReachProviderFactory.create((N) namedMinecraftProvider, jarProcessorManager);
	}

	// Factory interfaces:
	private interface MinecraftProviderFactory<M extends CosmicReachProvider> {
		M create(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext);
	}

	private interface FinalizedCRProviderFactory<M extends CosmicReachProvider> {
		FinalizedCosmicReachProvider<M> create(Project project, M crProvider);
	}

	private interface ProcessedNamedCRProviderFactory<M extends CosmicReachProvider, N extends FinalizedCosmicReachProvider<M>> {
		ProcessedNamedCosmicReachProvider<M, N> create(N parentMinecraftProvide, CosmicReachJarProcessorManager jarProcessorManager);
	}

}
