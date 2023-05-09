// Patrick Hirsch    HirschP2@nku.edu
// CSC 460-001 | Operating Systems
// Program #2, TicTacToe Dispatcher

import java.io.*;
import java.net.*;

public class Dispatcher
{	//CONFIGURATION VARIABLES
	private static int
		PORT=9877,
		MAXCONNECTIONS=0;

	public static void main(String[] args)
	{	if (args.length!=0) MAXCONNECTIONS=Integer.parseInt(args[0]);
		System.out.println("Listening on port "+PORT+"...");
		int concurent=0;
		try
		{	ServerSocket listener = new ServerSocket(PORT);
			Socket client;
			while((MAXCONNECTIONS==0)||(concurent++<MAXCONNECTIONS))//
			{	client=listener.accept();							// accept client connection
				System.out.println("**Received client request.**");	//
				ServerThread conn_c=new ServerThread(client);		// pass client to thread
				Thread t=new Thread(conn_c);						// create thread object & start it up
				t.start();											//
			}
		}catch(IOException ioe)
		{	System.out.println("**Client disconnected unexpectedly.**");
		}
	}
}
