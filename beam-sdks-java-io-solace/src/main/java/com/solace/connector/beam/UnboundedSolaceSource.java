package com.solace.connector.beam;

import com.google.common.annotations.VisibleForTesting;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.display.DisplayData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

@VisibleForTesting
class UnboundedSolaceSource<T> extends UnboundedSource<T, SolaceCheckpointMark> {
	private static final long serialVersionUID = 42L;
	private static final Logger LOG = LoggerFactory.getLogger(UnboundedSolaceSource.class);
	private final SolaceIO.Read<T> spec;
	private String queueName;

	public SolaceIO.Read<T> getSpec() {
		return spec;
	}

	public String getQueueName() {
		return queueName;
	}


	public UnboundedSolaceSource(SolaceIO.Read<T> spec) {
		this.spec = spec;
	}

	@Override
	public UnboundedReader<T> createReader(
			PipelineOptions options, @Nullable SolaceCheckpointMark checkpointMark) {
		LOG.debug("createReader() is called.");

		// it makes no sense to resume a Solace Session with the previous checkpoint
		// so don't need the checkpoint to new a Solace Reader
		return new UnboundedSolaceReader<T>(this);
	}

	@Override
	public List<UnboundedSolaceSource<T>> split(int desiredNumSplits, PipelineOptions options) {
		LOG.info("Queue Numbers: {}, desiredNumSplits: {}, PipelineOptions: {}",
				spec.queues().size(), desiredNumSplits, options);

		List<UnboundedSolaceSource<T>> sourceList = new ArrayList<>();
		for (String queueName : spec.queues()) {
			UnboundedSolaceSource<T> source = new UnboundedSolaceSource<T>(spec);
			source.queueName = queueName;
			sourceList.add(source);
		}
		return sourceList;
	}

	@Override
	public void populateDisplayData(DisplayData.Builder builder) {
		spec.populateDisplayData(builder);
	}

	@Override
	public Coder<SolaceCheckpointMark> getCheckpointMarkCoder() {
		return SerializableCoder.of(SolaceCheckpointMark.class);
	}

	@Override
	public Coder<T> getOutputCoder() {
		return this.spec.coder();
	}

}