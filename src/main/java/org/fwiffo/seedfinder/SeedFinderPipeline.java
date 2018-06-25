package org.fwiffo.seedfinder;

import java.lang.Math;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.beam.sdk.io.AvroIO;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.metrics.MetricResult;
import org.apache.beam.sdk.metrics.MetricResults;
import org.apache.beam.sdk.metrics.MetricQueryResults;
import org.apache.beam.sdk.metrics.MetricsFilter;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.KV;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.SeedIO;
import org.fwiffo.seedfinder.finder.BiomeSearchConfig;
import org.fwiffo.seedfinder.finder.FullSeedFinder;
import org.fwiffo.seedfinder.finder.PotentialSeedFinder;
import org.fwiffo.seedfinder.types.SeedFamily;
import org.fwiffo.seedfinder.types.SeedMetadata;

/**
 * Minecraft seed finder Beam pipeline.
 *
 * <p>To run locally
 * mvn compile exec:java \
 *   -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
 *   -Dexec.args="--output=./output/seeds \
 *   --start_seed=0 --end_seed=1G ..."
 *
 * <p>To run using managed resource in Google Cloud Platform:
 * GOOGLE_APPLICATION_CREDENTIALS='credentials.json' mvn compile exec:java \
 *   -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
 *   -Dexec.args="--project=<YOUR_PROJECT_ID> \
 *   --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> \
 *   --output=<OUTPUT_LOCATION_IN_CLOUD_STORAGE> \
 *   --region=us-west1 \
 *   --runner=DataflowRunner \
 *   --jobName=minecraft-seed-finder \
 *   --start_seed=0 --end_seed=5G ..."
 */
public class SeedFinderPipeline {
	private static final Logger LOG = LoggerFactory.getLogger(PotentialSeedFinder.class);

	// Only search the lower 48 bits of the key space.
	public interface SeedFinderOptions extends PipelineOptions {
		@Description("Path of the file to write to.")
		@Required
		String getOutput();
		void setOutput(String value);

		@Description("Avro format file of precomputed quad witch hut seeds.")
		@Default.String("")
		String getInput();
		void setInput(String value);

		@Description("Lower 48 bits of start seed for search; 0 to 256T. suffixes of " +
				"K, M, G and T may be used.")
		@Default.String("0")
		String getStart_seed();
		void setStart_seed(String value);

		@Description("Lower 48 bits of end seed for search; 0 to 256T. suffixes of " +
				"K, M, G and T may be used.")
		@Default.String("1G")
		String getEnd_seed();
		void setEnd_seed(String value);

		@Description("Maximum time for generating candidate seeds, in minutes.")
		@Default.Integer(0)
		int getMax_sequence_time();
		void setMax_sequence_time(int value);

		@Description("Radius to search for structures and biomes, in blocks; structures will " +
				"round up to an integer number of regions. For example, 2048 blocks will be " +
				"exact for witch huts, and ocean monuments which have 32 chunk regions, but will " +
				"round up to 2560 for woodland mansions, which have 80 chunk regions.")
		@Default.Integer(2048)
		int getSearch_radius();
		void setSearch_radius(int value);

		@Description("Bulk search mode ignores search options other than seed range, search " +
				"radius and timeout. Outputs a binary format of all quad huts seed for further " +
				"searching later.")
		@Default.Boolean(false)
		boolean getBulk_search_mode();
		void setBulk_search_mode(boolean value);

		@Description("Search for seeds with ocean monuments close to quad huts; a max distance " +
				"from the perimeter in chunks. 5 is rare, and pretty close, 4 or less is " +
				"vanishingly rare. Monuments inside the perimeter are theoretically possible, " +
				"but might be too rare to actually exist.")
		@Default.Integer(0)
		int getOcean_monument_near_huts();
		void setOcean_monument_near_huts(int value);

		@Description("Search for seeds with a number nearby woodland mansions.")
		@Default.Integer(0)
		int getWoodland_mansions();
		void setWoodland_mansions(int value);

