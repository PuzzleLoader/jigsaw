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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loom.configuration.ConfigContext;

public final class MergedCosmicReachProvider extends CosmicReachProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(MergedCosmicReachProvider.class);

	private Path cosmicReachMergedJar;

	public MergedCosmicReachProvider(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
		super(metadataProvider, configContext);
	}

	@Override
	protected void initFiles() {
		super.initFiles();
		cosmicReachMergedJar = path("cosmic-reach-merged.jar");
	}

	@Override
	public List<Path> getCosmicReachJars() {
		return List.of(cosmicReachMergedJar);
	}

	@Override
	public void provide() throws Exception {
		super.provide();

		if (!provideServer() || !provideClient()) {
			throw new UnsupportedOperationException("This version does not provide both the client and server jars - please select the client-only or server-only jar configuration!");
		}

		if (!Files.exists(cosmicReachMergedJar) || getExtension().refreshDeps()) {
			try {
				mergeJars();
			} catch (Throwable e) {
				Files.deleteIfExists(getCosmicReachClientJar().toPath());
				Files.deleteIfExists(getCosmicReachServerJar().toPath());
				Files.deleteIfExists(cosmicReachMergedJar);

				getProject().getLogger().error("Could not merge JARs! Deleting source JARs - please re-run the command and move on.", e);
				throw e;
			}
		}
	}

	private void mergeJars() throws IOException {
		File cosmicReachClientJar = getCosmicReachClientJar();
		File cosmicReachServerJar = getCosmicReachServerJar();

		mergeJars(cosmicReachClientJar, cosmicReachServerJar, cosmicReachMergedJar.toFile());
	}

	public static void mergeJars(File clientJar, File serverJar, File mergedJar) throws IOException {
		LOGGER.info(":merging jars");

		Objects.requireNonNull(clientJar, "Cannot merge null client jar?");
		Objects.requireNonNull(serverJar, "Cannot merge null server jar?");

		try (var jarMerger = new CosmicReachJarMerger(clientJar, serverJar, mergedJar)) {
			jarMerger.enableSyntheticParamsOffset();
			jarMerger.merge();
		}

	}

	public Path getMergedJar() {
		return cosmicReachMergedJar;
	}
}
