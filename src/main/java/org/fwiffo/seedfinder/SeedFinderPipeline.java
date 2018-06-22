package org.fwiffo.seedfinder;

import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft seed finder Beam pipeline.
 *
 * <p>To run locally
 * mvn compile exec:java \
 *   -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
 *   -Dexec.args="--output=./output/"
 *
 * <p>To run using managed resource in Google Cloud Platform:
 * mvn compile exec:java \
 *   -Dexec.mainClass=org.fwiffo.seedfinder.SeedFinderPipeline \
 *   -Dexec.args="--project=<YOUR_PROJECT_ID>"
 *   --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE>
 *   --output=<OUTPUT_LOCATION_IN_CLOUD_STORAGE>
 *   --runner=DataflowRunner
 */
public class SeedFinderPipeline {
	private static final Logger LOG = LoggerFactory.getLogger(SeedFinderPipeline.class);

	public static void main(String[] args) {
		Pipeline p = Pipeline.create(
				PipelineOptionsFactory.fromArgs(args).withValidation().create());

		// TODO: Generate 0 -> 2**48-1, pipe it through filters for potential
		// structure generation, expand to full 64-bits, pipe it through filters
		// for actual structure generation and biomes and whatever else.
		p.apply(GenerateSequence.from(0x00).to(0xff))
		.apply(ParDo.of(new DoFn<Long, Void>() {
			@ProcessElement
			public void processElement(ProcessContext c)  {
				LOG.info(String.format("%d", c.element()));
			}
		}));

		p.run();
	}
}
