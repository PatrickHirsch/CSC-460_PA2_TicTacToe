// Patrick Hirsch    HirschP2@nku.edu
// CSC 460-001 | Operating Systems
// Program #2, TicTacToe ServerThread

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.util.LinkedList;

class ServerThread implements Runnable
{	
	private Socket client;
	private String line, msg;
	private Random random=new Random();	//Random number generator.
	private char board[][]=		// 2D Array of chars representing the board maintained on 
	{	{' ',' ',' ',' '},		//	Server-side.  Unplayed spaces are represented by 
		{' ',' ',' ',' '},		//	spaces (' '), Server-claimed spaces are represented by 'X's,
		{' ',' ',' ',' '},		//	and Client-claimed spaces are represented by 'O's.
		{' ',' ',' ',' '}	};
	// Rather than guessing randomly until it finds an open space on the board, ServerTread
	//	maintains a list of valid moves it can pick from for better time complexity.  Additionally, 
	//	the length of the list decreases each move with a once-valid space being played allowing 
	//	the the size to be used to calculate turn/round number.  A LinkedList is chosen as an 
	//	element is removed from somewhere in the list every round, as where a random element is 
	//	accessed by index only at server's turn and worse case decreases with the list's shrinkage.
	private LinkedList<Integer> validMoves
		=new LinkedList<>(Arrays.asList(00,01,02,03,10,11,12,13,20,21,22,23,30,31,32,33));	
	
	
// constructor for thread
	ServerThread(Socket c){this.client=c;}

	public void run()
	{	msg="";
		try(BufferedReader in = new BufferedReader (new InputStreamReader(client.getInputStream()));
			PrintStream out = new PrintStream(client.getOutputStream());
		)
		{	// Upon thread's creation, start game by picking first player, 50/50, chance between 
			//	Server & Client.  If the Client is chosen, msg is set to "Client" and sent to
			//	Client to inform it of the choice.  If the Server is chosen, it makes it's first 
			//	move and sends Client the appropriate message.
			System.out.println("**Game started with client.**");
			if(random.nextInt()%2==0)	msg="Client";
			else						msg=serverMove();
			System.out.println("SERVER: "+msg);
			out.println(msg);
			
			// Until connection to client is lost, continuously interpret Client's message as move 
			//	and either return proper end of game message or follow with server's move and
			//	return appropriate message to communicate it.
			while((line=in.readLine())!=null)
			{	msg=line;
				System.out.println("CLIENT: "+line);
				boolean clientWin=parseMove(line);
				
				if(validMoves.size()==0)msg="MOVE -1 -1 TIE";
				else if(clientWin)		msg="MOVE -1 -1 WIN";
				else					msg=serverMove();
				
				System.out.println("SERVER: "+msg);
				out.println(msg);
			}
		}catch (IOException ioe)
		{	System.out.println("**Client disconnected unexpectedly.**");
		}finally
		{	try{client.close();}
			catch(IOException e){}
			System.out.println("**Connection to client closed.**\n\n");
		}
	}	
	
	
	
		
// General Gameplay Functionality /////////////////////////////////////////////////////////////////
	
	// Function run when its the Server's turn.  Picks a valid spot at random from the validMoves 
	//	list, updates server-side records, checks if it has won (or game ended in a tie) and 
	//	returns the appropriate message to send to Client.
	private String serverMove()
	{	String msg="MOVE ";		// Begin constructing msg to client with MOVE command
		
		// Pick a random space from list of validMoves, parse it to a row/column value, append msg
		int i=validMoves.get(random.nextInt(validMoves.size()));
		int r=i/10;
		int c=i%10;
		msg+=r+" "+c;
		
		// Update the server-side board and validMoves list, check if server has won, & append msg
		updateLocalBoad('x',r,c);
		if(checkWin(r,c))				msg+=" LOSS";
		else if(validMoves.size()==0)	msg+=" TIE";
		return msg;
	}
	
	// To be run after receiving a msg from the client.  Interprets msg to extract row/col updates 
	//	the board/validMoves list, and checks if client's move results in a win, returning true if 
	//	so, otherwise, false.
	private boolean parseMove(String msg)
	{	String[] ray=msg.split(" ");
		int r=Integer.parseInt(ray[1]);
		int c=Integer.parseInt(ray[2]);
		updateLocalBoad('O',r,c);
		return checkWin(r,c);
	}
	
	// To be run following the board[][] being updated with a new move.  This function takes a 
	//	space through row and column index and checks whether that space contributes to a win.  
	//	Following the game's logic, this will result in a return of True following a player's 
	//	winning move.
	private boolean checkWin(int r,int c)
	{	char s=board[r][c];			// Store the char at the referenced space to a local var to shorten line lengths
		if(s==' ')	return false;	// If a space is empty, it does not contribute to a win.
		if(s==board[r][0]&&s==board[r][1]&&s==board[r][2]&&s==board[r][3])	return true;
		if(s==board[0][c]&&s==board[1][c]&&s==board[2][c]&&s==board[3][c])	return true;
		if(r==c)		// Check for a top-left to bottom-right win if space falls in that range.
			if(s==board[0][0]&&s==board[1][1]&&s==board[2][2]&&s==board[3][3])	return true;
		else if(r+c==3)	// Check for a top-right to bottom-left win if space falls in that range.
			if(s==board[0][3]&&s==board[1][2]&&s==board[2][1]&&s==board[3][0])	return true;
		return false;	// Return falls if all win conditions fail to validate.
	}
	
	// Function used to record a space has been played and by whom, first by updating the board[][] 
	//	array with the player's char, then removing the space from the list of validMoves.  
	// No validation is taken that the move is valid because if the server is making a move, it
	//	should be valid as it came from the list of validMoves and per assignment "Neither the client 
	//	NOR the server should EVER make an illegal move" so it is trusted that the client's move is 
	//	valid.  If a client were to send an invalid move (within bounds) it will override the existing 
	//	space on the server-side, if out of bounds, an unhandled ArrayIndexOutOfBoundsException occurs
	//	ungracefully killing the Thread and severing the connection with the client.
	private void updateLocalBoad(char player,int r,int c)
	{	board[r][c]=player;
		validMoves.remove(Integer.valueOf(r*10+c));
	}

}
