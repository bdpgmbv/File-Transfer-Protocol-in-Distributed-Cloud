package edu.stevens.cs549.ftpserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Stack;
import java.util.logging.Logger;

import edu.stevens.cs549.ftpinterface.IServer;

/**
 *
 * @author dduggan
 */
public class Server extends UnicastRemoteObject
        implements IServer {
	
	static final long serialVersionUID = 0L;
	
	public static Logger log = Logger.getLogger("edu.stevens.cs.cs549.ftpserver");
    
	/*
	 * For multi-homed hosts, must specify IP address on which to 
	 * bind a server socket for file transfers.  See the constructor
	 * for ServerSocket that allows an explicit IP address as one
	 * of its arguments.
	 */
	private InetAddress host;
	
	final static int backlog = 5;
	
	/*
	 *********************************************************************************************
	 * Current working directory.
	 */
    static final int MAX_PATH_LEN = 1024;
    private Stack<String> cwd = new Stack<String>();
    
    /*
     *********************************************************************************************
     * Data connection.
     */
   
    enum Mode { NONE, PASSIVE, ACTIVE };
    
    //Vyshali: Recording the state of the server and not of the client
    private Mode mode = Mode.NONE;
    
    /*
     * If passive mode, remember the server socket.
     */
    
    private ServerSocket dataChan = null;
    
    private InetSocketAddress makePassive () throws IOException {
    	/*
    	 * Vyshali - Server now needs to create a server socket on which it will listen for
    	 * request to establish a data channel. 
    	 */
    	dataChan = new ServerSocket(0, backlog, host);
    	mode = Mode.PASSIVE;
    	return (InetSocketAddress)(dataChan.getLocalSocketAddress());
    }
    
    /*
     * If active mode, remember the client socket address.
     */
    private InetSocketAddress clientSocket = null;
    
    private void makeActive (InetSocketAddress s) {
    	clientSocket = s;
    	mode = Mode.ACTIVE;
    }
    
    /*
     **********************************************************************************************
     */
            
    /*
     * The server can be initialized to only provide subdirectories
     * of a directory specified at start-up.
     */
    private final String pathPrefix;

    public Server(InetAddress host, int port, String prefix) throws RemoteException {
    	super(port);
    	this.host = host;
    	this.pathPrefix = prefix + "/";
        log.info("A client has bound to a server instance.");
    }
    
    public Server(InetAddress host, int port) throws RemoteException {
        this(host, port, "/");
    }
    
    private boolean valid (String s) {
        // File names should not contain "/".
        return (s.indexOf('/')<0);
    }
    
    private static class GetThread implements Runnable {
    	private ServerSocket dataChan = null;
    	private FileInputStream file = null;
    	public GetThread (ServerSocket s, FileInputStream f) { dataChan = s; file = f; }
    	/*
    	 * This thread takes two piece of information - ServerSocket in which you should listen for data channel connection request from the client. 
    	 * Other piece of information it needs is the file that needs to be downloaded to the client. 
    	 * This is saved as a private field as type `FileInputStream`, 
    	 * actually what happens is we open the file for input, this is the file on the servers local disk, that we open for input, and we pass that to this lightweight thread.
    	 */
    	public void run () {
    		/*
    		 * TODO: Process a client request to transfer a file.
    		 */
    		// Vyshali: Write the bytes to the socket
    		try {
				Socket xfer = dataChan.accept(); // Listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made. Returns the new Socket
				OutputStream os = xfer.getOutputStream(); // an output stream for writing bytes to this socket.
				byte[] buf = new byte[512];
				int nbytes = file.read(buf,0,512);
				while(nbytes>0) {
					os.write(buf, 0, nbytes);
					nbytes = file.read(buf, 0, nbytes);
				}
				file.close();
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    private static class GetThread1 implements Runnable {
    	private ServerSocket dataChan = null;
    	private FileOutputStream file = null;
    	public GetThread1 (ServerSocket s, FileOutputStream f) { dataChan = s; file = f; }
    
    	public void run () {
    		try {
				Socket xfer = dataChan.accept();
				InputStream os = xfer.getInputStream(); 
				byte[] buf = new byte[512];
				int nbytes = os.read(buf,0,512);
				while(nbytes>0) {
					file.write(buf, 0, nbytes);
					nbytes = os.read(buf, 0, nbytes);
				}
				file.close();
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    public void get (String file) throws IOException, FileNotFoundException, RemoteException {
        if (!valid(file)) {
            throw new IOException("Bad file name: " + file);
        } else if (mode == Mode.ACTIVE) {
        	Socket xfer = new Socket (clientSocket.getAddress(), clientSocket.getPort());
        	/*
        	 * TODO: connect to client socket to transfer file.
        	 */
        	//Vyshali: Writing bytes to the socket
        	InputStream in = new FileInputStream(path()+file);
        	OutputStream os = xfer.getOutputStream();
        	byte[] buf = new byte[512];
        	int nbytes = in.read(buf,0,512);
        	while(nbytes > 0) {
        		os.write(buf,0,nbytes);
        		nbytes = in.read(buf,0,512);
        	}
        	in.close();
        	os.close();
        	/*
			 * End TODO.
			 */
        } else if (mode == Mode.PASSIVE) {
        	/*
        	 * 3. What the server wants to do is, the whole point of the GET operation is really 
        	 * just to tell the server start listening for a connection request for a data channel. 
        	 * And when you get the connection request, establish the data channel and transfer the contents of this file.
        	 * So what the server wants to do is, return back from clients letting the client know it is now listening for connection request.
        	 * The problem is, when the client returns back to the server, its finished, its done.
        	 * There is no way round to listen for these connection requests from the client, 
        	 */
            FileInputStream f = new FileInputStream(path()+file);
            /*
             * 4. Fork a new light weight thread on the server side, that after the server returns back to the client
             * this thread will continue to run and it will wait for a connection request to establish a data channel from the client. 
             */
            new Thread (new GetThread(dataChan, f)).start();
        }
    }
    
    public void put (String file) throws IOException, FileNotFoundException, RemoteException {
    	/*
    	 * TODO: Finish put (both ACTIVE and PASSIVE).
    	 */
    	if (!valid(file)) {
            throw new IOException("Bad file name: " + file);
        } else if (mode == Mode.ACTIVE) {
        	Socket xfer = new Socket (clientSocket.getAddress(), clientSocket.getPort());
        	OutputStream os = new FileOutputStream(path()+file);
        	InputStream is = xfer.getInputStream();
        	byte[] buf = new byte[512];
        	int nbytes = is.read(buf,0,512);
        	while(nbytes > 0) {
        		os.write(buf,0,nbytes);
        		nbytes = is.read(buf,0,512);
        	}
        	is.close();
        	os.close();
        } else if (mode == Mode.PASSIVE) {
            FileOutputStream f = new FileOutputStream(path()+file);
            new Thread (new GetThread1(dataChan, f)).start();
        }
    }
    
    public String[] dir () throws RemoteException {
        // List the contents of the current directory.
        return new File(path()).list();
    }

	public void cd(String dir) throws IOException, RemoteException {
		// Change current working directory (".." is parent directory)
		if (!valid(dir)) {
			throw new IOException("Bad file name: " + dir);
		} else {
			if ("..".equals(dir)) {
				if (cwd.size() > 0)
					cwd.pop();
				else
					throw new IOException("Already in root directory!");
			} else if (".".equals(dir)) {
				;
			} else {
				File f = new File(path());
				if (!f.exists())
					throw new IOException("Directory does not exist: " + dir);
				else if (!f.isDirectory())
					throw new IOException("Not a directory: " + dir);
				else
					cwd.push(dir);
			}
		}
	}

    public String pwd () throws RemoteException {
        // List the current working directory.
        String p = "/";
        for (Enumeration<String> e = cwd.elements(); e.hasMoreElements(); ) {
            p = p + e.nextElement() + "/";
        }
        return p;
    }
    
    private String path () throws RemoteException {
    	return pathPrefix+pwd();
    }
    
    public void port (InetSocketAddress s) {
    	makeActive(s);
    }
    
    public InetSocketAddress pasv () throws IOException {
    	return makePassive();
    }

}
