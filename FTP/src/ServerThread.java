/* ServerThread.java
 * @author Montana Wong
 * @author Justin Tumale
 * @author Matthew Haneburger
 * @description: Handles connections to the server. Individual thread will listen to client to tell it
 * what commands to run and sends the output of said command. Contains parsing method to generate the 
 * specific output given by the client/user.
 * */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
/**
 * Implements Runnable to override the inherited run method
 */
public class ServerThread implements Runnable {
	
	private Socket clientSocket;
	private File currentWorkingDir;
	private PrintWriter out;
	private BufferedReader br;
	private volatile HashMap<String, Boolean> commandIds;
	private String commandId = null;
	private final int TERMINATE_INTERVAL = 1000;
	private final int BUF_SIZE = 16*1024;
	private final int HEADER_OFFSET = 3;
	private final byte[] VALID = new byte[]{1,1,1}, INVALID = new byte[]{0,0,0};
	
	
	public ServerThread(Socket clientSocket, ServerSocket serverSocket, HashMap<String, Boolean> commandIds){
		this.clientSocket = clientSocket;
		this.currentWorkingDir = new File(System.getProperty("user.dir"));
		this.commandIds = commandIds;
		//out is the message buffer to return to the client
		try {
			this.out = new PrintWriter(clientSocket.getOutputStream(), true);
			//br is the incoming message buffer from the client to be read by the server
			this.br = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
		} catch (IOException e) {
		    e.printStackTrace();
			System.out.println("Error connecting to socket.");
		}

		
		
	}
	
	/**
	 * Automatically called when the thread is created. This method handles the communication between
	 * client and server until the client enters the quit command. The thread dies soonthereafter. 
	 */
	@Override
	public void run(){
		//do tasks until no more, then let thread die
		System.out.println("Running thread!");
		
		try {
			String command = "";
			String response = "";
			while((command = this.br.readLine()) != null){
				if(command.equalsIgnoreCase("quit")) {
					this.out.println("Goodbye, Exiting\n");
					break;
				}
				
				//parse client's request
				response = this.parse(command);
				
				//return server's response
				if(response != null)
					this.out.println(response + "\n");
			}
			//close reader and writer
			this.out.close();
			this.br.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try {
			    //close client conn.
			    this.clientSocket.close();
			} catch (IOException e) {
				System.out.println("Error closing client socket");
			}
			//remove the command Id from table if we finished a get or put command
			if(this.commandId != null){
				synchronized (this.commandIds){
					this.commandIds.remove(this.commandId);
				}
			}
		}
		
	}
	/**
	 * Parses the client's command and returns the response string
	 * @param cmd String the command to be parsed.
	 * @return response String the response to return to the client.
	 */
    private String parse(String cmd){
		//break command into an array of each word
		// e.g. mkdir files -> {"mkdir", "files"}
		String[] tokens = cmd.split(" ");
		
		if (tokens.length == 1){
			switch(cmd){
				case "ls":
					return this.ls();
				case "pwd":
					return this.pwd();
			}
		}
		else if(tokens.length == 2){
			switch(tokens[0]){
				case "mkdir":
					return this.mkdir(tokens[1]);
				case "delete":
					return this.delete(tokens[1]);
				case "get":
					return this.get(tokens[1]);
				case "put":
					return this.put(tokens[1]);
				case "cd":
					return this.cd(tokens[1]);
				case "terminate":
					return this.terminate(tokens[1]);
			}	
		}
		return "Command not supported.";
	}

