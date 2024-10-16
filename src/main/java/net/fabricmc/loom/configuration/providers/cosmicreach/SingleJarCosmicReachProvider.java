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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.BundleMetadata;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public abstract sealed class SingleJarCosmicReachProvider extends CosmicReachProvider permits SingleJarCosmicReachProvider.Server, SingleJarCosmicReachProvider.Client {
	private Path minecraftEnvOnlyJar;

	private SingleJarCosmicReachProvider(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
		super(metadataProvider, configContext);
	}

	public static SingleJarCosmicReachProvider.Server server(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
		return new SingleJarCosmicReachProvider.Server(metadataProvider, configContext);
	}

	public static SingleJarCosmicReachProvider.Client client(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
		return new SingleJarCosmicReachProvider.Client(metadataProvider, configContext);
	}

	@Override
	protected void initFiles() {
		super.initFiles();

		minecraftEnvOnlyJar = path("cosmic-reach-%s-only.jar".formatted(type()));
	}

	@Override
	public List<Path> getCosmicReachJars() {
		return List.of(minecraftEnvOnlyJar);
	}

	@Override
	public void provide() throws Exception {
		super.provide();

		// Server only JARs are supported on any version, client only JARs are pretty much useless after 1.3.
//		if (provideClient() && !isLegacyVersion()) {
//			getProject().getLogger().warn("Using `clientOnlyCosmicReachJar()` is not recommended for CosmicReach versions 1.3 or newer.");
//		}

		boolean requiresRefresh = getExtension().refreshDeps() || Files.notExists(minecraftEnvOnlyJar);

		if (!requiresRefresh) {
			return;
		}

		final Path inputJar = getInputJar(this);

		TinyRemapper remapper = null;

		try {
			remapper = TinyRemapper.newRemapper().build();

			Files.deleteIfExists(minecraftEnvOnlyJar);

			// Pass through tiny remapper to fix the meta-inf
			try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(minecraftEnvOnlyJar).build()) {
				outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper);
				remapper.readInputs(inputJar);
				remapper.apply(outputConsumer);
			}
		} catch (Exception e) {
			Files.deleteIfExists(minecraftEnvOnlyJar);
			throw new RuntimeException("Failed to process %s only jar".formatted(type()), e);
		} finally {
			if (remapper != null) {
				remapper.finish();
			}
		}
	}

	public Path getCosmicReachEnvOnlyJar() {
		return minecraftEnvOnlyJar;
	}

	abstract SingleJarEnvType type();

	abstract Path getInputJar(SingleJarCosmicReachProvider provider) throws Exception;

	public static final class Server extends SingleJarCosmicReachProvider {
		private Server(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
			super(metadataProvider, configContext);
		}

		@Override
		public SingleJarEnvType type() {
			return SingleJarEnvType.SERVER;
		}

		@Override
		public Path getInputJar(SingleJarCosmicReachProvider provider) throws Exception {
			BundleMetadata serverBundleMetadata = provider.getServerBundleMetadata();

			if (serverBundleMetadata == null) {
				return provider.getCosmicReachServerJar().toPath();
			}

			return provider.getCosmicReachServerJar().toPath();
		}

		@Override
		protected boolean provideServer() {
			return true;
		}

		@Override
		protected boolean provideClient() {
			return false;
		}
	}

	public static final class Client extends SingleJarCosmicReachProvider {
		private Client(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
			super(metadataProvider, configContext);
		}

		@Override
		public SingleJarEnvType type() {
			return SingleJarEnvType.CLIENT;
		}

		@Override
		public Path getInputJar(SingleJarCosmicReachProvider provider) throws Exception {
			return provider.getCosmicReachClientJar().toPath();
		}

		@Override
		protected boolean provideServer() {
			return false;
		}

		@Override
		protected boolean provideClient() {
			return true;
		}
	}
}
