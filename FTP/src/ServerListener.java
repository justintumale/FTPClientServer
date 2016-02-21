import java.net.Socket;
import java.util.concurrent.ExecutorService;


public class ServerListener implements Runnable{
	
	private int port;
	private ExecutorService threadPool;
	private Socket clientSocket;
	
	public ServerListener(int port, Socket clientSocket, ExecutorService threadPool){
		this.port = port;
		this.clientSocket = clientSocket;
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
