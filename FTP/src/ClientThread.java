
/**
 * ClientThread.java
 * @author Montana Wong
 * @author Justin Tumale
 * @author Matthew Haneburger
 * @description Represents one connection
 * */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.FileSystemException;
import java.util.Arrays;
import java.util.HashMap;



/**
 * Creates ClientThread inherited from Java.lang.Thread 
 * Represents one connection
 *
 */
public class ClientThread extends Thread {
	
	private Socket socketN, socketT;
	private String cmd;
    private InputStream in;
    private BufferedReader br;
    private BufferedReader brTerminate;
    private PrintWriter out;
	private final int BUF_SIZE = 16*1024;
	private final int HEADER_OFFSET = 3;
    private final byte[] VALID = new byte[]{1,1,1}, INVALID = new byte[]{0,0,0};
    private String commandId = null;
    private ClientThread self = null;

    
	/**
	 * Creates new ClientThread with param socket and cmd 
	 * Sets socket and connection
	 * Constructor
	 * @param socket
	 * @param cmd
	 */
	public ClientThread(Socket socketN,Socket socketT, String cmd){
		super();
		this.socketN = socketN;
		this.socketT = socketT;
		this.cmd = cmd;
		try{
		    this.in = socketN.getInputStream();
		    this.br = new BufferedReader(new InputStreamReader(this.socketN.getInputStream()));
		    this.brTerminate = new BufferedReader(new InputStreamReader(this.socketT.getInputStream()));
		    this.out = new PrintWriter(this.socketN.getOutputStream());
		}
		catch(Exception e){
		    e.printStackTrace();   
		}
	}
	
