package org.fwiffo.seedfinder.util;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

@DefaultCoder(AvroCoder.class)
public class SeedMetadata implements Serializable {
	// These will get populated with empty/dummy values if not present
	// to make them AvroCoderable.
	public final long seed;
	public final Location[] huts;
	public final Location spawn;

	public SeedMetadata() {
		this.seed = 0;
		this.spawn = new Location();
		this.huts = new Location[0];
	}

	public SeedMetadata(long seed) {
		this.seed = seed;
		this.spawn = new Location();
		this.huts = new Location[0];
	}

	public SeedMetadata(long seed, Location[] huts) {
		this.seed = seed;
		this.spawn = new Location(0, 0);
		this.huts = huts;
	}

	public SeedMetadata(long seed, Location spawn, Location[] huts) {
		this.seed = seed;
		this.spawn = spawn;
		this.huts = huts;
	}

	public SeedMetadata expanded(long fullSeed, Location spawn) {
		return new SeedMetadata(fullSeed, spawn, huts);
	}

	public String asString() {
		ArrayList<String> parts = new ArrayList(8);

		parts.add(String.format("%20d", seed));
		if (spawn != null) {
			parts.add(String.format("(spawn %4d, %4d)", spawn.x, spawn.z));
		}
		if (huts != null) {
			parts.add("huts:");
			for (Location hut : huts) {
				parts.add(hut.asString());
			}
		}

		return String.join(" ", parts);
	}
}
