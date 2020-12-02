/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stevens.cs549.ftpclient;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

// import org.apache.log4j.PropertyConfigurator;

import edu.stevens.cs549.ftpinterface.IServer;
import edu.stevens.cs549.ftpinterface.IServerFactory;

/**
 * 
 * @author dduggan
 */
public class Client {

	private static String clientPropsFile = "/client.properties";
	private static String loggerPropsFile = "/log4j.properties";

	protected String clientIp;
	
	protected String serverAddr;
	
	protected int serverPort;
	
	private static Logger log = Logger.getLogger(Client.class.getCanonicalName());

	public void severe(String s) {
		log.severe(s);
	}

	public void warning(String s) {
		log.info(s);
	}

	public void info(String s) {
		log.info(s);
	}

	protected List<String> processArgs(String[] args) {
		List<String> commandLineArgs = new ArrayList<String>();
		int ix = 0;
		Hashtable<String, String> opts = new Hashtable<String, String>();

		while (ix < args.length) {
			if (args[ix].startsWith("--")) {
				String option = args[ix++].substring(2);
				if (ix == args.length || args[ix].startsWith("--"))
					severe("Missing argument for --" + option + " option.");
				else if (opts.containsKey(option))
					severe("Option \"" + option + "\" already set.");
				else
					opts.put(option, args[ix++]);
			} else {
				commandLineArgs.add(args[ix++]);
			}
		}
		/*
		 * Overrides of values from configuration file.
		 */
		Enumeration<String> keys = opts.keys();
		while (keys.hasMoreElements()) {
			String k = keys.nextElement();
			if ("clientIp".equals(k))
				clientIp = opts.get("host");
			else if ("serverAddr".equals(k))
				serverAddr = opts.get("serverAddr");
			else if ("serverPort".equals(k))
				serverPort = Integer.parseInt(opts.get("http"));
			else
				severe("Unrecognized option: --" + k);
		}

		return commandLineArgs;
	}
	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		new Client(args);
	}
	
	public Client(String[] args) {
		try {
			// PropertyConfigurator.configure(getClass().getResource(loggerPropsFile));
			/*
			 * Load server properties.
			 */
			Properties props = new Properties();
			InputStream in = getClass().getResourceAsStream(clientPropsFile);
			props.load(in);
			in.close();
			clientIp = (String) props.get("client.ip");
			serverAddr = (String) props.get("server.machine");
			String serverName = (String) props.get("server.name");
			serverPort = Integer.parseInt((String) props.get("server.port"));
			
			/*
			 * Overrides from command-line
			 */
			processArgs(args);
			
			/*
			 * TODO: Get a server proxy.
			 */
			//Vyshali: server is the reference to the remote server object, for invoking methods.
			Registry registry = LocateRegistry.getRegistry(serverAddr, serverPort);
			IServerFactory serverFactory = (IServerFactory) registry.lookup(serverName);
			IServer server = serverFactory.createServer();
			//IServer server = null;
			
			/*
			 * Start CLI.  Second argument should be server proxy.
			 */
			cli(serverAddr, server);

		} catch (java.io.FileNotFoundException e) {
			log.severe("Client error: " + clientPropsFile + " file not found.");
		} catch (java.io.IOException e) {
			log.severe("Client error: IO exception.");
			e.printStackTrace();
		} catch (Exception e) {
			log.severe("Client exception:");
			e.printStackTrace();
		}

	}

	static void msg(String m) {
		System.out.print(m);
	}

	static void msgln(String m) {
		System.out.println(m);
	}

	static void err(Exception e) {
		System.err.println("Error : "+e);
		e.printStackTrace();
	}

	public static void cli(String svrHost, IServer svr) {

		// Main command-line interface loop

		try {
			InetAddress serverAddress = InetAddress.getByName(svrHost);
			Dispatch d = new Dispatch(svr, serverAddress);
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

			while (true) {
				msg("ftp> ");
				String line = in.readLine();
				String[] inputs = line.split("\\s+");
				if (inputs.length > 0) {
					String cmd = inputs[0];
					if (cmd.length()==0)
						;
					else if ("get".equals(cmd))
						d.get(inputs);
					else if ("put".equals(cmd))
						d.put(inputs);
					else if ("cd".equals(cmd))
						d.cd(inputs);
					else if ("pwd".equals(cmd))
						d.pwd(inputs);
					else if ("dir".equals(cmd))
						d.dir(inputs);
					else if ("ldir".equals(cmd))
						d.ldir(inputs);
					else if ("port".equals(cmd))
						d.port(inputs);
					else if ("pasv".equals(cmd))
						d.pasv(inputs);
					else if ("help".equals(cmd))
						d.help(inputs);
					else if ("quit".equals(cmd))
						return;
					else
						msgln("Bad input.  Type \"help\" for more information.");
				}
			}
		} catch (EOFException e) {
		} catch (UnknownHostException e) {
			err(e);
			System.exit(-1);
		} catch (IOException e) {
			err(e);
			System.exit(-1);
		}
		

	}

	public static class Dispatch {

		private IServer svr;
		
		private InetAddress serverAddress;

		Dispatch(IServer s, InetAddress sa) {
			svr = s;
			serverAddress = sa;
		}

		public void help(String[] inputs) {
			if (inputs.length == 1) {
				msgln("Commands are:");
				msgln("  get filename: download file from server");
				msgln("  put filename: upload file to server");
				msgln("  pwd: current working directory on server");
				msgln("  cd filename: change working directory on server");
				msgln("  dir: list contents of working directory on server");
				msgln("  ldir: list contents of current directory on client");
				msgln("  port: server should transfer files in active mode");
				msgln("  pasv: server should transfer files in passive mode");
				msgln("  quit: exit the client");
			}
		}

		/*
		 * ********************************************************************************************
		 * Data connection.
		 */

		enum Mode {
			NONE, PASSIVE, ACTIVE
		};

		/*
		 * Note: This refers to the mode of the SERVER.
		 */
		private Mode mode = Mode.NONE;

		/*
		 * If active mode, remember the client socket.
		 */

		private ServerSocket dataChan = null;

		/*
		 * Vyshali - Making a Server Active
		 * When the server wants to do a file transfer, it has to connect to client, 
		 * client should be listening for server connection request, 
		 * so the server should make a connection request to the client.
		 * When it connects to the client, the client will accept the connection request,
		 * We would have establised a data channel - TCP Socket, 
		 * and the server can transfer the file through the TCP Socket.
		 * Inorder to make the server active, we need to provide the server with the socket 
		 * on which we are listening for connection request. We the Client. 
		 */
		private InetSocketAddress makeActive() throws IOException {
			/*
			 * Vyshali: Inorder to make the server active, the first thing we do is to create a ServerSocket
			 * This can be considered as the Request Channels for creating data channels
			 */
			dataChan = new ServerSocket(0);
			mode = Mode.ACTIVE;
			/* 
			 * Note: this only works (for the server) if the client is not behind a NAT.
			 * Vyshali - from the ServerSocket we are going to get the socket address, 
			 * Socket address is going to be a pair of IP Address and Port Number
			 * - It should always be an Internet Socket Address
			 */
			return (InetSocketAddress) (dataChan.getLocalSocketAddress()); 
		}

		/*
		 * If passive mode, remember the server socket address.
		 */
		private InetSocketAddress serverSocket = null;

		private void makePassive(InetSocketAddress s) {
			serverSocket = s;
			mode = Mode.PASSIVE;
		}

		/*
		 * *********************************************************************************************
		 */

		private static class GetThread implements Runnable {
			/*
			 * This client-side thread runs when the server is active mode and a
			 * file download is initiated. This thread listens for a connection
			 * request from the server. The client-side server socket (...)
			 * should have been created when the port command put the server in
			 * active mode.
			 */
			private ServerSocket dataChan = null;
			private FileOutputStream file = null;

			public GetThread(ServerSocket s, FileOutputStream f) {
				dataChan = s;
				file = f;
			}

			public void run() {
				try {
					/*
					 * TODO: Complete this thread.
					 */
					// Vyshali: Reading bytes from the socket
					Socket xfer = dataChan.accept();
					InputStream is = xfer.getInputStream();
					byte[] buf = new byte[512];
					int nbytes = is.read(buf,0,512);
					while(nbytes > 0) {
						file.write(buf,0,nbytes);
						nbytes = is.read(buf,0,512);
					}
					file.close();
					is.close();
					/*
					 * End TODO
					 */
				} catch (IOException e) {
					msg("Exception: " + e);
					e.printStackTrace();
				}
			}
		}

		
		
		// Vyshali
		private static class GetThread1 implements Runnable {
			
			private ServerSocket dataChan = null;
			private FileInputStream file = null;

			public GetThread1(ServerSocket s, FileInputStream f) {
				dataChan = s;
				file = f;
			}

			public void run() {
				try {
					/*
					 * TODO: Complete this thread.
					 */
					// Vyshali: Reading bytes from the socket
					Socket xfer = dataChan.accept();
					OutputStream os = xfer.getOutputStream();
					byte[] buf = new byte[512];
					int nbytes = file.read(buf,0,512);
					while(nbytes > 0) {
						os.write(buf,0,nbytes);
						nbytes = file.read(buf,0,512);
					}
					file.close();
					os.close();
					/*
					 * End TODO
					 */
				} catch (IOException e) {
					msg("Exception: " + e);
					e.printStackTrace();
				}
			}
		}
		
		public void get(String[] inputs) {
			if (inputs.length == 2) {
				try {
					/*
					 * Client side code for the server in the passive mode. 
					 * 1. In PASSIVE mode, when we start a file transfer, the server is going to listen
					 * on a conection request channel for request from the client for a Data Channel.
					 * Once the connection is established, the server will transfer the contents of the file over the data channel.
					 * The client will then get those contents and write them to the disk in a so called file system. 
					 */
					if (mode == Mode.PASSIVE) {
						/*
						 * 2. Its no good the client trying to make a connection to the server straight away, because the server doesnt start listening.
						 * Although the connection request channel has been created, 
						 * the server is not listening on that channel for connection request, until a client initiates a file transfer by using GET/PUT.
						 * So, the client has to first of all notify the server that it wishes to do a file transfer.
						 * For eg., using GET I want to do a File Download. So the client does first of all is it isssues the GET operation 
						 * which is not to actually to transfer the file content, its just to tell the server I want to do a file download. 
						 * In Response to that, the server will start listening for a connection request from the client to establish the TCP/IP socket for data transfer.
						 * Going to the Server Side
						 */
						svr.get(inputs[1]);
						FileOutputStream f = new FileOutputStream(inputs[1]);
						Socket xfer = new Socket(serverAddress, serverSocket.getPort());
						//Vyshali: reading bytes from socket
						InputStream is = xfer.getInputStream(); //an input stream for reading bytes from this socket.
						byte[] buf = new byte[512];
						int nbytes = is.read(buf,0,512);
						while(nbytes > 0) {
							f.write(buf,0,nbytes);
							nbytes = is.read(buf,0,512);
						}
						f.close();
						is.close();
						/*
						 * TODO: connect to server socket to transfer file.
						 */
					} else if (mode == Mode.ACTIVE) {
						FileOutputStream f = new FileOutputStream(inputs[1]);
						new Thread(new GetThread(dataChan, f)).start();
						svr.get(inputs[1]);
					} else {
						msgln("GET: No mode set--use port or pasv command.");
					}
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void put(String[] inputs) {
			if (inputs.length == 2) {
				try {
					/*
					 * TODO: Finish put (both ACTIVE and PASSIVE mode supported).
					 */
					if (mode == Mode.PASSIVE) {
						svr.put(inputs[1]);
						FileInputStream f = new FileInputStream(inputs[1]);
						Socket xfer = new Socket(serverAddress, serverSocket.getPort());
						OutputStream os = xfer.getOutputStream(); 
						byte[] buf = new byte[512];
						int nbytes = f.read(buf,0,512);
						while(nbytes > 0) {
							os.write(buf,0,nbytes);
							nbytes = f.read(buf,0,512);
						}
						f.close();
						os.close();
					} else if (mode == Mode.ACTIVE) {
						FileInputStream f = new FileInputStream(inputs[1]);
						new Thread(new GetThread1(dataChan, f)).start();
						svr.put(inputs[1]);
					} else {
						msgln("PUT: No mode set--use port or pasv command.");
					}
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void cd(String[] inputs) {
			if (inputs.length == 2)
				try {
					svr.cd(inputs[1]);
					msgln("CWD: "+svr.pwd());
				} catch (Exception e) {
					err(e);
				}
		}

		public void pwd(String[] inputs) {
			if (inputs.length == 1)
				try {
					msgln("CWD: "+svr.pwd());
				} catch (Exception e) {
					err(e);
				}
		}

		public void dir(String[] inputs) {
			if (inputs.length == 1) {
				try {
					String[] fs = svr.dir();
					for (int i = 0; i < fs.length; i++) {
						msgln(fs[i]);
					}
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void pasv(String[] inputs) {
			if (inputs.length == 1) {
				try {
					/*
					 * svr.pasv() - telling the remote server to put yourself on the passive mode. 
					 * It will then return the socket address on which the client should make 
					 * connection request to the server. 
					 * In Passive mode, server listens for connection request from the client.
					 * And client needs to remember that, and thus calling makePassive Operation
					 */
					makePassive(svr.pasv());
					msgln("PASV: Server in passive mode.");
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void port(String[] inputs) {
			if (inputs.length == 1) {
				try {
					InetSocketAddress s = makeActive();
					/*
					 * We call the Server PORT operation with the socket address we got back
					 * s - is the network address of the port on which the client is waiting for 
					 * connection request from the server to establish the data channel
					 */
					svr.port(s);
					msgln("PORT: Server in active mode.");
				} catch (Exception e) {
					err(e);
				}
			}
		}

		public void ldir(String[] inputs) {
			if (inputs.length == 1) {
				String[] fs = new File(".").list();
				for (int i = 0; i < fs.length; i++) {
					msgln(fs[i]);
				}
			}
		}

	}

}
