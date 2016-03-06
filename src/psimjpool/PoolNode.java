package psimjpool;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import psimj.Communicator;
import psimj.network.NodeSocket;

class PoolNode {
	static void start(String hostAddress, int hostPort, PoolKey key) throws Exception {
		System.out.println("PSimJ Pool Node");
		System.out.println("@" + hostAddress + ":" + hostPort);
		if (key == null) {
			key = PoolKey.DEFAULT_KEY;
		}
		
		// Try connecting to host
		while (true) {
			try {
				NodeSocket pool = new NodeSocket(new Socket(hostAddress, hostPort));
				// Submit key for verification
				key.submit(pool);

				try {
					while (true) {
						// Wait for directives from pool
						int msg = 0;
						while (msg != Pool.MSG_START) {
							msg = pool.is.readInt();
							switch(msg) {
							case Pool.MSG_START:
								break;
							case Pool.MSG_HEARTBEAT:
								break;
							case Pool.MSG_REFUSE:
								System.err.println("Pool refused key.");
							case Pool.MSG_QUIT:
								System.exit(0);
							}
						}
						
						try {
							// Initialize communications with all nodes
							Communicator comm = Pool.buildCommunicator(pool);
							comm.runTask(null);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				} catch (IOException e) {
					//Connection lost
					System.out.println("Connection lost.");
				}
				pool.close();
			} catch (UnknownHostException e) {
				System.err.println("Unable to resolve host.");
			} catch (IOException e) {
				// Do nothing, try again later.
			}
			try {
				Thread.sleep(1000);
			} catch (Exception s) {
			}
		}
	}
}
