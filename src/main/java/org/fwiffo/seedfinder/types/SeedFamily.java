package org.fwiffo.seedfinder.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

import org.fwiffo.seedfinder.types.SeedMetadata;

// TODO: Get @Nullable to work
@DefaultCoder(AvroCoder.class)
public class SeedFamily implements Serializable {
	public final long baseSeed;
	public final Hashtable<Long, Location> fullSeeds;
	public final Location[] huts;
	public final Location[] mansions;
	public final Location[] monuments;

	public SeedFamily() {
		this.baseSeed = 0;
		this.fullSeeds = new Hashtable<Long, Location>();
		this.huts = new Location[0];
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedFamily(
			long baseSeed, Hashtable<Long, Location> fullSeeds,
			Location[] huts, Location[] mansions, Location[] monuments) {
		this.baseSeed = baseSeed;
		this.fullSeeds = fullSeeds;
		this.huts = huts;
		this.mansions = mansions;
		this.monuments = monuments;
	}

	public SeedFamily(long baseSeed, Location[] huts) {
		this.baseSeed = baseSeed;
		this.fullSeeds = new Hashtable<Long, Location>();
		this.huts = huts;
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedFamily(long baseSeed, Hashtable<Long, Location> fullSeeds, Location[] huts) {
		this.baseSeed = baseSeed;
		this.fullSeeds = fullSeeds;
		this.huts = huts;
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedFamily withMansions(Location[] mansions) {
		return new SeedFamily(baseSeed, fullSeeds, huts, mansions, monuments);
	}

	public SeedFamily withMonuments(Location[] monuments) {
		return new SeedFamily(baseSeed, fullSeeds, huts, mansions, monuments);
	}

	public SeedMetadata expanded(long fullSeed, Location spawn) {
		return new SeedMetadata(fullSeed, spawn, huts, mansions, monuments);
	}

	public String toString() {
		ArrayList<String> parts = new ArrayList<String>(8);

		parts.add(String.format("%20d", baseSeed));
		if (huts.length > 0) {
			parts.add("huts:");
			for (Location hut : huts) {
				parts.add(hut.toString());
			}
		}

		return String.join(" ", parts);
	}

	public boolean equals(Object other) {
		if (other == this) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		SeedFamily otherFamily = (SeedFamily)other;
		return (baseSeed == otherFamily.baseSeed &&
				fullSeeds.equals(otherFamily.fullSeeds) &&
				Arrays.equals(huts, otherFamily.huts) &&
				Arrays.equals(mansions, otherFamily.mansions) &&
				Arrays.equals(monuments, otherFamily.monuments));
	}
}

