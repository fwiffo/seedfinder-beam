package org.fwiffo.seedfinder.biome;

import java.util.*;
import minecraft.layer.*;

public class BiomeGenerator {
	public GenLayer biomeIndexLayer;
	public GenLayer biomeIndexLayerquarter;
	public BiomeGenerator(long seed, int quarter)
	{
		if(quarter == 0)
			biomeIndexLayer = GenLayer.func_180781_a(seed, "")[1]; //1:1 resolution
		else if(quarter == 1)
			biomeIndexLayerquarter = GenLayer.func_180781_a(seed, "")[0]; // 1:4 fourth resolution less calculations
		else
		{
			biomeIndexLayer = GenLayer.func_180781_a(seed, null)[1];
			biomeIndexLayerquarter = GenLayer.func_180781_a(seed, "")[0]; // 1:4 fourth resolution less calculations
		}

	}

	public int getBiomeAt(int x, int z)
	{
		IntCache.resetIntCache();
		return biomeIndexLayer.getInts(x, z, 1, 1)[0];
	}

	public int getBiomeAtCenterOfChunk(int x, int z) {
		return getBiomeAt(x*16 + 8, z*16 + 8);
	}

	public int[] getBiomeData(int x, int y, int width, int height, boolean quarter)
	{
		IntCache.resetIntCache();
		if(quarter)
			return biomeIndexLayerquarter.getInts(x, y, width, height);
		else
			return biomeIndexLayer.getInts(x, y, width, height);
	}

	public Hashtable<Integer, Float> biomeCensus(int x, int z, int width, int height, boolean quarter) {
		int left = x - width/2;
		int top = z - height/2;
		if (quarter) {
			left /= 4;
			top /= 4;
			width /= 4;
			height /= 4;
		}
		int[] biomes = getBiomeData(left, top, width, height, quarter);
		Hashtable<Integer, Float> census = new Hashtable<Integer, Float>();
		int area = width*height;
		for (int i=0; i<256; i++) {
			census.put(i, 0f);
		}
		for (int i=0; i<area; i++) {
			census.put(biomes[i], census.get(biomes[i]) + 1);
		}
		for (Integer i : census.keySet()) {
			census.put(i, census.get(i) / area);
		}
		return census;
	}
}
