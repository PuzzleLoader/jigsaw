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

package net.fabricmc.loom.configuration.providers.cosmicreach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loom.api.mappings.layered.MappingsNamespace;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.BundleMetadata;

public final class SplitCosmicReachProvider extends CosmicReachProvider {
	private Path cosmicReachClientOnlyJar;
	private Path cosmicReachCommonJar;

	public SplitCosmicReachProvider(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
		super(metadataProvider, configContext);
	}

	@Override
	protected void initFiles() {
		super.initFiles();

		cosmicReachClientOnlyJar = path("minecraft-client-only.jar");
		cosmicReachCommonJar = path("minecraft-common.jar");
	}

	@Override
	public List<Path> getCosmicReachJars() {
		return List.of(cosmicReachClientOnlyJar, cosmicReachCommonJar);
	}

	@Override
	public MappingsNamespace getOfficialNamespace() {
		return MappingsNamespace.OFFICIAL;
	}

	@Override
	public void provide() throws Exception {
		super.provide();

		boolean requiresRefresh = getExtension().refreshDeps() || Files.notExists(cosmicReachClientOnlyJar) || Files.notExists(cosmicReachCommonJar);

		if (!requiresRefresh) {
			return;
		}

		BundleMetadata serverBundleMetadata = getServerBundleMetadata();

		if (serverBundleMetadata == null) {
			throw new UnsupportedOperationException("Only CosmicReach versions using a bundled server jar can be split, please use a merged jar setup for this version of cosmicReach");
		}

		extractBundledServerJar();

		final Path clientJar = getCosmicReachClientJar().toPath();
		final Path serverJar = getCosmicReachExtractedServerJar().toPath();

		try (CosmicReachJarSplitter jarSplitter = new CosmicReachJarSplitter(clientJar, serverJar)) {
			// Required for loader to compute the version info also useful to have in both jars.
			jarSplitter.sharedEntry("post_build/Cosmic-Reach-Localization/CREDITS.txt");
			jarSplitter.sharedEntry("build_assets/Licences/COSMIC_REACH_LICENSE.txt");
			jarSplitter.sharedEntry("build_assets/Licences/COSMIC_REACH_LOCALIZATION.txt");
			jarSplitter.sharedEntry("build_assets/Licences/COSMIC_REACH_SAVE_LOCATION.txt");
			jarSplitter.sharedEntry("build_assets/version.txt");

			jarSplitter.split(cosmicReachClientOnlyJar, cosmicReachCommonJar);
		} catch (Exception e) {
			Files.deleteIfExists(cosmicReachClientOnlyJar);
			Files.deleteIfExists(cosmicReachCommonJar);

			throw new RuntimeException("Failed to split cosmicReach", e);
		}
	}

	public Path getCosmicReachClientOnlyJar() {
		return cosmicReachClientOnlyJar;
	}

	public Path getCosmicReachCommonJar() {
		return cosmicReachCommonJar;
	}
}