    private String terminate(String commandId){
    	Boolean b;
    	synchronized (this.commandIds){
	    		b = this.commandIds.get(commandId);
	    	
	    	if (b == null){
	    		return "Command ID not found";
	    	}
	    	else if(b.booleanValue() == true){
	    		this.commandIds.put(commandId, new Boolean(false));
	    		return "Terminating command Id: " + commandId;
	    	}
	    	else if (b.booleanValue() == false){
	    		return "Command id is terminating or already terminated";
	    	}
	    	return "Error in terminate command ";
    	}
    	
    }
    
    
    //TODO add flag check to this method
	/**
     * method of file transfer
     * @param name of file to remote machine from local machine
     * @return success or failure message
     * */
	private String put(String fileName) {
		byte buffer[] = new byte[BUF_SIZE];
		synchronized (this.commandIds){
			//add cmd Id to hashtable
			this.commandId = this.generateId();
			this.commandIds.put(this.commandId, new Boolean(true));
		}
		
		//send client the command ID
		this.out.println(
			this.commandId
		);
		this.out.flush();
		
	    InputStream in = null;
		try {
			in = this.clientSocket.getInputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//////////////
		int count, limit = 0;
		File f = new File(this.currentWorkingDir, fileName);
		FileOutputStream fos;
		boolean keepActive = true;
		try {
			fos = new FileOutputStream(f);
			//receive the bytes from the client
			while ((count = in.read(buffer)) > 0){
				//write the bytes to a buffer
				fos.write(buffer);
				//increment the limit count
				limit += count;
				//check if we've reached the interval
				if (limit >= TERMINATE_INTERVAL){
					//reset the limit
					limit = 0;
					//check the hash table
	    			synchronized (this.commandIds){
	    				keepActive = this.commandIds.get(this.commandId).booleanValue();
	    			}
	    			//if the command was terminated, close the stream and delete the 
	    			//partial file from the directory
					if (!keepActive){
						System.out.println("Thread received terminate command. put process killed.");
						fos.close();
						this.delete(fileName);
						return "Received terminate command, file: " + fileName + " download stopped and deleted from server.";
					}
				}
			}
			fos.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "Error on server";
		} catch (IOException e) {
			e.printStackTrace();
			return "Error on server";
		}
		return null;//fileName + " successfully copied to server";
	}

	/**
	 * This method is intended to transfer a file from remote machine to local machine
	 * @param name of the file to transfer
	 * @return success or failure message of the file transfer
	 * */
	private String get(String fileName) {
		synchronized (this.commandIds){
			//add cmd Id to hashtable
			this.commandId = this.generateId();
			this.commandIds.put(this.commandId, new Boolean(true));
		}
		//send client the command ID
		this.out.println(
			this.commandId
		);
	    File f = null;
	    
	    //Create new file
	    try{
	    	f = new File(this.currentWorkingDir, fileName);
	    	if (!f.exists()){
	    		throw new FileNotFoundException();
	    	}
	    	if(f.isDirectory() == true){
	    		this.notifyClient(false);
	    		return "This is a directory, you can only move files.";
	    	}
	    }
	    catch(FileNotFoundException e){
	    	this.notifyClient(false);
	    	return "File does not exist";   
	    }
	    try{	    	
	    	this.notifyClient(true);
	    	//move code here
	    	OutputStream fileOut = this.clientSocket.getOutputStream();

	    	boolean keepActive = true;
	    	
	    	//create an input stream for the file
	    	FileInputStream fileInputStream = new FileInputStream(f);
		    //create a byte array
	    	byte[] buffer = new byte[BUF_SIZE];	    	
	    	int count, checkLimit = 0;
		    //write the bytes to the output stream and add header to each packet
	    	while ((count = fileInputStream.read(buffer, HEADER_OFFSET, BUF_SIZE - HEADER_OFFSET)) > 0){
	    		
	    		//copy appropriate header to offsetted buffer
	    		System.arraycopy((keepActive) ? VALID : INVALID, 0, buffer, 0, VALID.length);
	    		
	    		//write to client. (count needs 3 because we only read count-3 from the fileinputstream)
	    		fileOut.write(buffer, 0, count + HEADER_OFFSET);
	    		
	    		//sleep for testing terminate
	    		/*try {
	    			System.out.println("sleeping for 10 sec");
					Thread.sleep(10000);
					System.out.println("waking up");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
	    		
	    		//if cmd is terminated, notify client via headerInvalid then close stream
	    		if(!keepActive) {
	    			System.out.println("Terminate command received in get worker thread.");
	    			break;
	    		}
	    		
	    		checkLimit += count;
	    		//check terminate flag every 1000 bytes
	    		if(checkLimit >= TERMINATE_INTERVAL){
	    			synchronized (this.commandIds){
	    				keepActive = this.commandIds.get(this.commandId).booleanValue();
	    			}
	    			checkLimit = 0;
	    		}
	    	}
	    	fileInputStream.close();
	    	fileOut.flush();
	    	this.clientSocket.shutdownOutput();
	    	return null;//"Download successful.";
		
	    }
	    
	    catch(IOException e){
		    this.notifyClient(false);
		    return "Error reading file";
	    }
	}
    
/**Notify client whether or not the file exists.
	 * @param boolean flag
	 * @return "Accept" or "Error" based on result
	 * */
	 private void notifyClient(boolean sendingFile){
	//write to stream send some text
    	//System.out.println("Client notified");
    	if(sendingFile == false){
			this.out.println("Error");
    	}
    	else{
		    this.out.println("Accept");
    	}
    	   this.out.flush();
    }
	 
	
    /**
     * Changes the current working directory to the directory specified
     * @param path of the directory as only parameter
     * @return path of the new directory
     */
	private String cd(String newPath){
		//does not actually change the location, just rebuild this.cwd.
		File dir = new File(this.currentWorkingDir, newPath);
		if(dir.isDirectory() == true){
			System.setProperty("user.dir", dir.getAbsolutePath());
			this.currentWorkingDir = dir;
		}
		else{
			return newPath + " is not a directory.";
		}
		return "Changed directory";
	}
	
	/**
	 * Prints the current working directory
	 * @return the current working directory
	 */
   	private String pwd(){
		return System.getProperty("user.dir");
	}
	
   	/**
   	 * Returns a list of all files in the currently working directory on new lines
   	 * @return list of files
   	 */
	private String ls(){
		StringBuffer output = new StringBuffer();
		//File currentDirectory = new File(System.getProperty("user.dir"));
		String childs[] = this.currentWorkingDir.list();
		for(String file: childs){
			output.append(file + "\n");
		}
		return output.toString().substring(0, output.length());
	}
	
	/**
	 * Makes a new directory in the current working directory
	 * @param dirName, name of new directory
	 * @return error message 'success' or 'failure'
	 */
	private String mkdir(String dirName) {
		//Makes file object to check if it exists
		File file = new File(this.currentWorkingDir, dirName);
		if(!file.exists()){
			file.mkdir();
			return "Directory created!";
		}
		return "Directory not created, it already exists!";
		
	}
	/**
	 * Deletes a specified file
	 * @param fileName
	 * @return message indicating whether a file was deleted, not deleted, or error message
	 */
	private String delete(String fileName){
		//Makes file object to check if it exists
		File file = new File(this.currentWorkingDir, fileName);
		if(file.exists()){
			file.delete();
			if (!file.exists()){
				return "File Deleted!";
			}
			else{
				return "There was an error deleting the file";
			}
		}
		return "File not deleted. File does not exist!";	
	}
	
	private String generateId(){
		//max 6 digit number
		int max = 999999;
		//min 6 digit number
		int min = 100000;
		//adds min to random generated number to ensure 6 digits
		String id = Integer.toString( (int) Math.round(Math.random() * (max - min + 1) + min));
		 
		//return the hash or if it already exists in commandId table recompute
		synchronized (this.commandIds){
		    id = (this.commandIds.get(id) != null) ? generateId() : id;	
		}
		return id;
	}
	
	
}

