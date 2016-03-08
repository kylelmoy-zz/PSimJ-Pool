package psimjpool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import psimj.Communicator;
import psimj.network.NetworkCommunicator;
import psimj.network.NodeSocket;

/**
 * Utility for interfacing with a PSimJ Pool
 * 
 * @author Kyle Moy
 *
 */
public class Pool {
	public static final int ALL = -1;

	static final int MSG_REFUSE = -2;
	static final int MSG_QUIT = -1;
	static final int MSG_HEARTBEAT = 0;
	static final int MSG_START = 1;
	static final int MSG_ACCEPT = 2;

	private final String address;
	private final int port;
	private PoolKey key = PoolKey.DEFAULT_KEY;

	/**
	 * Constructs a Pool for the specified address
	 * 
	 * @param poolHostAddress
	 * @param poolHostPort
	 */
	public Pool(String poolHostAddress, int poolHostPort) {
		this.address = poolHostAddress;
		this.port = poolHostPort;
	}

	/**
	 * Specifies a key to use for authentication
	 * 
	 * @param key
	 */
	public void useAuthentication(PoolKey key) {
		this.key = key;
	}

	/**
	 * Attempts to connect to the PSimJ Pool, then requests a list of IPs to
	 * connect
	 * 
	 * @param numNodes
	 *            the number of PSimJ Nodes to request
	 * @return the Communicator for communicating with the requested PSimJ Nodes
	 * @throws IOException
	 */
	public Communicator requestCommunicator(int numNodes) throws IOException {
		if (numNodes == 0 || numNodes == 1) {
			System.err.println("Request must be greater than 0.");
			return null;
		}

		// Connect to pool host
		NodeSocket pool = NodeSocket.openNow(address, port);

		// Submit key for verification
		key.submit(pool);

		// Request n nodes from pool
		pool.os.writeInt(numNodes);

		int msg = pool.is.readInt();

		switch (msg) {
		case Pool.MSG_START:
			return buildCommunicator(pool);
		case Pool.MSG_REFUSE:
			System.err.println("Pool refused our key.");
			break;
		case Pool.MSG_QUIT:
			System.err.println("Pool cannot fulfill node request.");
			break;
		default:
			System.err.println("Pool sent unrecognized message.");
			break;
		}
		return null;
	}

	/**
	 * Builds a Communicator from a list of IPs
	 * 
	 * @param hostSocket
	 *            the source of the IP list
	 * @return
	 * @throws IOException
	 */
	public static Communicator buildCommunicator(NodeSocket hostSocket) throws IOException {
		// Receive my rank from pool
		int rank = hostSocket.is.readInt();

		// Receive other node info from pool
		List<String> ipList = new ArrayList<String>();
		int size = hostSocket.is.readInt();
		for (int i = 0; i < size; i++) {
			int bufSize = hostSocket.is.readInt();
			byte[] buf = new byte[bufSize];
			hostSocket.is.readFully(buf);
			ipList.add(new String(buf));
		}

		return new NetworkCommunicator(ipList, rank);
	}
}
