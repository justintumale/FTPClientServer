import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;


public class ServerListener extends Thread{
	
	private int port;
	private ExecutorService threadPool;
	private ServerSocket serverSocket;
	private HashMap<String, Boolean> commandIds;
	
	public ServerListener(int port, ServerSocket serverSocket, ExecutorService threadPool, HashMap<String, Boolean> commandIds){
		this.port = port;
		this.serverSocket = serverSocket;
		this.threadPool = threadPool;
		this.commandIds = commandIds;
	}
	
	@Override
	public void run() {
		while(true){
		//wait for incoming message from client
			try {
				//accept message
				Socket clientSocket = this.serverSocket.accept();
				//create new server thread to handle
				this.threadPool.execute(new ServerThread(
						clientSocket, 
						this.serverSocket,
						this.commandIds
				));
				
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
		
		
	}
	
}
