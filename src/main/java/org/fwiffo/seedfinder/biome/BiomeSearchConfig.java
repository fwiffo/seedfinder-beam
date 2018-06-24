package org.fwiffo.seedfinder.biome;

import java.io.Serializable;
import org.fwiffo.seedfinder.biome.Biome;

public class BiomeSearchConfig implements Serializable  {
	public static final BiomeSearchConfig OCEAN = new BiomeSearchConfig(
			0.8f,
			new Biome[]{
				Biome.ocean, Biome.frozenOcean, Biome.deepOcean,
				Biome.oceanM, Biome.frozenOceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig MUSHROOM_ISLAND = new BiomeSearchConfig(
			0.5f,
			new Biome[]{
				Biome.mushroomIsland, Biome.mushroomIslandShore,
				Biome.mushroomIslandM, Biome.mushroomIslandShoreM},
			new Biome[]{Biome.ocean, Biome.deepOcean, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig JUNGLE = new BiomeSearchConfig(
			0.8f,
			new Biome[]{
				Biome.jungle, Biome.jungleHills, Biome.jungleEdge},
			new Biome[]{Biome.ocean, Biome.deepOcean, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig MEGA_TAIGA = new BiomeSearchConfig(
			0.8f,
			new Biome[]{
				Biome.megaTaiga, Biome.megaTaigaHills,
				Biome.megaSpruceTaiga, Biome.megaSpurceTaigaHills},
			new Biome[]{Biome.ocean, Biome.deepOcean, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig MESA = new BiomeSearchConfig(
			0.8f,
			new Biome[]{
				Biome.mesa, Biome.mesaPlateauF, Biome.mesaPlateau,
				Biome.mesaBryce, Biome.mesaPlateauFM, Biome.mesaPlateauM},
			new Biome[]{Biome.ocean, Biome.deepOcean, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig FLOWER_FOREST = new BiomeSearchConfig(
			0.3f,
			new Biome[]{Biome.flowerForest},
			new Biome[]{Biome.ocean, Biome.deepOcean, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig ICE_SPIKES = new BiomeSearchConfig(
			0.5f,
			new Biome[]{Biome.icePlainsSpikes},
			new Biome[]{
				Biome.icePlains, Biome.iceMountains,
				Biome.frozenOcean, Biome.frozenRiver, Biome.frozenOceanM, Biome.frozenRiverM,
				Biome.ocean, Biome.deepOcean, Biome.oceanM, Biome.deepOceanM});

	public final float minFraction;
	public final int[] includeBiomes;
	public final int[] ignoreBiomes;

	public BiomeSearchConfig(float minFraction, Biome[] includeBiomes, Biome[] ignoreBiomes) {
		this.minFraction = minFraction;
		this.includeBiomes = new int[includeBiomes.length];
		this.ignoreBiomes = new int[ignoreBiomes.length];

		// Convert directly to integers so that instances of this class don't
		// have references to Biome objects, and therefore are serializable and
		// don't have a bunch of junk they don't need.
		for (int i=0; i<includeBiomes.length; i++) {
			this.includeBiomes[i] = includeBiomes[i].index;
		}
		for (int i=0; i<ignoreBiomes.length; i++) {
			this.ignoreBiomes[i] = ignoreBiomes[i].index;
		}
	}

	public BiomeSearchConfig(float minFraction, Biome[] includeBiomes) {
		this(minFraction, includeBiomes, new Biome[]{});
	}
}
