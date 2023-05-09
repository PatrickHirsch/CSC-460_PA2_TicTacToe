// Patrick Hirsch    HirschP2@nku.edu
// CSC 460-001 | Operating Systems
// Program #2, TicTacToe Client

import java.io.*;
import java.net.*;
 
public class Client
{
	//CONFIGURATION VARIABLES
	private static int
		PORT=9877;
	private static String
		HOST="localhost";
		//HOST="10.15.32.76";
	
	
	//GLOBAL VARIABLES
	private static char board[][]=	// 2D Array of chars representing the board maintained on 
	{	{' ',' ',' ',' '},          //	Client-side to print.  Unplayed spaces are represented by 
		{' ',' ',' ',' '},          //	spaces (' '), Server-claimed spaces are represented by 'X's,
		{' ',' ',' ',' '},          //	and Client-claimed spaces are represented by 'O's.
		{' ',' ',' ',' '}};
	private static String hr="|-------------+-------------|\n\n";	// Simple ASCII divider String
	
	
	public static void main(String[] args)
	{	if (args.length>0) HOST=args[0];					// Override HOST if custom value passed
		if (args.length>1) PORT=Integer.parseInt(args[1]);	// Override PORT if custom value passed
		
		try
		(	//Connect to Server
			Socket socket = new Socket(HOST,PORT);
			
			//Set-up server/client communication
			PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
		)
		{	//Inform player a connection has been made
			System.out.println("Connected to "+HOST+":"+PORT+"!");
			
			
			// Special handling of first Server msg, to determine if Server or Client plays first.
			String userInput=in.readLine();
			if(userInput.equals("Client"))
			{	System.out.println("You play first!");
			}else
			{	System.out.println("Server plays first!");
				serverMove(userInput);
			}
			
			// Infinite loop to run until Socket closes, forever prompting the user for a space, 
			//	validating legality of selected move, sending msg to Server, and interpreting
			//	server message.
			while(true)
			{	String usrMove="MOVE ";		// Begin constructing msg to send to Server
				int row=-1,					// Initialize selected row and column to invalid -1
					col=-1;
				boolean moveInvalid=false;	// Set moveInvalid to false, if toggled, reprompt player
				
				// Prompt user for column to play
				System.out.println(stringifyBoard(row,col));
				System.out.print("Select Column <1,2,3,4>: ");
				while(col==-1 && ((userInput = stdIn.readLine()) != null))
				{	try
					{	col=Integer.parseInt(userInput);
					}catch(Exception e)
					{	col=-1;}
					
					if(col<1||col>4)
					{	col=-1;
						System.out.print(hr+"Invalid input, try again.\nSelect Column <1,2,3,4>: ");
					}
					else col--;
				}
				
				// Prompt user for row to play
				System.out.println(stringifyBoard(row,col));
				System.out.print("Select Row <1,2,3,4>: ");
				while(row==-1 && ((userInput = stdIn.readLine()) != null))
				{	try
					{	row=Integer.parseInt(userInput);
					}catch(Exception e)
					{	row=-1;}
					
					if(row<1||row>4)
					{	row=-1;
						System.out.print(hr+"Invalid input, try again.\nSelect Row <1,2,3,4>: ");
					}
					else row--;
				}
				
				// Confirm move is legal, if not, inform user and start new while loop iteration
				if(!isValidMove(row,col))	
				{	System.out.println(hr+"Space is not open. Please select a different space.");
					continue;
				}
				
				// Confirm space user wishes to play, if user wishes to choose new space, inform user 
				//	and start new while loop iteration/
				System.out.println(stringifyBoard(row,col));
				System.out.print("Push Enter to confirm, enter any character to cancel: ");
				while((usrMove.equals("MOVE ")&&!moveInvalid) && (userInput = stdIn.readLine()) != null)
				{	if(!(userInput.toUpperCase().equals("")))
						moveInvalid=true;
					else	usrMove+=row+" "+col;
				}if(moveInvalid)
				{	System.out.println(hr+"Space not played.  Select a different space.");
					continue;
				}
				
				// With move confirmed and validated, update local board, send to Server, print new 
				//	board to client.
				updateLocalBoard('O',row,col);
				out.println(usrMove);
				System.out.println(stringifyBoard());
				
				// Print <hr>, receive Server's move, and interpret received move.
				System.out.println(hr);
				String gameState=serverMove(in.readLine());
				
				// After interpreting Server's move, add a bit of suspense and panache by adding a 
				//	delay before the server's move is revealed.
				if(gameState.equals("MOVE"))
				{	System.out.print("Server is thinking");
					for(int i=0;i<4;i++)
					{	try {Thread.sleep(750);}
						catch(Exception e){}
						if(i!=3) System.out.print('.');
					}	System.out.println();
					continue;
				}
				else if(gameState.equals("TIE")) System.out.println(stringifyBoard());
				
				System.out.println(handleGameOver(gameState));
				
				// After game ends, close connection, end loop, end program.
				socket.close();
				return;
			}
		}catch (UnknownHostException e)
		{	System.err.println("Unable to connect to server " + HOST);
			System.exit(1);
		}catch (IOException e)
		{	System.err.println("Couldn't get I/O for the connection to " +HOST);
			System.exit(1);
		}
	}
	