		// TODO: Maybe add more options.
		@Description("Search for seeds with specific biomes at spawn")
		@Default.Enum("none")
		BiomeSearchConfig.Name getSpawn_biomes();
		void setSpawn_biomes(BiomeSearchConfig.Name value);

		@Description("Search for seeds with all biome types within the search radius.")
		@Default.Boolean(false)
		boolean getAll_biomes_nearby();
		void setAll_biomes_nearby(boolean value);

		// TODO: Lots of a specific biome nearby; e.g. lots of mushroom islands.
		// TODO: Stronghold close to quad huts. Stronghold location search
		// requires full seed.
	}

	private static long parseHuman(String value) {
		if (value.length() < 1) {
			return 0L;
		}
		int base = 0;
		switch (value.substring(value.length()-1)) {
			case "T": base = 40; break;
			case "G": base = 30; break;
			case "B": base = 30; break;
			case "M": base = 20; break;
			case "K": base = 10; break;
			default: return Long.parseLong(value);
		}
		return Long.parseLong(value.substring(0, value.length()-1)) << base;
	}

	private static void logMetrics(MetricResults metrics) {
		MetricQueryResults metricResults = metrics.queryMetrics(MetricsFilter.builder().build());

		TreeMap<String, Long> counters = new TreeMap<String, Long>();
		for (MetricResult<Long> counter : metricResults.counters()) {
			counters.put(counter.name().name(), counter.committed());
		}

		ArrayList<String> formatted = new ArrayList<String>();
		formatted.add("\nCounters:");
		for (String key : counters.keySet()) {
			formatted.add(String.format("%40s: %d", key, counters.get(key)));
		}
		LOG.info(String.join("\n", formatted));
	}

	public static void bulkSearch(
			PCollection<KV<Long, SeedFamily>> potentialSeeds, SeedFinderOptions options) {
		// Expands to full 64-bit seeds, checks the biomes are present for
		// structures to spawn, gets the world spawn coordinates, then groups
		// them back into seed families.
		PCollection<SeedFamily> groupedSeeds = potentialSeeds
			.apply("VerifyQuadHuts",
					ParDo.of(new FullSeedFinder.VerifyQuadHuts()))
			.apply("GroupQuadHuts", GroupByKey.<Long, SeedMetadata>create())
			.apply("AggregateSeedFamilies", ParDo.of(new SeedIO.AggregateSeeds()));

		// Writes the resulting seeds to a binary format, which can be searched
		// in more detail later.
		groupedSeeds
			.apply("WriteSeedsAsAvro",
					AvroIO.write(SeedFamily.class).to(options.getOutput()).withSuffix(".avro"));
	}

