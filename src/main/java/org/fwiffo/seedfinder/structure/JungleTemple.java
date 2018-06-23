package org.fwiffo.seedfinder.structure;

import org.fwiffo.seedfinder.biome.Biome;

public class JungleTemple extends Structure {
	private static final Biome[] JUNGLE_BIOMES = new Biome[] {
		Biome.jungle,
		Biome.jungleHills,
	};

	public JungleTemple() {
		super(14357619L, 24, 32, JUNGLE_BIOMES);
	}
}
