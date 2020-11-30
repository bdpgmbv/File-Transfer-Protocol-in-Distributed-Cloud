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
