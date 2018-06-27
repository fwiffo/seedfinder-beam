package org.fwiffo.seedfinder.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

// TODO: Get @Nullable to work
@DefaultCoder(AvroCoder.class)
public class SeedMetadata implements Serializable {
	// These will get populated with empty/dummy values if not present
	// to make them AvroCoderable.
	public final long seed;
	public final Location spawn;
	public final Location[] huts;
	public final Location[] mansions;
	public final Location[] monuments;

	public SeedMetadata() {
		this.seed = 0;
		this.spawn = new Location();
		this.huts = new Location[0];
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedMetadata(long seed, Location[] huts) {
		this.seed = seed;
		this.spawn = new Location(0, 0);
		this.huts = huts;
		this.mansions = new Location[0];
		this.monuments = new Location[0];
	}

	public SeedMetadata(
			long seed, Location spawn,
			Location[] huts, Location[] mansions, Location[] monuments) {
		this.seed = seed;
		this.spawn = spawn;
		this.huts = huts;
		this.mansions = mansions;
		this.monuments = monuments;
	}

	public String toString() {
		ArrayList<String> parts = new ArrayList<String>(8);

		parts.add(String.format("%20d", seed));
		parts.add(String.format("(spawn %4d, %4d)", spawn.x, spawn.z));

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
		SeedMetadata otherSeed = (SeedMetadata)other;
		return (seed == otherSeed.seed &&
				spawn.equals(otherSeed.spawn) &&
				Arrays.equals(huts, otherSeed.huts) &&
				Arrays.equals(mansions, otherSeed.mansions) &&
				Arrays.equals(monuments, otherSeed.monuments));
	}
}
