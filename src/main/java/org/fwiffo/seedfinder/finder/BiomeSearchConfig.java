package org.fwiffo.seedfinder.finder;

import java.io.Serializable;

import org.fwiffo.seedfinder.biome.Biome;

public class BiomeSearchConfig implements Serializable  {

	public static final BiomeSearchConfig FLOWER_FOREST = new BiomeSearchConfig(
			0.65f,
			new Biome[]{Biome.flowerForest},
			new Biome[]{
				Biome.river, Biome.ocean, Biome.deepOcean,
				Biome.riverM, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig ICE_SPIKES = new BiomeSearchConfig(
			0.75f,
			new Biome[]{Biome.icePlainsSpikes},
			new Biome[]{
				Biome.icePlains, Biome.iceMountains, Biome.frozenRiver, Biome.frozenRiverM,
				Biome.river, Biome.frozenOcean, Biome.ocean, Biome.deepOcean,
				Biome.riverM, Biome.frozenOceanM, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig JUNGLE = new BiomeSearchConfig(
			0.95f,
			new Biome[]{
				Biome.jungle, Biome.jungleHills, Biome.jungleEdge},
			new Biome[]{
				Biome.river, Biome.ocean, Biome.deepOcean,
				Biome.riverM, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig MEGA_TAIGA = new BiomeSearchConfig(
			0.90f,
			new Biome[]{
				Biome.megaTaiga, Biome.megaTaigaHills,
				Biome.megaSpruceTaiga, Biome.megaSpurceTaigaHills},
			new Biome[]{
				Biome.river, Biome.ocean, Biome.deepOcean,
				Biome.riverM, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig MESA = new BiomeSearchConfig(
			0.90f,
			new Biome[]{
				Biome.mesa, Biome.mesaPlateauF, Biome.mesaPlateau,
				Biome.mesaBryce, Biome.mesaPlateauFM, Biome.mesaPlateauM},
			new Biome[]{
				Biome.river, Biome.ocean, Biome.deepOcean,
				Biome.riverM, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig MUSHROOM_ISLAND = new BiomeSearchConfig(
			0.50f,
			new Biome[]{
				Biome.mushroomIsland, Biome.mushroomIslandShore,
				Biome.mushroomIslandM, Biome.mushroomIslandShoreM},
			new Biome[]{
				Biome.river, Biome.ocean, Biome.deepOcean,
				Biome.riverM, Biome.oceanM, Biome.deepOceanM});

	public static final BiomeSearchConfig OCEAN = new BiomeSearchConfig(
			0.80f,
			new Biome[]{
				Biome.ocean, Biome.frozenOcean, Biome.deepOcean,
				Biome.oceanM, Biome.frozenOceanM, Biome.deepOceanM});

	public enum Name {
		none, flower_forest, ice_spikes, jungle, mega_taiga, mesa, mushroom_island, ocean
	}

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

	public static BiomeSearchConfig getConfig(Name name) {
		switch (name) {
			case flower_forest:   return FLOWER_FOREST;
			case ice_spikes:      return ICE_SPIKES;
			case jungle:          return JUNGLE;
			case mega_taiga:      return MEGA_TAIGA;
			case mesa:            return MESA;
			case mushroom_island: return MUSHROOM_ISLAND;
			case ocean:           return OCEAN;
		}
		return null;
	}
}
