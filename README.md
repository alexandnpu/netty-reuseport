This repo mainly based on https://github.com/mikepii/netty-reuseport-demo

But the logic is different.

The project aims at keep the already-established tcp connection to work, but
no longer accept new ones.

According to linux implementation of SO_REUSEPORT, if mulitple threads/processes
are listening to the same port, it will does a round-robin balance between
these threads/processes on the incoming connection requests. So if you start 
more than one instance of this project, the instance which is fired with 
signal SIGBUS (`kill -7`) will no longer have the chance to get new connection,
as the kernel will not route new connection requests to it.
