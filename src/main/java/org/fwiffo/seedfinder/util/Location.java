package org.fwiffo.seedfinder.util;

import java.io.*;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

@DefaultCoder(SerializableCoder.class)
public class Location implements java.io.Serializable {
	public final int x;
	public final int z;

	public Location(int x, int z) {
		this.x = x;
		this.z = z;
	}
}
