package psimjpool;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import psimj.network.NodeSocket;

/**
 * A listening server for incoming connections
 * 
 * @author Kyle Moy
 *
 */
class NodeListener implements java.lang.Runnable {
	private final int port;

	private boolean run;
	private List<NodeSocket> nodePool;
	private Thread thread;
	private ServerSocket serverSocket;
	private PoolKey key = PoolKey.DEFAULT_KEY;

	NodeListener(int port) {
		this.port = port;
		nodePool = new ArrayList<NodeSocket>();
	}
	
	public void useAuthentication (PoolKey key) {
		this.key = key;
	}
	
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			run = true;

			// This creates a new thread to call heartbeat() every 500 ms
			(new Thread() {
				public void run() {
					while (run) {
						heartbeat();
						try {
							Thread.sleep(500);
						} catch (Exception e) {
						}
					}
				}
			}).start();

			// Listen for new nodes
			while (run) {
				try {
					Socket nodeSocket = serverSocket.accept();
					NodeSocket node = new NodeSocket(nodeSocket);

					// Verify key before adding to pool
					if (key.validate(node)) {
						nodePool.add(node);
					} else {
						node.os.writeInt(Pool.MSG_REFUSE);
						node.close();
					}
				} catch (IOException e) {
					// Timed out, try again
				}
			}
			serverSocket.close();
		} catch (IOException e) {
			// Host port already in use, probably
			e.printStackTrace();
		}
	}

	public List<NodeSocket> getNodes() {
		heartbeat();
		return nodePool;
	}

	public void start() {
		if (thread == null || !thread.isAlive()) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		try {
			run = false;
			serverSocket.close();

			// Wait for closure
			thread.join();
		} catch (Exception e) {
		}
	}

	private void heartbeat() {
		synchronized (nodePool) {
			if (nodePool.size() > 0) {
				for (int i = 0; i < nodePool.size(); i++) {
					NodeSocket node = nodePool.get(i);
					try {
						new DataOutputStream(node.os).writeInt(Pool.MSG_HEARTBEAT);
					} catch (Exception e) {
						nodePool.remove(i--);
					}
				}
			}
		}
	}

}