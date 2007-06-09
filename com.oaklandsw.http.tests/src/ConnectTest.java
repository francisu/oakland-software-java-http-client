

import java.net.Socket;

public class ConnectTest
{

    public static final void main(String[] args)
        throws Exception
    {
        if (args.length < 2)
        {
            System.err.println("usage: java ConnectTest <hostName> <portNumber>");
            System.exit(-1);
        }
        int port = Integer.parseInt(args[1]);

        Socket sock = new Socket(args[0], port);
        System.out.println("connected ok " + sock);
    }
        
}

 
