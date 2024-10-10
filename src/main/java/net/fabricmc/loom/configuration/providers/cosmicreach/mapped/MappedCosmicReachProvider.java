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

import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loom.configuration.providers.cosmicreach.CosmicReachJar;
import net.fabricmc.loom.configuration.providers.cosmicreach.SingleJarEnvType;

public interface MappedCosmicReachProvider {
	default List<Path> getMinecraftJarPaths() {
		return getMinecraftJars().stream().map(CosmicReachJar::getPath).toList();
	}

	List<CosmicReachJar> getMinecraftJars();

	interface ProviderImpl extends MappedCosmicReachProvider {
		Path getJar(CosmicReachJar.Type type);
	}

	interface Merged extends ProviderImpl {
		default CosmicReachJar getMergedJar() {
			return new CosmicReachJar.Merged(getJar(CosmicReachJar.Type.MERGED));
		}

		@Override
		default List<CosmicReachJar> getMinecraftJars() {
			return List.of(getMergedJar());
		}
	}

	interface Split extends ProviderImpl {
		default CosmicReachJar getCommonJar() {
			return new CosmicReachJar.Common(getJar(CosmicReachJar.Type.COMMON));
		}

		default CosmicReachJar getClientOnlyJar() {
			return new CosmicReachJar.ClientOnly(getJar(CosmicReachJar.Type.CLIENT_ONLY));
		}

		@Override
		default List<CosmicReachJar> getMinecraftJars() {
			return List.of(getCommonJar(), getClientOnlyJar());
		}
	}

	interface SingleJar extends ProviderImpl {
		SingleJarEnvType env();

		default CosmicReachJar.Type envType() {
			return env().getType();
		}

		default CosmicReachJar getEnvOnlyJar() {
			return env().getJar().apply(getJar(env().getType()));
		}

		@Override
		default List<CosmicReachJar> getMinecraftJars() {
			return List.of(getEnvOnlyJar());
		}
	}
}