	public static void narrowSearch(
			PCollection<KV<Long, SeedFamily>> potentialSeeds, SeedFinderOptions options) {

		// Checks for potential ocean monuments within the desired distance
		// of quad huts.
		int monumentNear = options.getOcean_monument_near_huts();
		if (monumentNear > 0) {
			potentialSeeds = potentialSeeds
				.apply("PotentialOceanMonuments",
						ParDo.of(new PotentialSeedFinder.HasPotentialOceanMonuments(monumentNear)));
		}

		// Annotates the seed families with all potential mansion locations.
		// There are no restrictions on the mansion locations, so all seed
		// families will have many.
		int numMansions = options.getWoodland_mansions();
		if (numMansions > 0) {
			potentialSeeds = potentialSeeds
				.apply("PotentialWoodlandMansions",
						ParDo.of(new PotentialSeedFinder.FindPotentialWoodlandMansions()));
		}

		// Expands to full 64-bit seeds, checks the biomes are present for
		// witch huts to spawn and gets the world spawn coordinates.
		PCollection<KV<Long, SeedMetadata>> fullSeeds;
		if ("".equals(options.getInput())) {
			fullSeeds = potentialSeeds
				.apply("VerifyQuadHuts", ParDo.of(new FullSeedFinder.VerifyQuadHuts()));
		} else {
			// Deaggregate from the pre-verified seed families.
			fullSeeds = potentialSeeds
				.apply("ExtractFullSeeds", ParDo.of(new SeedIO.DeaggregateSeeds()));
		}

		if (monumentNear > 0) {
			fullSeeds = fullSeeds
				.apply("VerifyOceanMonuments",
						ParDo.of(new FullSeedFinder.VerifyOceanMonuments()));
		}

		if (numMansions > 0) {
			fullSeeds = fullSeeds
				.apply("VerifyWoodlandMansions",
						ParDo.of(new FullSeedFinder.VerifyWoodlandMansions(numMansions)));
		}

		BiomeSearchConfig config = BiomeSearchConfig.getConfig(options.getSpawn_biomes());
		if (config != null) {
			fullSeeds = fullSeeds
				.apply("CheckSpawnBiomes", ParDo.of(new FullSeedFinder.HasSpawnBiomes(config)));
		}

		int searchRadius = options.getSearch_radius();
		if (options.getAll_biomes_nearby()) {
			fullSeeds = fullSeeds
				.apply("CheckAllBiomesNearby",
						ParDo.of(new FullSeedFinder.HasAllBiomesNearby(searchRadius)));
		}

		// Convert seeds to human-readable strings then write them to
		// plain text files.
		fullSeeds
			.apply("ConvertToText", ParDo.of(new DoFn<KV<Long, SeedMetadata>, String>() {
				@ProcessElement
				public void processElement(ProcessContext c) {
					c.output(c.element().getValue().asString());
				}
			}))
			.apply("WriteSeedsAsText", TextIO.write().to(options.getOutput()));
	}

	public static void main(String[] args) {
		PipelineOptionsFactory.register(SeedFinderOptions.class);
		SeedFinderOptions options =
			PipelineOptionsFactory.fromArgs(args).withValidation().as(SeedFinderOptions.class);
		Pipeline p = Pipeline.create(options);

		// TODO: Fail with a helpful error message for incompatible command line options.
		//   - Search constraints incompatible with bulk mode
		//   - Seed range options and timeout incompatible with --input
		//     (radius still makes some sense; you might want a radius for other
		//     searches which is different from the quad hut radius).

		String input = options.getInput();
		PCollection<KV<Long, SeedFamily>> potentialSeeds;
		if ("".equals(input)) {
			// Search a sequence of seeds.
			long startSeed = Math.max(parseHuman(options.getStart_seed()), 0);
			long endSeed = Math.min(parseHuman(options.getEnd_seed()), Constants.MAX_SEED);
			LOG.info(String.format("Searching seed families from %d to %d...", startSeed, endSeed));

			// Perform search on lower 48 bits of seed for potential structures.
			GenerateSequence seeds48bit = GenerateSequence.from(
					startSeed / Constants.BATCH_SIZE).to(endSeed / Constants.BATCH_SIZE);
			int maxTime = options.getMax_sequence_time();
			if (maxTime > 0) {
				seeds48bit = seeds48bit.withMaxReadTime(Duration.standardMinutes(maxTime));
			}

			int searchRadius = options.getSearch_radius();
			potentialSeeds = p
				.apply("Generate48BitSeeds", seeds48bit)
				.apply("PotentialQuadHuts",
						ParDo.of(new PotentialSeedFinder.HasPotentialQuadHuts(searchRadius)));

		} else {
			// Start with precomputed quad witch hut seeds.
			LOG.info(String.format("Reading precomputed seeds from \"%s\"...", input));
			potentialSeeds = p
				.apply("ReadPrecomputedSeeds", AvroIO.read(SeedFamily.class).from(input))
				.apply("AddKeys", ParDo.of(new DoFn<SeedFamily, KV<Long, SeedFamily>>() {
					@ProcessElement
					public void ProcessElement(ProcessContext c) {
						SeedFamily family = c.element();
						c.output(KV.of(family.baseSeed, family));
					}
				}));
		}

		if (options.getBulk_search_mode()) {
			bulkSearch(potentialSeeds, options);
		} else {
			narrowSearch(potentialSeeds, options);
		}

		PipelineResult result = p.run();
		result.waitUntilFinish();
		logMetrics(result.metrics());
	}
}
