package psimjpool;

class PoolHost {
	static void start(int nodePort, int listenPort, int wwwPort, PoolKey key) throws Exception {
		System.out.println("PSimJ Pool Server");
		System.out.println("Node port: " + nodePort);
		NodeListener nodeListener = new NodeListener(nodePort);
		nodeListener.start();

		System.out.println("Web UI port: " + wwwPort);
		StatusListener www = new StatusListener(wwwPort, nodeListener);
		www.start();

		System.out.println("Requests port: " + listenPort);
		ClientListener clientListener = new ClientListener(listenPort, nodeListener);
		clientListener.start();
		
		if (key != null) {
			System.out.println("Use Authentication: YES");
			nodeListener.useAuthentication(key);
			clientListener.useAuthentication(key);
		} else {
			System.out.println("Use Authentication: NO");
		}
		
		//clientListener.stop();
		//nodeListener.stop();
		//www.stop();
	}


}
