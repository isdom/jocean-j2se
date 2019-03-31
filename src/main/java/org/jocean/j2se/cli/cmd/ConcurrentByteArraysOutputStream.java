/**
 *
 */
package org.jocean.j2se.cli.cmd;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Marvin.Ma
 *
 */
public class ConcurrentByteArraysOutputStream extends OutputStream {

	private final	BlockingQueue<byte[]>	byteArrayList = new LinkedBlockingQueue<byte[]>();

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(final int b) throws IOException {
		byteArrayList.offer( new byte[]{(byte)b} );
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		byteArrayList.offer( Arrays.copyOfRange(b, off, off + len) );
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(final byte[] b) throws IOException {
		byteArrayList.offer( Arrays.copyOf(b, b.length) );
	}

	public byte[] poll(final long timeoutAsMillis) throws InterruptedException {
		return byteArrayList.poll(timeoutAsMillis, TimeUnit.MILLISECONDS);
	}

	public int drainTo(final Collection<? super byte[]> c) {
		return byteArrayList.drainTo(c);
	}

	public int getByteArrayCount() {
		return byteArrayList.size();
	}
}
