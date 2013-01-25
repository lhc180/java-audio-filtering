package org.emmef.samples;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.emmef.samples.BytesPerSample.Storage;
import org.junit.Test;

public class SampleCodecsTest {
	private static final String HEXDIGITS = "0123456789abcdef";

	public SampleCodecsTest() {
		// TODO Auto-generated constructor stub
	}
	
	@Test
	public void testIdempotencyForDoubles() {
		for (SampleCodec codec : SampleCodecs.values()) {
			CodecHelper helper = new CodecHelper(codec, true);
			testIdempotencyFor(helper);
		}
	}
	
	@Test
	public void testIdempotencyForFloats() {
		for (SampleCodec codec : SampleCodecs.values()) {
			CodecHelper helper = new CodecHelper(codec, false);
			testIdempotencyFor(helper);
		}
	}
	
	@Test
	public void testLimitingBehaviorForDouble() {
		for (SampleCodec codec : SampleCodecs.values()) {
			CodecHelper helper = new CodecHelper(codec, true);
			testLimitingFor(helper);
		}
	}
	
	@Test
	public void testLimitingBehaviorForFloats() {
		for (SampleCodec codec : SampleCodecs.values()) {
			CodecHelper helper = new CodecHelper(codec, false);
			testLimitingFor(helper);
		}
	}


	private void testIdempotencyFor(CodecHelper helper) {
		StringBuilder message = new StringBuilder();
		
		long min = Integer.MIN_VALUE;
		long max = -(long)Integer.MIN_VALUE;
		long steps = Math.min(helper.bitsmax, -SampleScales.MIN_VALUE_24_BIT);
		long step = Math.max(1, -(long)Integer.MIN_VALUE / steps);
		
		for (long i = min ; i <= max; i += step) {
			double input;
			if (i != max) {
				input = -1.0 * i / Integer.MIN_VALUE;
			}
			else {
				input = helper.ceiling;
			}
			double output = helper.encodeDecodeInSameSpace(input);
			double delta = input - output;

			if (Math.abs(delta) > helper.epsilon) {
				message.setLength(0);
				message.append("Codec=").append(helper.codec).append("; input=").append(input).append("; encoded=");
				helper.resetBuffer();
				for (int bit = 0; bit < helper.codec.bytesPerSample(); bit++) {
					int byteValue = helper.buffer.get();
					message.append(HEXDIGITS.charAt(0xf & byteValue >>> 4));
					message.append(HEXDIGITS.charAt(0xf & byteValue));
				}
				message.append("; decoded=").append(output).append("; delta=").append(delta).append("; epsilon-").append(helper.epsilon);
				message.insert(0, "Delta too big: ");
				System.err.println(message);
				output = helper.encodeDecodeInSameSpace(input);
				fail(message.toString());
			}
		}
	}
	private void testLimitingFor(CodecHelper helper) {
		
		double input = 0.1;
		
		for (input = 0.1; input < SampleScales.MAX_VALUE_24_BIT; input *= 1.5) {
			assertExpectedOutput(helper, input);
			assertExpectedOutput(helper, -input);
		}
	}

	private void assertExpectedOutput(CodecHelper helper, double input) {
		double output = helper.encodeDecodeInSameSpace(input);
		double epsilon = helper.epsilon * 2; // double faults of non-integer float values can give double faults
		double expected = input;
		boolean scalingType = helper.codec.getStorage() == Storage.FLOAT;
		
		if (input > helper.ceiling) {
			if (scalingType) {
				epsilon *= input;
			}
			else {
				expected = helper.ceiling;
			}
		}
		else if (input < helper.floor) {
			if (scalingType) {
				epsilon *= -input;
			}
			else {
				expected = helper.floor;
			}
		}
		double delta = Math.abs(expected - output);
		int deltaFactor = (int)(100 * delta / epsilon);
		boolean condition = delta < epsilon;
		if (!condition) {
			String message = helper.codec + ": with input " + input + ", output (" + output + ") should be " + expected + "(" + deltaFactor + "% epsilon)";
			System.err.println(message);
			System.err.println(" " + output * helper.bitsmax + " <-> " + expected * helper.bitsmax + "\n" + helper.floor * helper.bitsmax + ".." + helper.ceiling * helper.bitsmax);
			fail(message);
		}
	}
	
	static class CodecHelper {
		public final double floor;
		public final double ceiling;
		public final double epsilon;
		public final SampleCodec codec;
		public final ByteBuffer buffer;
		public final long bitsmax;
		private final boolean useDoubles;
		
		public CodecHelper(SampleCodec codec, boolean useDoubles) {
			this.codec = codec;
			this.useDoubles = useDoubles;
			bitsmax = 1L << Math.min(3, codec.bytesPerSample())*8 - 1;
			ceiling = codec.getStorage() == Storage.TWOS_COMPLEMENT ? 1.0 * (bitsmax - 1) / bitsmax : 1.0;
			epsilon = 0.50001 / bitsmax; // double faults: to and fro
			floor = -1.0;
			buffer = ByteBuffer.allocateDirect(16);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		double encodeDecodeInSameSpace(double input) {
			if (useDoubles) {
				resetBuffer();
				codec.encodeDouble(buffer, input);
				resetBuffer();
				return codec.decodeDouble(buffer);
			}
			else {
				resetBuffer();
				codec.encodeFloat(buffer, (float)input);
				resetBuffer();
				return codec.decodeFloat(buffer);
			}
		}
		
		void resetBuffer() {
			buffer.position(0);
		}
	}
}