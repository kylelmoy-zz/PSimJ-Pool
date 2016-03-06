package psimjpool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import psimj.network.NodeSocket;

/**
 * Key authentication utility
 * 
 * @author Kyle Moy
 *
 */
public class PoolKey {
	public static final PoolKey DEFAULT_KEY = new PoolKey(new byte[] { 0, 1, 2, 4, 8, 16, 32, 64 });
	private final byte[] key;

	/**
	 * Constructs a PoolKey from a byte array
	 * 
	 * @param byteArray
	 */
	public PoolKey(byte[] byteArray) {
		this.key = byteArray;
	}

	/**
	 * Constructs a PoolKey from a file
	 * 
	 * @param path
	 * @throws IOException
	 */
	public PoolKey(String path) throws IOException {
		key = Files.readAllBytes(Paths.get(path));
	}

	/**
	 * Sends this key over a NodeSocket
	 * 
	 * @param socket
	 * @throws IOException
	 */
	void submit(NodeSocket socket) throws IOException {
		socket.os.writeInt(key.length);
		socket.os.write(key);
	}

	/**
	 * Validates a key over a NodeSocket
	 * 
	 * @param socket
	 * @return true if the received key matches this PoolKey
	 * @throws IOException
	 */
	boolean validate(NodeSocket socket) throws IOException {
		// Get key from node
		socket.socket.setSoTimeout(1000);
		int length = socket.is.readInt();
		byte[] test = new byte[length];
		socket.is.readFully(test);
		socket.socket.setSoTimeout(0);

		if (length != key.length) {
			return false;
		}
		for (int i = 0; i < key.length; i++) {
			if (key[i] != test[i]) {
				return false;
			}
		}
		return true;
	}
}