	// To be run following a message being received from the Server.  Interprets the Server's msg to 
	//	update the local board and return a "gameState".  The gameState returned is the 4ᵗʰ value 
	//	passed in the Server's message, first value of "MOVE" if no 4ᵗʰ value present.
	private static String serverMove(String msg)
	{	String[] ray=msg.split(" ");
		if((ray.length>=3)&&(ray[0].equals("MOVE")))
		{	int r=Integer.parseInt(ray[1]);
			int c=Integer.parseInt(ray[2]);
			updateLocalBoard('x',r,c);
		}
		if(ray.length>3) return ray[3];
		return ray[0];
	}
	
	// Takes a row and column in the board[][] and returns true if it is a valid move, that is in 
	//	bounds and empty (containing a ' ').
	private static boolean isValidMove(int r,int c)
	{	try
		{	if(board[r][c]==' ') return true;}
		catch(ArrayIndexOutOfBoundsException e)
		{	}// If out of bounds, carry on and return false, this is not a valid move.
		return false;
	}
	
	// After a move is made, update the locally maintained board to be shown to the user.
	private static boolean updateLocalBoard(char player,int r,int c)
	{	if(isValidMove(r,c))
		{	board[r][c]=player;
			return true;
		}else return false;
	}
	
	// Convert board to a human-readable String using a simple ASCII art grid layout.
	//	Optional parameter are taken to show an arrow at a specified row &|| column.
	private static String stringifyBoard()
	{	return stringifyBoard(-1,-1);
	}
	private static String stringifyBoard(int row, int col)
	{	String ret="";
		for(int r=0;r<4;r++)
		{	ret+="  ";
			for(int c=0;c<4;c++)
			{	ret+=board[r][c];
				if(c!=3) ret+=" | ";
			}
			if(row==r)	ret+=" <--";
			if(r!=3) ret+=" \n ---+---+---+--- \n";
			else ret+="\n";
		}
		if(col!=-1)
		{	for(int i=0;i<col;i++) ret+="    ";
			ret+="  ^\n";
			for(int i=0;i<col;i++) ret+="    ";
			ret+="  |";
		}else ret+="\n";
		return "\n"+ret;
	}
	
	// To be run at Game Over.  Takes a gameState string indicating how a game won ("WIN"/"LOSS"/
	//	"TIE") and returns an appropriate Game Over message to print to player.
	private static String handleGameOver(String state)
	{	switch(state)
		{	case "WIN":
				return "Congrats!  You win!";
			case "LOSS":
				System.out.println(stringifyBoard());
				return "Computer won. :(";
			case "TIE":
				return "The cat has won.";
		}
		return "GAME OVER";
	}
}
