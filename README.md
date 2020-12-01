```
Quick Info:
* FTP Server can run in active/passive mode.
Server in Active Mode
* Client uses the PORT command to put the FTP Server into active mode. Eg, Server, when it wants to download a file, will connect to a client socket to eastablish a channel and then download the file through that channel. 
* The Client tells the server to go into active mode with the PORT command, and it specifies the Internet Socket Address which the server will then use to connect to the client socket. 
Server in Passive Mode
* When the server runs in passive mode it waits for the client to connect with on its socket inorder to establish a data channel for communication, given a file transfer.
* Client can put the server into passive mode by executing the PASV command and the server returns the socket on which it is listening for the client connection request.
```


### Step 1: Interface for a remote RMI server object
* IServer Interface extends the Remote Interface.
* Below are the Operations that are available to me in the remote FTP server object (I define these operations in the IServer.java Interface).
```
IServer.java
public interface IServer extends Remote {
	
	public void get(String f) throws IOException, FileNotFoundException,
			RemoteException;
	public void put(String f) throws IOException, FileNotFoundException,
			RemoteException;
	public String pwd() throws RemoteException; 
	public void cd(String d) throws IOException, RemoteException;
	public String[] dir() throws RemoteException; 
	public void port(InetSocketAddress s) throws RemoteException;
	public InetSocketAddress pasv() throws IOException, RemoteException;
}
```
* Note: Configured all of the methods to throw RemoteException, because network failure can happen on anyone of these methods. 

`Notes - Stateful Client Server Interaction` 
* When you are interacting with the server, you are interacting with a RMI Object that implements this interface.
* One thing we cannot do: We cannot have several clients interacting with the same server object. The problem is the server is maintaining the internal state about the client, In Particular, its remembering the current directory where the client is on the remote server. 
* If we have two clients interacting with the object simultaneously, obviously things will get confusing as they keep changing directories, and obviously there are privacy concerns as well.


### Step 2: Create New Server Object for each Client
* So, what do we do is whenever the client connects, we create a new server object just for that client. For each client session there is a seperate server object handling that client session. 
* So, we have a way for creating a new server object everytime the client comes in. 
* Here, I follow a standard design pattern for these RMI systems is for the clients to actually get access not to a server object but to a factory object that will create server objects. 
```
IServerFactory.java
public interface IServerFactory extends Remote {
	public IServer createServer() throws RemoteException;
}

```
`Protocol for Client connecting to the FTP server: First we go to a named service, and we will use the RMI registry service for the named service. Thus we first go to the Registry Service, and from the registry service we do a lookup on the name of the server, getting back ServerFactory object. And now we have one method that can be called on the ServerFactory Object - createServer() which will create a new server object.`


### Step 3: Creating Server
```
public class ServerFactory extends UnicastRemoteObject implements
		IServerFactory {
	private String pathPrefix = "/";
	private InetAddress host; //Specify host (IP address) for multi-homed hosts.
	private int serverPort; //Specify port of server for allowing access through a firewall.
	static final long serialVersionUID = 0L;

	public ServerFactory(InetAddress h, int port, String p) throws RemoteException {
		super(port);
		this.host = h;
		this.serverPort = port;
		this.pathPrefix = p;
	}

	public IServer createServer() throws RemoteException {
		return new Server(host, serverPort, pathPrefix);
	}
}
```
* @Params: pathPrefix: Registry has Internal State, path in the file system to which you are allowing clients access on the server file system. 

`All RMI Server Classes extends UnicastRemoteObject - it has all the functionalities and the hidden internal state that you need for RMI Server Object. Here we have 2 RMI Server Classes - ServerFactory.java, Server.java`

## Passive Mode

### Step 4: How we do File Transfer
`GET operation Client Side`: For doing a file download there are 2 cases, whether the server is in Passive Mode or Active Mode
* In Passive Mode when we start a file transfer, the server is going to listen on a Connection Request Channel for a request from client for a Data Channel. Once that connection is establised, the server will transfer the contents of the file over that data channel. Client will then get those contents and write them to a disk in a so called file system.

`NOTE: its no good the client trying to make a connection to the server straightaway because the server doesnt start listening, although the connection request channel has been created, the server is not listening on that channel for connection request until a client initiates a file transfer by using GET/PUT`
* Client has to first of all notify the server that it wishes to do a file transfer, for eg - Using GET I want to do a file download. 
* The client does first of all is it issues a GET operation which is not to actually transfer file content, its just to tell the server I want to do a file Download.
* In response to that the server will start listening for a connection request from the client to establish a TCP/IP socket for Data transfer.
```
svr.get(inputs[1]);
```

