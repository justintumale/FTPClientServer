import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.net.*;

public class Server {

	private ExecutorService threadPool = Executors.newCachedThreadPool();
	private final int BACKLOG = 20;
	private int nPort, tPort;
	private String address = "localhost";
	private ServerSocket serverSocketN = null;
	private ServerSocket serverSocketT = null;
	private ServerListener listenerT = null;
	private ServerListener listenerN = null;
	private volatile HashMap<String, Boolean> commandIds;
	
	public Server(String address, int nPort, int tPort){
		this.address = address;
		this.nPort = nPort;
		this.tPort = tPort;
	}
	
	public Server(int nPort, int tPort){
		this.nPort = nPort;
		this.tPort = tPort;
	}
	
	public void run(){
		System.out.println("Server is running");
		this.commandIds = new HashMap<>();
		
		//create a socket
		try {
			//create two sockets listening at terminate and normal ports
			this.serverSocketN = new ServerSocket(
					this.nPort,
					this.BACKLOG
			);
			this.serverSocketT = new ServerSocket(
					this.tPort,
					this.BACKLOG
			);
			//create listeners that handle logic from each port
			this.listenerN = new ServerListener(this.nPort, this.serverSocketN, threadPool, commandIds);
			this.listenerT = new ServerListener(this.tPort, this.serverSocketT, threadPool, commandIds);
			
			//run listeners
			this.listenerN.start();
			this.listenerT.start();
			
			//keep Server running till both listeners die.
			this.listenerN.join();
			this.listenerT.join();
					
		} 
		
		catch (IOException e) {
 			System.out.println("Error reading socket");
		}
		catch(IllegalArgumentException iae){
			iae.printStackTrace();
		}
		catch(InterruptedException ie){
			ie.printStackTrace();
		}
		finally{
			//close sockets
			if (this.serverSocketN != null){
				try {
					this.serverSocketN.close();
				} 
				catch (IOException e) {
					System.out.println("Error closing server socket.");
				}
			}
			if (this.serverSocketT != null){
				try {
					this.serverSocketT.close();
				} 
				catch (IOException e) {
					System.out.println("Error closing server socket.");
				}
			}
		}
		
	}

	
	public static void main(String[] args){
		
		boolean DEVELOPMENT = true;
		if (DEVELOPMENT){
			Server myFtpServer = new Server("localhost", 60000, 60001);
			myFtpServer.run();
		}
		else{
			try{
				if(args.length == 2 && Integer.valueOf(args[0]) >= 49152 && Integer.valueOf(args[0]) <= 65535  && Integer.valueOf(args[1]) <= 65535){
					Server myFtpServer = new Server("localhost", Integer.valueOf(args[0]), Integer.valueOf(args[1]));
					myFtpServer.run();
				}
				else{
					throw new NumberFormatException();
				}
			}
			catch(NumberFormatException nfe){
				System.out.println("Server must be run with this syntax: java Server [nport number (49152 - 65535) ] [tport number (49152 - 65535) ]");
			}
		}
		
	}

}
