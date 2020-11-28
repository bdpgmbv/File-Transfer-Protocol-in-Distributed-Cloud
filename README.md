* The communication between client and server is handled by using two intermediate objects: Stub object (on client side) and Skeleton object (on server side).
* The stub object on the client machine builds an information block and sends this information to the server. 
* The block consists of - 
1. An identifier of the remote object to be used..
2. Method name which is to be invoked..
3. Parameters to the remote JVM..
