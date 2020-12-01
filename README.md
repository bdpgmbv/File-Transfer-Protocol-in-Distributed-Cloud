##### Quick Info: 
* FTP Server can run in active/passive mode.
###### Server in Active Mode
* Client uses the PORT command to put the FTP Server into active mode. Eg, Server, when it wants to download a file, will connect to a client socket to eastablish a channel and then download the file through that channel. 
* The Client tells the server to go into active mode with the PORT command, and it specifies the Internet Socket Address which the server will then use to connect to the client socket. 
###### Server in Passive Mode
* When the server runs in passive mode it waits for the client to connect with on its socket inorder to establish a data channel for communication, given a file transfer.
* Client can put the server into passive mode by executing the PASV command and the server returns the socket on which it is listening for the client connection request. 


#### Step 1: IServer.java
* An Interface for a remote RMI server object (IServer Interface extends the Remote Interface). 
* Below are the Operations that are available to me in the remote FTP server object (I define these operations in the IServer.java Interface).
```
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

##### Notes - Stateful Client Server Interaction 
* When you are interacting with the server, you are interacting with a RMI Object that implements this interface.
* One thing we cannot do: We cannot have several clients interacting with the same server object. The problem is the server is maintaining the internal state about the client, In Particular, its remembering the current directory where the client is on the remote server. 
* If we have two clients interacting with the object simultaneously, obviously things will get confusing as they keep changing directories, and obviously there are privacy concerns as well.

#### Step 2: Create New Server Object for each Client
* So, what do we do is whenever the client connects, we create a new server object just for that client. For each client session there is a seperate server object handling that client session. 
* So, we have a way for creating a new server object everytime the client comes in. 
* Here, I follow a standard design pattern for these RMI systems is for the clients to actually get access not to a server object but to a factory object that will create server objects. 
```
public interface IServerFactory extends Remote {
	public IServer createServer() throws RemoteException;
}

```
###### Protocol for Client connecting to the FTP server: First we go to a named service, and we will use the RMI registry service for the named service. Thus we first go to the Registry Service, and from the registry service we do a lookup on the name of the server, getting back ServerFactory object. And now we have one method that can be called on the ServerFactory Object - createServer() which will create a new server object. 
