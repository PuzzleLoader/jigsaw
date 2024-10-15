/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 FabricMC
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

package net.fabricmc.loom.configuration.accesswidener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.github.puzzle.access_manipulators.AccessManipulators;

import com.github.puzzle.access_manipulators.readers.AccessTransformerReader;
import com.github.puzzle.access_manipulators.readers.api.IAccessModifierReader;
import com.github.puzzle.access_manipulators.util.ClassPathUtil;

import net.fabricmc.loom.util.fmj.PuzzleModJson;
import net.fabricmc.loom.util.fmj.ModEnvironment;

import static com.github.puzzle.access_manipulators.AccessManipulators.readerMap;

/**
 * {@link AccessWidenerEntry} implementation for a {@link PuzzleModJson}.
 */
public record ModAccessWidenerEntry(PuzzleModJson mod, String path, ModEnvironment environment, boolean transitiveOnly) implements AccessWidenerEntry {
	public static List<ModAccessWidenerEntry> readAll(PuzzleModJson modJson, boolean transitiveOnly) {
		var entries = new ArrayList<ModAccessWidenerEntry>();

		for (Map.Entry<String, ModEnvironment> entry : modJson.getClassTweakers().entrySet()) {
			entries.add(new ModAccessWidenerEntry(modJson, entry.getKey(), entry.getValue(), transitiveOnly));
		}

		return Collections.unmodifiableList(entries);
	}

	@Override
	public String getSortKey() {
		return mod.getId() + ":" + path;
	}

	public static void registerModifierFile(String path, String content) {
		String fileExt = path.split("\\.")[path.split("\\.").length - 1].toLowerCase();
		IAccessModifierReader reader = readerMap.get(fileExt);
		if (reader == null) {
			throw new RuntimeException("Unsupported Access Modifier Extension \"." + fileExt + "\"");
		} else {
			reader.read(content);
		}
	}

	@Override
	public void read() {
		try {
			registerModifierFile(path, new String(readRaw()));
		} catch (Exception ignore) {}
	}

	private byte[] readRaw() throws IOException {
		return mod.getSource().read(path);
	}
}
