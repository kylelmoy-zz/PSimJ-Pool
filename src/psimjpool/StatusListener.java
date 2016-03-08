package psimjpool;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import psimj.network.NodeSocket;

/**
 * Simple web server.
 * 
 * @author Kyle Moy
 *
 */
class StatusListener implements java.lang.Runnable {
	private static volatile boolean run = true;
	public int port;
	public static String root = "./www/";
	ServerSocket serverSocket;
	Thread thread;
	NodeListener pool;

	public StatusListener(int port, NodeListener pool) {
		this.port = port;
		this.pool = pool;
		try {
			serverSocket = new ServerSocket(port, 6);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// Accept incoming connections
		while (run) {
			try {
				Socket socket = serverSocket.accept();
				// Start a new Worker for each connection
				new ClientHandler(socket, pool).start();
			} catch (Exception e) {
			}
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
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
			serverSocket.close();
			thread.join();
		} catch (Exception e) {
		}
	}
}

class ClientHandler extends Thread {
	private Socket socket;
	private InputStream is;
	private OutputStream os;
	private NodeListener pool;

	ClientHandler(Socket socket, NodeListener pool) throws IOException {
		this.socket = socket;
		this.pool = pool;
		is = socket.getInputStream();
		os = socket.getOutputStream();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		try {
			// Make things easier
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			PrintStream out = new PrintStream(new BufferedOutputStream(os));

			// Get GET
			String request = in.readLine();

			// Keepin' Aliiiiivvee
			if (request == null)
				return;

			while (true) {
				String line = in.readLine();
				if (line.length() == 0)
					break;
			}

			// Test for GET request
			if (request.startsWith("GET")) {
				String req = request.substring(4, request.length() - 9).trim();
				if (req.contains(".json")){
					// Print some JSON
					List<NodeSocket> nodes = pool.getNodes();
					JSONObject obj = new JSONObject();
					obj.put("poolname", InetAddress.getLocalHost().getCanonicalHostName());
					JSONArray list = new JSONArray();
					for (NodeSocket node : nodes) {
						JSONObject attr = new JSONObject();
						attr.put("name", node.socket.getInetAddress().getHostAddress());
						attr.put("port", node.socket.getPort());
						attr.put("cores", node.cores);
						attr.put("os", node.osName);
						list.add(attr);
					}
	
					obj.put("nodes", list);
					out.print(buildResponseHeader(200, "OK", "application/json"));
					out.print(obj.toJSONString());
				} else {
					//Trim trailing file separator
					if (req.endsWith("/")) req.substring(0, req.length() - 1);
					
					//Get request location
					File f = new File(StatusListener.root + req);
					File fIndex =  new File(StatusListener.root + req + "/index.html");
					
					//If index.html exists, point to that instead
					if (f.isDirectory() && fIndex.exists()) f = fIndex;
					
					
					if (!f.isDirectory()) { 
						try {
							InputStream file = new FileInputStream(f);
							//Write header
							out.print(buildResponseHeader(200, "OK", f.length(), getContentType(f.getPath())));
							//Flush because we're about to write directly to the OutputStream
							out.flush();
				            byte[] buf = new byte[4096];
				            while (file.available() > 0) os.write(buf, 0, file.read(buf));
						} catch (FileNotFoundException e) {
							//Write 404 Error Page
							out.println(buildResponseHeader(404, "Not Found", "text/html"));
							out.println("<html><body><h1>404 NOT FOUND</h1></body></html>");
						}
					}
				}
			} else {
				// Write 400 Error Page
				out.println(buildResponseHeader(400, "Bad Request", "text/html"));
				out.println("<html><body><h1>400 BAD REQUEST</h1></body></html>");
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			// I really don't care.
		} finally {
			// Clean up
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Builds a response header
	 * 
	 * @param code
	 *            The response code
	 * @param title
	 *            The response title
	 * @param contentType
	 *            The MIME type
	 * @return The response header
	 */
	public static String buildResponseHeader(int code, String title, String contentType) {
		return "HTTP/1.0 " + code + " " + title + " \r\n" + "Content-Type: " + contentType + "\r\n" + "Date: "
				+ new Date() + "\r\n" + "Server: Kyle's Web Server 1.0\r\n\r\n";
	}

	/**
	 * Builds a response header, with Content-Length
	 * 
	 * @param code
	 *            The response code
	 * @param title
	 *            The response title
	 * @param contentLength
	 *            The content length
	 * @param contentType
	 *            The MIME type
	 * @return The response header
	 */
	public static String buildResponseHeader(int code, String title, long contentLength, String contentType) {
		return "HTTP/1.0 " + code + " " + title + " \r\n" + "Content-Length: " + contentLength + "\r\n"
				+ "Content-Type: " + contentType + "\r\n" + "Date: " + new Date() + "\r\n"
				+ "Server: Kyle's Web Server 1.0\r\n\r\n";
	}

	/**
	 * Guesses the content type of a file
	 * 
	 * @param path
	 *            The file path
	 * @return The guessed content type
	 * @throws IOException
	 */
	public static String getContentType(String path) throws IOException {
		return Files.probeContentType(Paths.get(path));
	}
}