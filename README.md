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
