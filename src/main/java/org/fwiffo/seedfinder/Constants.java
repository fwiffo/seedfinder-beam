package org.fwiffo.seedfinder;

public class Constants {
	public final static long BATCH_SIZE = 8192L;  // * 8 byte long = 64kb.
	public final static int LEGAL_SPAWN_RADIUS = 256;
	public final static int SPAWN_CHUNK_RADIUS = 128;
	public final static long MAX_SEED = 1L << 48;
}
