/**
 * 
 */
package connections;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Wolfram Wingerath
 * 
 */
public class Server {
	private static Logger logger = LoggerFactory.getLogger(Server.class);
	private ServerSocketChannel serverSocket;
	private ServiceHandler handler;

	public Server(int port) throws IOException {
		this.serverSocket = ServerSocketChannel.open();
		serverSocket.socket().bind(new InetSocketAddress(port));
	}

	public void serve() {
		logger.info("Listening to external connections on " + serverSocket);

		new ConnectionServer(serverSocket, handler).run();
	}

	private class ConnectionServer implements Runnable {
		ServerSocketChannel serverSocket;
		ServiceHandler handler = null;

		private ConnectionServer(ServerSocketChannel serverSocket,
				ServiceHandler handler) {
			this.serverSocket = serverSocket;
			this.handler = handler;
		}

		@Override
		public void run() {
			while (true) {
				try {
					SocketChannel clientSocket = serverSocket.accept();

					new Thread(new ConnectionHandler(clientSocket, handler))
							.start();
				} catch (IOException e) {
					logger.warn("Error accepting socket on " + serverSocket, e);
				}
			}
		}
	}

	private class ConnectionHandler implements Runnable {
		KryoSerializer serializer = new KryoSerializer();
		ServiceHandler handler = null;
		SocketChannel clientSocket = null;

		public ConnectionHandler(SocketChannel clientSocket,
				ServiceHandler handler) {
			this.clientSocket = clientSocket;
			serializer.setInputStream(Channels.newInputStream(clientSocket));
			serializer.setOutputStream(Channels.newOutputStream(clientSocket));
			this.handler = handler;
		}

		@Override
		public void run() {
			try {
				while (true) {
					Object request = serializer.getObject();

					if (request instanceof String) {
						if (request.equals("EXIT")) {
							clientSocket.close();
							return;
						}
					} else if (request instanceof Request) {
						Response response = handler
								.processRequest((Request) request);
						serializer.serialize(response);
					}

				}
			} catch (KryoException e) {
				logger.error("Kryo error with client " + clientSocket, e);

				try {
					clientSocket.close();
				} catch (IOException ex) {
					logger.error("Exception closing connection: ", ex);
				}
			} catch (Exception e) {
				logger.error("Error executing request for client "
						+ clientSocket, e);
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);

				try {
					serializer.serialize(new ClientError(e + " "
							+ sw.toString()));
				} catch (IOException ioe) {
					logger.error("Error executing serialize for client error",
							ioe);
				}

			}
		}
	}
}