`GET operation Server Side`: The whole point about this GET operation is really just to tell the server start listening for a connection request for a Data Channel and when you get the connection request, establish the Data Channel and then transfer the contents of this file. 
* So what the server wants to do is return back from the client, letting the client know that it is listening for connection request. 
* The problem is when the client returns back to the server its finished, its done, so there is nothing left around to listen for these connection request from the client. Before the server returns, it needs to leave somthing behind that will for connection request from the client.
* So what it has to do is it has to fork a `new Thread` of control or a lightweight process, this is basically starting up a execution, very lightweight execution of the same program - actually executing a specific piece of code that will hang around after the server returns back to the client and it will listen for the connection request from the client. 
```
FileInputStream f = new FileInputStream(path()+file);
new Thread (new GetThread(dataChan, f)).start();
```

### Step 5: What does the Thread do ?
* Its going to wait for the client request, a data channel connection, once the data channel is established, it will then transfer the contents of the file to the client. 
* This thread takes two piece of information - ServerSocket in which you should listen for data channel connection request from the client. Other piece of information it needs is the file that needs to be downloaded to the client. This is saved as a private field as type `FileInputStream`, actually what happens is we open the file for input, this is the file on the servers local disk, that we open for input, and we pass that to this lightweight thread. 
* So once we establish a data connection with the client in response to the client request, we will then ship the bytes for the file contents along that channel downloaded to the client. WHEN THE SERVER IS IN PASSIVE MODE, WHEN IT GETS THE GET REQUEST, IT STARTS THAT THREAD, THAT THREAD IS NOW LISTENING FOR CONNECTION REQUEST FROM CLIENT.

```
private static class GetThread implements Runnable {
    	private ServerSocket dataChan = null;
    	private FileInputStream file = null;
    	public GetThread (ServerSocket s, FileInputStream f) { dataChan = s; file = f; }
    	public void run () {
    	}
    }
```

`Back to client side`: The client is done with the server.get() operation, start the server waiting for connection request. We are going to do - write the contents of the remote file to a file on the clients local file system, So we have to open up this output file `FileOutputStream`, creating a output file on the clients file system, and now we have to make a connection request to the server to establish a data channel 
```
Socket xfer = new Socket(serverAddress, serverSocket.getPort());
```
This piece of code actually establishes a socket connection to the server for the data channel. Once we established that data channel, we then trasnfer the contents of the file.
```
```

### Difficulties Faced During Development
GETTING THE SERVER TO LAUNCH A THREAD BEFORE RETURNS TO THE CLIENT, AND THAT THREAD WAITS FOR CONNECTION REQUEST FROM THE CLIENT. 

## Active Mode
client side :
* What happens if the server is in active mode? When we do a file transfer, the server is going to have to connect to the client to establish a data channel.
* Server makes the conection request instead of the client, client has to listen and wait for the connection request from the server.
* In active mode, we will contact a server saying, i want to do a file transfer, connect back to me to establish a data channel, and then transfer the file. 
* So before I do that I need to first of all establish a thread, that will then listen for the servers connection request. 
* Why do I have to fork a thread to do this, well I will need to have somebody listening at the clients side when i do a get operation.
* So the server as soon as it gets the GET invocation, as part of executing GET, it will then try to establish a connection to the client, so the client needs to have somebody else back at home base listening for that connection request. So before i talk to the server, i fork a thread. 
* In active mode, we open a file in the local file system to be written as we get the data from the server. We fork a thread, this thread is going to listen for connection request from the server, when it gets connection request from the server, it will start reading the file contents off of that data channel and write it out to local disk in its file f. 
* Thread - client, accepting the connection request from the server and then downloading the contents of the file using read operations on the underlying input stream, reading contents of the file and writing it out to a file.  
* Now we can go ahead and tell the server, Ok now you need to make a connection request to me, and start transfering contents of the file. 
Server Side:
* If the server is in active mode, server makes the connection request to the client using the client socket address that was provided as part of the PORT operation, which we should have used before this, to put the server in active mode. 
* So in Active Mode the Server knows the client socket address to connect to. Here is where it establishes that connection.
```
Socket xfer = new Socket (clientSocket.getAddress(), clientSocket.getPort());
```
* Once this is done it opens the file for input on a local server disk and starts transferring the content from the input file to the output stream that underlies this socket connection. 

