package org.emmef.audio.nodes;

import java.io.IOException;

public interface SoundSource extends SoundSourceOrSink {
	
	long readFrames(double[] buffer) throws IOException;

	long readFrames(float[] buffer) throws IOException;

	long readFrames(double[] buffer, int frameCount) throws IOException;

	long readFrames(float[] buffer, int frameCount) throws IOException;

}
