
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
			//this.send();
			this.parse();
			//this.receive();
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
	
	/**
	 * Sends signals
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws FileSystemException
	 */
	private void send() throws FileNotFoundException, IOException, FileSystemException{
	    String[] tokens = this.cmd.split(" ");		
		//sending a file to server
		if(tokens[0].equals("put") && tokens.length == 2){
			String fileName = tokens[1];
			
			//send command the server first
		    
		    out.println(this.cmd);
		    out.flush();	
		    
		    //stream the file next
			this.readBytesAndOutputToStream(fileName);			
		}
		//sending string to server
		else{
		    //Get the output stream to the server
		    PrintWriter out = null;
		    out = new PrintWriter(this.socketN.getOutputStream());
		    //Send the command to the server
		    out.println(this.cmd);
		    out.flush();	
		}
	}
	
	private void receiveGet() throws IOException{
		//read in a line it will tell you command ID
		String inputCommandId = inputCommandId = this.br.readLine();
		System.out.println(inputCommandId);
		
		//read in another line, it will tell you if file exists or not
		//if file exists read it, otherwise end thread
		String[] tokens = this.cmd.split(" ");
		//TODO cant assume that there is a tokens[1]
		String fileName = tokens[1];
	    boolean acceptFile = this.checkServerResponse();
	    int count = -1;
	    if(acceptFile){
		    //If the file exists then we need to write to file.
		byte[] buffer = new byte[16*1024];
		FileOutputStream fos = new FileOutputStream(fileName);
		while((count = this.in.read(buffer)) > 0){
    		    //CreateFile
		    fos.write(buffer, 0, count);
		}
		fos.flush();
		fos.close();
	    }
	    //read line after wards. it wil either say file successfuy downloaded or it will be a filename to delete
	  String response = this.br.readLine();

	    if (!response.equals("File does not exist") || !response.equals("This is a directory, you can only move files.") || 
	    		!response.equals("Error reading file") || !response.equals("Download successful.")){
	    	
			File file = new File(response);
			if (file.exists()){
				file.delete();
				System.out.println("File deleted");
			}		
	    }
	    else{
	    	System.out.println(response);
	    }
	
		//read in file once server sends it
		//return it or print
	}
	private void receiveElse() throws IOException{
			this.printResponse();
		
	}
	
	private void receivePut() throws IOException{
		//read in a line it will tell you command ID
				String input = null;
			
		//read in another line saying if writing was successful or not
		input = this.br.readLine();
		System.out.println("Message from the server: " + input);
		
		
	}
	
	private void receiveTerminate() throws IOException{
		//expect 1 line (string) print it out
		String input = null;
		input = this.brTerminate.readLine();
		System.out.println("Message from the server:" + input);
	}
	
	//TODO second part.
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
	private void sendPut() throws IOException{
		String[] tokens = this.cmd.split(" ");
		String fileName = tokens[1];
		//check if file exists
		//if it doesnt, return error, let thread die
		File file = new File(fileName);
		if (!file.exists()){
		    throw new FileNotFoundException();
		}
		if (file.length() > Long.MAX_VALUE){
			throw new FileSystemException("File size too large");
		}
		
		//else send command to nSocket		
		//send command the server first
	    PrintWriter out = new PrintWriter(this.socketN.getOutputStream());
	    out.println(this.cmd);
	    out.flush();	
	    
	  //read in a line it will tell you command ID
	  		String input = null;
	  		input = this.br.readLine();
	  		System.out.println("Put- command id: " + input);
	    
	    //stream the file next
		this.readBytesAndOutputToStream(fileName);	
		// then send the actual file
		
		this.receivePut();
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
	
	private void sendTerminate() throws IOException{
		//send command to Tsocket
		 PrintWriter out = null;
		  out = new PrintWriter(this.socketT.getOutputStream());
		  out.println(this.cmd);
		  out.flush();
		  //send command
		this.receiveTerminate();
	}
	
	/**
	 * Receives signals and checks the command to tokenize them
	 * @throws IOException
	 */
	private void receive() throws IOException{
	    String[] tokens = this.cmd.split(" ");
	    String cmd = tokens[0];
	     	
    	if (tokens.length > 1 && cmd.equals("get")){
    		 String fileName = tokens[1];
    		//case 1, client issued a get file command and server is currently returning file. 
	    
		    //first check to make sure there was no error in getting file
		    //check server's response. 
		    boolean acceptFile = this.checkServerResponse();
		    if(acceptFile){
			    //If the file exists then we need to write to file.
			    byte[] bytes = new byte[16*1024];
			    
			    this.in.read(bytes);
			    
			    //CreateFile
			    FileOutputStream fos = new FileOutputStream(fileName);
			    fos.write(bytes);
			    fos.close();
		    }
    	}
    	//print response is called no matter what
	    printResponse();
	}//receive
	
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
	 * Helper method for send. Reads a file to bytes then outputs it to the output stream
	 * @param fileName String the name of the file to output to the stream
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws FileSystemException
	 */
	private void readBytesAndOutputToStream(String fileName) 
			throws FileNotFoundException, IOException, FileSystemException{
		
		File file = new File(fileName);
		if (!file.exists()){
		    throw new FileNotFoundException();
		}
		if (file.length() > Long.MAX_VALUE){
			throw new FileSystemException("File size too large");
		}
		
		//get the output stream
		OutputStream out = this.socketN.getOutputStream();

		//create an input stream for the file
		FileInputStream fileInputStream = new FileInputStream(file);
		
		//create a byte array
		byte[] bytes = new byte[(int) file.length()];	    	
    	
		//write the bytes to the output stream
    	int count;
    	while ((count = fileInputStream.read(bytes)) > 0){
    		out.write(bytes, 0, count);
    	}
    	
    	//close the file input stream
    	fileInputStream.close();
	}
	
	
	
	/**
	 * Helper method; Receives byte streams in order to read then write to particular file
	 * 
	 * @param fileName
	 * @param fileWriter
	 * @param fileDownloader
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void receiveByteStreamAndWriteToFile(String fileName, FileOutputStream fileWriter, 
			InputStream fileDownloader) throws IOException{

		try{    
			//write file to client system
			byte bytes[] = new byte[16*1024];
			int count;
			while ((count = fileDownloader.read(bytes)) > 0) {
				fileWriter.write(bytes, 0, count);
			}//while
		}//try
		finally{
			if (fileWriter != null) fileWriter.close();
			if (fileDownloader != null) fileDownloader.close();
		}//finally
		
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
