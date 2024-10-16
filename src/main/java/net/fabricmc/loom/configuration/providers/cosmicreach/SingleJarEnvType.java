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

import java.nio.file.Path;
import java.util.function.Function;

public enum SingleJarEnvType {
	CLIENT(CosmicReachJar.Client::new, CosmicReachJar.Type.CLIENT),
	SERVER(CosmicReachJar.Server::new, CosmicReachJar.Type.SERVER);

	private final Function<Path, CosmicReachJar> jarFunction;
	private final CosmicReachJar.Type type;

	SingleJarEnvType(Function<Path, CosmicReachJar> jarFunction, CosmicReachJar.Type type) {
		this.jarFunction = jarFunction;
		this.type = type;
	}

	public Function<Path, CosmicReachJar> getJar() {
		return jarFunction;
	}

	public CosmicReachJar.Type getType() {
		return type;
	}
}
