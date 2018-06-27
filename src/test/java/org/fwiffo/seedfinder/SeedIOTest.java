package org.fwiffo.seedfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.junit.experimental.categories.Category;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.NeedsRunner;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.KV;

import org.fwiffo.seedfinder.types.Location;
import org.fwiffo.seedfinder.types.SeedFamily;
import org.fwiffo.seedfinder.types.SeedMetadata;
import org.fwiffo.seedfinder.SeedIO;

@RunWith(JUnit4.class)
public class SeedIOTest {
	@Rule
	public final transient TestPipeline p = TestPipeline.create();

	@Test
	@Category(NeedsRunner.class)
	public void testAggregateDeaggregateSeeds() {
		Location[] empty = new Location[0];
		Location[] huts1 = new Location[]{new Location(11, 111)};
		Location[] huts2 = new Location[]{new Location(22, 222)};
		Location spawn1 = new Location(1, 11);
		Location spawn2 = new Location(11, 1);
		Location spawn3 = new Location(2, 22);

		ArrayList<KV<Long, SeedMetadata>> inputs = new ArrayList<KV<Long, SeedMetadata>>();
		inputs.add(KV.of(1L, new SeedMetadata(10L, spawn1, huts1, empty, empty)));
		inputs.add(KV.of(1L, new SeedMetadata(11L, spawn2, huts1, empty, empty)));
		inputs.add(KV.of(2L, new SeedMetadata(20L, spawn3, huts2, empty, empty)));

		Hashtable<Long, Location> fullSeeds1 = new Hashtable<Long, Location>();
		Hashtable<Long, Location> fullSeeds2 = new Hashtable<Long, Location>();
		fullSeeds1.put(10L, spawn1);
		fullSeeds1.put(11L, spawn2);
		fullSeeds2.put(20L, spawn3);
		SeedFamily[] expectedOutputs = new SeedFamily[]{
			new SeedFamily(1L, fullSeeds1, huts1), new SeedFamily(2L, fullSeeds2, huts2)};

		PCollection<SeedFamily> actualOutputs = p
			.apply("CreateAgg", Create.of(inputs))
			.apply(GroupByKey.<Long, SeedMetadata>create())
			.apply("Agg", ParDo.of(new SeedIO.AggregateSeeds()));

		PAssert.that(actualOutputs).containsInAnyOrder(expectedOutputs);

		PCollection<KV<Long, SeedMetadata>> actualReverse = p
			.apply("CreateDeagg", Create.of(Arrays.asList(expectedOutputs)))
			.apply("AddKeys", ParDo.of(new SeedIO.AddKeys()))
			.apply("Deagg", ParDo.of(new SeedIO.DeaggregateSeeds()));

		PAssert.that(actualReverse).containsInAnyOrder(inputs);

		p.run();
	}
}
