* The communication between client and server is handled by using two intermediate objects: Stub object (on client side) and Skeleton object (on server side).

#### Stub Object
* The stub object on the client machine builds an information block and sends this information to the server. 
* The block consists of - 
1. An identifier of the remote object to be used..
2. Method name which is to be invoked..
3. Parameters to the remote JVM..

#### Skeleton Object
* The skeleton object passes the request from the stub object to the remote object.
* It performs following tasks
1. It calls the desired method on the real object present on the server.
2. It forwards the parameters received from the stub object to the method.

### Step 1: Defining a remote interface
* The first thing to do is to create an interface which will provide the description of the methods that can be invoked by remote clients. 
* This interface should extend the Remote interface and the method prototype within the interface should throw the RemoteException.

```
IServer.java
public interface IServer extends Remote {
	public void get(String f) throws IOException, FileNotFoundException, RemoteException;
	public void put(String f) throws IOException, FileNotFoundException, RemoteException;
	public String pwd() throws RemoteException;
	public void cd(String d) throws IOException, RemoteException;
	public String[] dir() throws RemoteException;
	public void port(InetSocketAddress s) throws RemoteException;
	public InetSocketAddress pasv() throws IOException, RemoteException;
}

IServerFactory.java
public interface IServerFactory extends Remote {
	public IServer createServer() throws RemoteException;
}
```

### Step 2: Implementing the remote interface
* The next step is to implement the remote interface. To implement the remote interface, the class should extend to UnicastRemoteObject class of java.rmi package. 
* Also, a default constructor needs to be created to throw the java.rmi.RemoteException from its parent constructor in class.

ServerFactory.java - Class Variables: Host(IP Address), Port, Path Prefix = '/'. Functions: 3 argument constructor (ServerFactory(InetAddress h, int port, String p) ) and Function to create a server proxy (IServer createServer()).

```
if (mode == Mode.ACTIVE) {
	FileOutputStream f = new FileOutputStream(inputs[1]); // inputs[1] - 1.txt
	new Thread(new GetThread(dataChan, f)).start();
	svr.get(inputs[1]);
}
```

```
private static class GetThread implements Runnable {
	private ServerSocket dataChan = null;
	private FileOutputStream file = null;

	public GetThread(ServerSocket s, FileOutputStream f) {
		dataChan = s;
		file = f;
	}

	public void run() {
		try {
			Socket xfer = dataChan.accept(); // Listens for a connection to be made to this socket and accepts it. Returns the new Socket
			InputStream is = new FileInputStream(file); // Creates a FileInputStream (A FileInputStream obtains input bytes from a file in a file system) by opening a connection to an actual file, the file named by the File object file in the file system.
			OutputStream os = xfer.getOutputStream(); // an output stream for writing bytes to this socket.
			byte[] buf = new byte[512];
			int nbytes = is.read(buf,0,512); Reads up to len(512) bytes of data from the input stream into an array of bytes. Parameters: buf - The buffer into which the data is read, 0 - the start offset in array buf at which the data is written, 512 - the maximum number of bytes to read. Returns: the total number of bytes read into the buffer.
			while(nbytes > 0){
				os.write(buf,0,nbytes);
				nbytes = is.read(buf,0,512);
			}
			is.close();
			os.close();
		} catch (IOException e) {
			msg("Exception: " + e);
			e.printStackTrace();
		}
	}
}
```

```
if (mode == Mode.ACTIVE) {
	Socket xfer = new Socket (clientSocket.getAddress(), clientSocket.getPort()); // clientSocket.getAddress() - 0.0.0.0, clientSocket.getPort() - 64181, Socket(InetAddress address, int port) - Creates a stream socket and connects it to the specified port number at the specified IP address.
	InputStream in = new FileInputStream(path()+file); // path() - /Users/VyshaliPrabananthLal/tmp/cs549/ftp-test/root
	OutputStream os = xfer.getOutputStream();
	byte[] buf = new byte[512];
	int nbytes = is.read(buf,0,512);
	while(nbytes > 0){
		os.write(buf,0,nbytes);
		nbytes = is.read(buf,0,512);
	}
	is.close();
	os.close();
}
```
