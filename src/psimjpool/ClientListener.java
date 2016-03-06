package psimjpool;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import psimj.network.NodeSocket;

class ClientListener implements Runnable {
	private int listenPort;
	private NodeListener nodeListener;
	private Thread thread;
	private boolean run;
	private PoolKey key = PoolKey.DEFAULT_KEY;
	
	ClientListener (int port, NodeListener listener) {
		listenPort = port;
		this.nodeListener = listener;
	}
	
	public void useAuthentication (PoolKey key) {
		this.key = key;
	}
	
	@Override
	public void run() {
		run = true;
		while (run) {
			try {
				ServerSocket serverSocket = new ServerSocket(listenPort);
				while (run) {
					try {
						NodeSocket client = new NodeSocket(serverSocket.accept());

						List<NodeSocket> nodes = nodeListener.getNodes();
						
						if (!key.validate(client)) {
							client.os.writeInt(Pool.MSG_REFUSE);
						}
						
						// Receive node count request, exclude the requester themselves
						int ncount = client.is.readInt() - 1;

						if (ncount > nodes.size()) {
							client.os.writeInt(Pool.MSG_QUIT);
							client.close();
							continue;
						} else if (ncount < 0) {
							// Assume all nodes
							ncount = nodes.size();
						}

						// Build a pool to meet ncount nodes
						List<NodeSocket> requestPool = new ArrayList<NodeSocket>();
						requestPool.add(client);
						for (int i = 0; i < ncount; i++) {
							requestPool.add(nodes.get(i));
						}

						// Send node ips to tasker
						int poolSize = requestPool.size();
						for (int i = 0; i < poolSize; i++) {
							NodeSocket node = requestPool.get(i);
							
							// Tell nodes to start
							node.os.writeInt(Pool.MSG_START);
							
							// Send this node's rank
							node.os.writeInt(i);
							
							// Send total pool size
							node.os.writeInt(poolSize);
						}
						
						// Send IP list
						for (int i = 0; i < poolSize; i++) {
							byte[] buf = requestPool.get(i).ip.getBytes();
							for (NodeSocket node : requestPool) {
								node.os.writeInt(buf.length);
								node.os.write(buf);
							}
						}
						
						// Done
						client.close();
					} catch (IOException e) {
						// Timed out, try again
					}
				}

				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
			// Wait for closure
			thread.join();
		} catch (Exception e) {
		}
	}
}
