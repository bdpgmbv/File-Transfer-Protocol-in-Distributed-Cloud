* The communication between client and server is handled by using two intermediate objects: Stub object (on client side) and Skeleton object (on server side).
* The stub object on the client machine builds an information block and sends this information to the server. 
* The block consists of - 
...An identifier of the remote object to be used..
...Method name which is to be invoked..
...Parameters to the remote JVM..
