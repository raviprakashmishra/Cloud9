package edu.umd.cloud9.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.umd.cloud9.util.MapIF;
import edu.umd.cloud9.util.OHMapIF;

/**
 * <p>
 * Writable representing a map where keys are ints and values are floats.
 * </p>
 * 
 * <p>
 * One notable feature of this class is the ability to support <i>lazy decoding</i>,
 * controlled by the {@link #setLazyDecodeFlag(boolean)} method. In lazy
 * decoding mode, when an object of this type is deserialized, key-value pairs
 * are not inserted into the map, but rather held in arrays. The reduces memory
 * used in cases where random access to values is not required. In lazy decoding
 * mode, the raw keys and values may be fetched by the {@link #getKeys()} and
 * {@link #getValues()} methods, respectively. The map can be subsequently
 * populated with the {@link #decode()} method.
 * </p>
 */
public class OHMapIFW extends OHMapIF implements Writable {

	private static boolean sLazyDecode = false;
	private static final long serialVersionUID = 4760032853L;

	private int mNumEntries = 0;
	private int[] mKeys = null;
	private float[] mValues = null;

	/**
	 * Creates a <code>OHMapIFW</code> object.
	 */
	public OHMapIFW() {
		super();
	}

	/**
	 * Deserializes the map.
	 * 
	 * @param in
	 *            source for raw byte representation
	 */
	@SuppressWarnings("unchecked")
	public void readFields(DataInput in) throws IOException {

		this.clear();

		mNumEntries = in.readInt();
		if (mNumEntries == 0)
			return;

		if (sLazyDecode) {
			// lazy initialization; read into arrays
			mKeys = new int[mNumEntries];
			mValues = new float[mNumEntries];

			for (int i = 0; i < mNumEntries; i++) {
				mKeys[i] = in.readInt();
				mValues[i] = in.readFloat();
			}
		} else {
			// normal initialization; populate the map
			for (int i = 0; i < mNumEntries; i++) {
				put(in.readInt(), in.readFloat());
			}
		}
	}

	/**
	 * In lazy decoding mode, populates the map with deserialized data.
	 * Otherwise, does nothing.
	 * 
	 * @throws IOException
	 */
	public void decode() throws IOException {
		if (mKeys == null)
			return;

		for (int i = 0; i < mKeys.length; i++) {
			put(mKeys[i], mValues[i]);
		}
	}

	/**
	 * Serializes the map.
	 * 
	 * @param out
	 *            where to write the raw byte representation
	 */
	public void write(DataOutput out) throws IOException {
		// Write out the number of entries in the map
		out.writeInt(size());
		if (size() == 0)
			return;

		for (MapIF.Entry e : entrySet()) {
			// WritableUtils.writeVInt(out, e.getKey());
			out.writeInt(e.getKey());
			out.writeFloat(e.getValue());
		}
	}

	/**
	 * Returns the serialized representation of this object as a byte array.
	 * 
	 * @return byte array representing the serialized representation of this
	 *         object
	 * @throws IOException
	 */
	public byte[] serialize() throws IOException {
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(bytesOut);
		write(dataOut);

		return bytesOut.toByteArray();
	}

	/**
	 * Creates a <code>OHMapIFW</code> object from a <code>DataInput</code>.
	 * 
	 * @param in
	 *            <code>DataInput</code> for reading the serialized
	 *            representation
	 * @return a newly-created <code>OHMapIFW</code> object
	 * @throws IOException
	 */
	public static OHMapIFW create(DataInput in) throws IOException {
		OHMapIFW m = new OHMapIFW();
		m.readFields(in);

		return m;
	}

	/**
	 * Creates a <code>OHMapIFW</code> object from a byte array.
	 * 
	 * @param bytes
	 *            raw serialized representation
	 * @return a newly-created <code>OHMapIFW</code> object
	 * @throws IOException
	 */
	public static OHMapIFW create(byte[] bytes) throws IOException {
		return OHMapIFW.create(new DataInputStream(new ByteArrayInputStream(bytes)));
	}

	/**
	 * Sets the lazy decoding flag.
	 * 
	 * @param b
	 *            the value of the lazy decoding flag
	 */
	public static void setLazyDecodeFlag(boolean b) {
		sLazyDecode = b;
	}

	/**
	 * Returns the value of the lazy decoding flag
	 * 
	 * @return the value of the lazy decoding flag
	 */
	public static boolean getLazyDecodeFlag() {
		return sLazyDecode;
	}

	/**
	 * In lazy decoding mode, returns an array of all the keys if the map hasn't
	 * been decoded yet. Otherwise, returns null.
	 * 
	 * @return an array of all the keys
	 */
	public int[] getKeys() {
		return mKeys;
	}

	/**
	 * In lazy decoding mode, returns an array of all the values if the map
	 * hasn't been decoded yet. Otherwise, returns null.
	 * 
	 * @return an array of all the values
	 */
	public float[] getValues() {
		return mValues;
	}

	/**
	 * In lazy decoding mode, adds values from keys of another map to this map.
	 * This map must have already been decoded, but the other map must not have
	 * been already decoded.
	 * 
	 * @param m
	 *            the other map
	 */
	public void lazyplus(OHMapIFW m) {
		int[] k = m.getKeys();
		float[] v = m.getValues();

		for (int i = 0; i < k.length; i++) {
			if (this.containsKey(k[i])) {
				this.put(k[i], this.get(k[i]) + v[i]);
			} else {
				this.put(k[i], v[i]);
			}
		}
	}
}