	/**
	 * overridden method run to send and receive signals and catches exceptions with reading files and 
	 * handling sockets
	 */
	//TODO first.
	@Override
	public void run(){
		
		try {
			this.parse();
		}
		catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
			System.out.println("File not found.");
		}
		catch(FileSystemException fse){
			System.out.println("File size too large.");
		}
		catch(IOException ioe){
			System.out.println("Error reading file or socket");
		}


	}
	
	private void parse() throws IOException{
		String[] tokens = this.cmd.split(" ");	
		if (tokens[0].equals("get")){
			this.sendGet();
		}
		else if (tokens[0].equals("put")){
			this.sendPut();
		
		}
		else if (tokens[0].equals("terminate")){
			this.sendTerminate();
		}
		else if(tokens[0].equals("quit")){
			return;
		}
		else{
			this.sendElse();
		}
	}	
	
	private void sendGet() throws IOException{
		//send command on Nsocket
	    PrintWriter out = null;
	    out = new PrintWriter(this.socketN.getOutputStream());
	    
	    //Send the command to the server
	    out.println(this.cmd);
	    out.flush();	
	
		this.receiveGet();
	}
	
	private void receiveGet() throws IOException{
		//read in a line it will tell you command ID
		String inputCommandId = this.br.readLine();
		System.out.println(inputCommandId);
		
		//read in another line, it will tell you if file exists or not
		//if file exists read it, otherwise end thread
		String[] tokens = this.cmd.split(" ");
		if (tokens.length!=2){
			System.out.println("Please enter the command in the proper format: get <filename>");
		}
		else{
				String fileName = tokens[1];
			    boolean acceptFile = this.checkServerResponse();
			    int count = -1;
			    String postFileResponse = "Received";
			    if(acceptFile){
					    //If the file exists then we need to write to file.
					byte[] buffer = new byte[BUF_SIZE];	
					FileOutputStream fos = new FileOutputStream(fileName);
					while((count = this.in.read(buffer)) > 0){
						//extract the header from packet received from server.
						byte[] serverHeader = (byte[])Arrays.copyOfRange(buffer, 0, VALID.length);
						if (Arrays.equals(serverHeader, VALID )){
							//packet header is valid so keep writing file
							fos.write(buffer, HEADER_OFFSET, count-HEADER_OFFSET); //offset the buffer by 3, that is where our packet header is contained.
						}
						else if(Arrays.equals(serverHeader, INVALID )){
							//we know that the file has been terminated. clean up.
							//delete file
							postFileResponse = "File transfer terminated";
							break;
						}
						else{
							System.out.println("Major unexpected error. Debug needed the header values are: " + Arrays.toString(serverHeader));
						}
					}
					fos.flush();
					fos.close();
					System.out.println(postFileResponse);
			    }
			    else{
			    	//if file was not found or error on server prevented file transfer starting.
			    	System.out.println(this.br.readLine());
			    } 
		}
	}
	
	
	private void sendPut() throws IOException{
		String[] tokens = this.cmd.split(" ");
		if (tokens.length != 2){
			System.out.println("Please enter the command in the proper format: put <filename>");
		}
		else{
			byte[] buffer = new byte[BUF_SIZE];
			
			String fileName = tokens[1];
			//check if file exists
			//if it doesnt, return error, let thread die
			File file = new File(fileName);
			if (!file.exists()){
			    throw new FileNotFoundException();
			}
			if ((long) file.length() > Long.MAX_VALUE){
				throw new FileSystemException("File size too large");
			}
			//else send command to nSocket		
			//send command the server first
		    this.out.println(this.cmd);
		    out.flush();
			System.out.println("Command id: " + this.br.readLine());
		    
			///
		    //open the output stream
			OutputStream fos = this.socketN.getOutputStream();
			
			//create a buffer for the bytes to be sent through the outstream
			
			//open the file's input stream
			FileInputStream fis = new FileInputStream(file);
			int count = -1;
			//grab the bytes from the file
			int packetCount = 0;
			while ((count = fis.read(buffer)) > 0){
				//write buffer onto output stream
				System.out.println("byte count" + count);
				fos.write(buffer);
				packetCount++;
			}
			System.out.println(packetCount);
			fis.close();
			fos.flush();
			this.socketN.shutdownOutput();
			this.receivePut();
			///
			/*
		    //read in a line it will tell you command ID
	  		String input = null;
	  		input = this.br.readLine();
	  		System.out.println("Put- command id: " + input);
	  		
			//Put id / Thread into hashmap
		    this.hashPut(input, this);
		    
		    //stream the file next
			this.readBytesAndOutputToStream(fileName);	
			// then send the actual file
			*/
			
		
		}
	}
	
	private void receivePut() throws IOException{
		//read in a line it will tell you command ID
				String input = null;
			
		//read in another line saying if writing was successful or not
		//input = this.br.readLine();
		//System.out.println("Message from the server: " + input);	
			System.out.println("File sent");
	}
	
	private void sendElse() throws IOException{
		//send command
		PrintWriter out = null;
	    out = new PrintWriter(this.socketN.getOutputStream());
	    //Send the command to the server
	    out.println(this.cmd);
	    out.flush();
	    this.receiveElse();
	}
	
	private void receiveElse() throws IOException{
			this.printResponse();
		
	}
	
	private void sendTerminate() throws IOException{
	    String[] tokens = this.cmd.split(" ");
	    if (tokens.length != 2){
	    	System.out.println("Please enter the command in the proper format: terminate <command ID>");
	    }
	    else{
			//send command to Tsocket
			  PrintWriter out = null;
			  out = new PrintWriter(this.socketT.getOutputStream());
			  out.println(this.cmd);
			  out.flush();

			  
			  //send command
			  this.receiveTerminate();
	    }
	}
	
	private void receiveTerminate() throws IOException{
		//expect 1 line (string) print it out
		String input = null;
		input = this.brTerminate.readLine();
		System.out.println("Message from the server:" + input);
	}
		
	/**
	 * Checks the server response and throws IOException
	 * 
	 * @return prompt to user that file is accepted or not accepted
	 * @throws IOException
	 */
    private boolean checkServerResponse() throws IOException{
		StringBuffer response = new StringBuffer();
		String input = null;
		
		input = this.br.readLine();
		response.append(input);
	
		return (response.toString().equals("Accept")) ? true : false;  

    }


	/**
	 * Prints response by user
	 * @throws IOException
	 */
	public void printResponse() throws IOException{
		//Print the response
		String input = null;
		while (((input = this.br.readLine()) != null) && !input.equals("")){
				System.out.println(input);
		}
	}
	
	
}
