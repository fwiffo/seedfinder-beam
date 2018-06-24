package org.fwiffo.seedfinder;

import java.lang.Math;

import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation.Required;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.KV;
import org.joda.time.Duration;

import org.fwiffo.seedfinder.biome.BiomeSearchConfig;
import org.fwiffo.seedfinder.Constants;
import org.fwiffo.seedfinder.finder.StructureFinder;
import org.fwiffo.seedfinder.finder.BiomeFinder;
import org.fwiffo.seedfinder.util.SeedMetadata;

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
	// Only search the lower 48 bits of the key space.
	public interface SeedFinderOptions extends PipelineOptions {
		@Description("Path of the file to write to")
		@Required
		String getOutput();
		void setOutput(String value);

		@Description("Radius to search for structures and biomes, in blocks; " +
				"structures will round up to an integer number of regions")
		@Default.Integer(2048)
		int getSearch_radius();
		void setSearch_radius(int value);

		@Description("Search for seeds with specific biomes at spawn; " +
				"flower_forest, ice_spikes, jungle, mega_taiga, mesa, mushroom_island, ocean")
		@Default.Enum("none")
		BiomeSearchConfig.Name getSpawn_biomes();
		void setSpawn_biomes(BiomeSearchConfig.Name value);

		@Description("Search for seeds with a number nearby Woodland Mansions")
		@Default.Integer(0)
		int getWoodland_mansions();
		void setWoodland_mansions(int value);

		@Description("Lower 48 bits of start seed for search; 0 to 256T")
		@Default.String("0")
		String getStart_seed();
		void setStart_seed(String value);

		@Description("Lower 48 bits of end seed for search; 0 to 256T")
		@Default.String("1G")
		String getEnd_seed();
		void setEnd_seed(String value);

		@Description("Maximum time for generating candidate seeds in minutes")
		@Default.Integer(0)
		int getMax_sequence_time();
		void setMax_sequence_time(int value);
	}

	private static long parseHuman(String value) {
		if (value.length() < 1) {
			return 0L;
		}
		long number = Long.parseLong(value.substring(0, value.length()-1));
		switch (value.substring(value.length()-1)) {
			case "T": return (1<<40) * number;
			case "G": return (1<<30) * number;
			case "M": return (1<<20) * number;
			case "K": return (1<<10) * number;
		}
		return number;
	}

	// TODO:
	// Biome types for spawn and search radius
	//   options for choosing a specific spawn biome
	//   options for having all biome types in the search region
	//   options for having lots of a particular biome (e.g. mushroom)
	// Monuments close to quad huts
	// Stronghold close to quad huts
	public static void main(String[] args) {
		SeedFinderOptions options =
			PipelineOptionsFactory.fromArgs(args).withValidation().as(SeedFinderOptions.class);
		Pipeline p = Pipeline.create(options);

		long startSeed = Math.max(
				parseHuman(options.getStart_seed()) / Constants.BATCH_SIZE, 0);
		long endSeed = Math.min(
				parseHuman(options.getEnd_seed()) / Constants.BATCH_SIZE, Constants.MAX_SEED);

		// Operations on the lower 48-bits of the seed.
		GenerateSequence seeds48bit = GenerateSequence.from(startSeed).to(endSeed);
		if (options.getMax_sequence_time() > 0) {
			seeds48bit = seeds48bit.withMaxReadTime(
					Duration.standardMinutes(options.getMax_sequence_time()));
		}

		PCollection<KV<Long, SeedMetadata>> seeds = p.apply(seeds48bit)
			.apply(ParDo.of(new StructureFinder.HasPotentialQuadHuts(
					options.getSearch_radius())));

		if (options.getWoodland_mansions() > 0) {
			seeds = seeds.apply(ParDo.of(new StructureFinder.FindPotentialWoodlandMansions()));
		}

		// Expands to full 64-bit seeds to do checks that require biomes.
		seeds = seeds.apply(ParDo.of(new StructureFinder.VerifyQuadHuts()));

		if (options.getWoodland_mansions() > 0) {
			seeds = seeds.apply(ParDo.of(
					new StructureFinder.VerifyWoodlandMansions(options.getWoodland_mansions())));
		}

		BiomeSearchConfig config = BiomeSearchConfig.getConfig(options.getSpawn_biomes());
		if (config != null) {
			seeds = seeds.apply(ParDo.of(new BiomeFinder.HasSpawnBiomes(config)));
		}

		seeds.apply(ParDo.of(new DoFn<KV<Long, SeedMetadata>, String>() {
			@ProcessElement
			public void processElement(ProcessContext c)  {
				c.output(c.element().getValue().asString());
			}
		}))
		.apply("WriteSeeds", TextIO.write().to(options.getOutput()));

		p.run().waitUntilFinish();
	}
}
