package org.fwiffo.seedfinder.structure;

import org.fwiffo.seedfinder.biome.Biome;

public class Igloo extends Structure {
	private static final Biome[] SNOWY_BIOMES = new Biome[] {
		Biome.icePlains,
		Biome.coldTaiga,
	};

	public Igloo() {
		super(14357618L, 24, 32, SNOWY_BIOMES);
	}
}
