/**Filename: Client.java
 *	@author Montana Wong
 * 	@author Justin Tumale
 * 	@author Matthew Haneburger
 * 	@description connects client to server, and makes sure that the connection is syntactically correct. main
 * 	class to spawn client threads to connect to the server. 
 * */
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * description above
 * */
public class Client {
	
	private Socket clientSocket = null;
	private Socket clientSocketTerminate = null;
	private Scanner scanner = new Scanner(System.in);
	private PrintStream output = null;
	private int normalPort;
	private int terminatePort;
	private String hostName = null;
	/**
	 * @params port number
	 * */
	public Client(int portN, int portT){
		this.normalPort = portN;
		this.terminatePort = portT;
		this.hostName = "localhost";
	}
	/**
	 * @params hostname, port number
	 * */
	public Client(String hostName, int portN, int portT){
		this.normalPort = portN;
		this.terminatePort = portT;
		this.hostName = hostName;
	}
	/**
	 * this method creates a socket, and listens to commands given by the user. Read in from scanner and
	 * shows prompt. infinite until the user inputs "quit"
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 * */
	public void run() throws UnknownHostException, IOException, InterruptedException{
		//create socket
		this.clientSocket = new Socket(this.hostName, this.normalPort);
		this.clientSocketTerminate = new Socket(this.hostName, this.terminatePort);
		
		//read commands from sys.in
		String input = null;
		while(true){
			System.out.print("ftpclient> ");	
			input = this.scanner.nextLine();
		
			ClientThread clientThread = new ClientThread(this.clientSocket, this.clientSocketTerminate, input);
			clientThread.start();

			try{
				//this forces our client to be synchronous for now, program blocks until thread dies
				clientThread.join();
			}
			catch(InterruptedException ie){
				ie.printStackTrace();
			}
			if(input.equals("quit")) break;
			
		}
		if(this.clientSocket != null){
			this.clientSocket.close();
		}
	}
	
	
	
	public static void main(String[] args){
		boolean DEVELOPMENT = false;
		if(DEVELOPMENT){
			Client client = new Client("localhost", 60000, 59999);
			System.out.println("Running client!");
			try {
				client.run();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch(ConnectException ce){
				System.out.println("Connection to server refused.");
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			System.out.println("Client shutdown!");
		}
		else{
		
			try{
				if(args.length == 3 && Integer.valueOf(args[1]) <= 65535 && Integer.valueOf(args[2]) <= 65535){
					Client client = new Client(args[0], Integer.valueOf(args[1]), Integer.valueOf(args[2]));
					System.out.println("Running client!");
					client.run();
					System.out.println("Client shutdown!");
				}
				else{
					throw new NumberFormatException();
				}
			}
			catch(ConnectException ce){
				System.out.println("Connection to server refused.");
			}
			catch (UnknownHostException e) {
				System.out.println("Count not find host");
			}
			catch(NumberFormatException nfe){
				System.out.println("Client must be run with this syntax: java Client hostname nport tport" + 
						"\n e.g. java Client localhost 60000 60001");
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
}
