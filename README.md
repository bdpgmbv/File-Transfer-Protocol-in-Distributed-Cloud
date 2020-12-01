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

### Step 4: How we do File Transfer
GET operation Client Side: Passive Mode  - For doing a file download there are 2 cases, whether the server is in Passive Mode or Active Mode
* In Passive Mode when we start a file transfer, the server is going to listen on a Connection Request Channel for a request from client for a Data Channel. Once that connection is establised, the server will transfer the contents of the file over that data channel. Client will then get those contents and write them to a disk in a so called file system.
`NOTE: its no good the client trying to make a connection to the server straightaway because the server doesnt start listening, although the connection request channel has been created, the server is not listening on that channel for connection request until a client initiates a file transfer by using GET/PUT`
* Client has to first of all notify the server that it wishes to do a file transfer, for eg - Using GET I want to do a file download. 
* The client does first of all is it issues a GET operation which is not to actually transfer file content, its just to tell the server I want to do a file Download.
* In response to that the server will start listening for a connection request from the client to establish a TCP/IP socket for Data transfer.

GET operation Server Side - The whole point about this GET operation is really just to tell the server start listening for a connection request for a Data Channel and when you get the connection request, establish the Data Channel and then transfer the contents of this file. 
*
