package psimjpool;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Not enough args!");
			return;
		}
		if (args[0].equals("host")) {
			if (args.length < 4) {
				System.err.println("Not enough args for host!");
				return;
			}

			int nodePort = Integer.parseInt(args[1]);
			int listenPort = Integer.parseInt(args[2]);
			int wwwPort = Integer.parseInt(args[3]);
			PoolKey key = null;
			if (args.length >= 5) {
				key = new PoolKey(Files.readAllBytes(Paths.get(args[4])));
			}
			PoolHost.start(nodePort, listenPort, wwwPort, key);
		} else if (args[0].equals("node")) {
			if (args.length < 3) {
				System.err.println("Not enough args for node!");
				return;
			}

			String host = args[1];
			int port = Integer.parseInt(args[2]);
			PoolKey key = null;
			if (args.length >= 4) {
				key = new PoolKey(Files.readAllBytes(Paths.get(args[3])));
			}
			PoolNode.start(host, port, key);
		} else {
			System.err.println("Mode not specified!");
		}
	}

}
