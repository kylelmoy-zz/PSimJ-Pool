package psimjpool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import psimj.Communicator;
import psimj.network.NetworkCommunicator;
import psimj.network.NodeSocket;

public class Pool {
	public static final int MSG_REFUSE = -2;
	public static final int MSG_QUIT = -1;
	public static final int MSG_HEARTBEAT = 0;
	public static final int MSG_START = 1;
	
	private final String address;
	private final int port;
	private PoolKey key;
	
	public Pool (String poolHostAddress, int poolHostPort) {
		this.address = poolHostAddress;
		this.port = poolHostPort;
	}

	public void useAuthentication (PoolKey key) {
		this.key = key;
	}
	
	public Communicator requestCommunicator(int numNodes) throws IOException{
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
		case Pool.MSG_QUIT:
			System.err.println("Pool cannot fulfill node request.");
		default:
			System.err.println("Pool sent unrecognized message.");
		}
		return null;
	}
	
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
