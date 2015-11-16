import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class WebServer {
	//Webserver waits to accept connections and then creates a new thread for each one
    public static void main(String args[]) throws Exception {
	  if(args.length == 0)
	  {
		  System.err.println("Usage: port number");
		  System.exit(1);
	  }
	  else{	
		  ServerSocket serverSock = new ServerSocket(Integer.parseInt(args[0]));
    		while(true) {
      		Socket conn = serverSock.accept();
			ThreadServer serve = new ThreadServer(conn);
			serve.start();
		}
	}
	}
} 

class ThreadServer extends Thread{
	Socket conn = null;
 	private static final String[] requests = {"HEAD","TRACE","GET"};
	public ThreadServer(Socket c) {conn = c;}
	
	public void run(){
		try{
		Calendar date = Calendar.getInstance();
		//creates a file called log, append is true
		FileOutputStream log = new FileOutputStream("log", true); 
		//creates a printStream to print to the log file
		PrintStream printLog = new PrintStream(log);  
		//format of date
		SimpleDateFormat fmt = new SimpleDateFormat("EEE',' dd MMM yyy HH:mm:ss zzz");
        Scanner scanin = new Scanner(conn.getInputStream());
        String line=null;
        String linesFromClient[] = new String[32]; //holds lines of the request

  	  for(int x = 0; x < linesFromClient.length; x++){
          line = scanin.nextLine();
         	if(line.length()==0) break;
         	linesFromClient[x] = line;
  	  }

        for(int x = 0; x < linesFromClient.length; x++){
  	  		if(linesFromClient[x] == null) break;
    	  }	  
  	  String reply = "";	  
      Scanner scanWord = new Scanner(linesFromClient[0]);
  	  String dateString = fmt.format(date.getTime());
      String command = scanWord.next(); // the command e.g GET
  	  String ip = conn.getInetAddress().getHostAddress();
	  //if the first line in linesFromClient does not have 3 words, or doesnt have a / this happens.
  	  if(linesFromClient[0].split(" ").length != 3 || !linesFromClient[0].contains(" /"))
  	  { 
  		  if(command.equals("GET")){
  		  String http = scanWord.next();
  		  reply="" + http + " 400 Bad Request\r\n" +
  			  "Connection: close\r\n" +
  				  "Date: " + dateString + "\r\n" +
  				  "Content-Type: text/html\r\n" +
  					  "\r\n" +
  						  "<h1>Error 400 Bad Request</h1>\r\n";}
  		  //if the command is not GET it does not send the body
  		  else{String http = scanWord.next();
  			  reply="" + http + " 400 Bad Request\r\n" +
  			  "Connection: close\r\n" +
  				  "Date: " + dateString + "\r\n" +
  				  "Content-Type: text/html\r\n" +
  					  "\r\n";}
  		  OutputStream outs = conn.getOutputStream();
  		  outs.write(reply.getBytes());
  		  String logLine = "" + command + " Client IP " + ip + " response: " + reply.substring(9,reply.indexOf("C")-2) + " - Date/time: " + fmt.format(date.getTime()) + "\r\n";
  		  printLog.println(logLine);
  		  conn.close();
  	  }
	  //else the rest of the words are seperated from the requests received.
  	  else{
      String resource = scanWord.next(); // the resource
  	  String http = scanWord.next(); // HTTP 1.0 or 1.1
      String fileName = "www" + resource; //The path to the file
  	  String hostLine = ""; //the full host: line
  	  String host = ""; // the host
  	  Boolean correctProtocol = true; //This is true unless using HTTP/1.1 and no host line.
	  int checkHost = 0;
	  //if using HTTP/1.1 checks for a host lines, if its found, breaks from loop, else ends when the next line is empty.
	  if(http.equals("HTTP/1.1"))
	  {
	  		while(checkHost < linesFromClient.length && linesFromClient[checkHost] != null)
	  	  	{
		  		if(linesFromClient[checkHost].contains("host") || linesFromClient[checkHost].contains("Host")){
	  		 	  Scanner scanWord2 = new Scanner(linesFromClient[checkHost]);
	  		 	  hostLine = scanWord2.nextLine();					          
	  		 	  host = hostLine.substring(hostLine.lastIndexOf(":") + 2);
	  		 	  correctProtocol = true;
			 	  break;	
		 	 }
		  	 else
			  {
				  checkHost += 1;
				  correctProtocol = false;
			  }  
		  }
	  }
	  if(!http.equals("HTTP/1.1") && !http.equals("HTTP/1.0")) correctProtocol = false;
      File checkFile = new File(fileName);
	  //check if the file is a directory, if it is, lead the client to index file
	  if(checkFile.isDirectory())
	  {
		  checkFile = new File(fileName + "/index.html");
		  if(!checkFile.exists())
		  {
			  checkFile = new File(fileName + "/index.htm");
		  }
	  }
  	  Date lastModified = new Date(checkFile.lastModified());
  	  String extension = fileName.substring(fileName.lastIndexOf(".")+1,fileName.length()); 	  //finds the files extension to decide what content type to put in the header
  	  String contentType = "";
      if(extension.equals("jpg") || extension.equals("jpeg"))
  	  {
  		  contentType = "image/jpeg";
  	  }
  	  else if(extension.equals("png"))
  	  {
  		  contentType = "image/png";
  	  }
	  else if(extension.equals("gif"))
	  {
	  	  contentType = "image/gif";
	  }
  	  else if(extension.equals("txt"))
  	  {
  	  	  contentType = "text/plain";
  	  }
  	  else{contentType = "text/html";}
  	  //checks to make sure the server can handle the request, has to be get, head or trace
  	  if(!command.equals(requests[0])&&!command.equals(requests[1])&&!command.equals(requests[2]))
  	  {
  		  reply="" + http + " 501 Not Implemented\r\n" +
  			  "Connection: close\r\n" +
  				"Date: " + dateString + "\r\n" +
  				  "Content-Type: text/html\r\n" +
  					  "\r\n" +
  						  "<h1>Error 501 Not Implemeted</h1>\r\n";
  	   OutputStream outs = conn.getOutputStream();
  	   outs.write(reply.getBytes());
  	  }
	  //The response if the correctProtocol check from earlier is false
	  else if(!correctProtocol){
	  		  if(command.equals("GET")){
	  		  reply="" + http + " 400 Bad Request\r\n" +
	  			  "Connection: close\r\n" +
	  				  "Date: " + dateString + "\r\n" +
	  				  "Content-Type: text/html\r\n" +
	  					  "\r\n" +
	  						  "<h1>Error 400 Bad Request</h1>\r\n";}
	  		  //if the command is not GET it does not send the body
	  		  else{
	  			  reply="" + http + " 400 Bad Request\r\n" +
	  			  "Connection: close\r\n" +
	  				  "Date: " + dateString + "\r\n" +
	  				  "Content-Type: text/html\r\n" +
	  					  "\r\n";}
	  		  OutputStream outs = conn.getOutputStream();
	  		  outs.write(reply.getBytes());
	  }
  	  //Next if checks to see whether the file exists, 
  	  //returns a 404 error if not
        else if(!checkFile.exists()){
			if(command.equals("GET")){
  	  reply="" + http + " 404 Not Found\r\n" +
                     "Connection: close\r\n" +
  				   "Date: " + dateString + "\r\n" +
                     "Content-Type: text/html\r\n" +
                     "\r\n" +
                     "<h1>404 Not Found</h1>\r\n";
  		}
		else{
    	  reply="" + http + " 404 Not Found\r\n" +
                       "Connection: close\r\n" +
    				   "Date: " + dateString + "\r\n" +
                       "Content-Type: text/html\r\n" +
                       "\r\n";
			}
	  	  	OutputStream outs = conn.getOutputStream();
	  	  	outs.write(reply.getBytes());
        }
  	  //Returns a 200 OK response if the file exists, then sends the file requested.
        else if(command.equals(requests[0])){  
  			  reply ="" + http + " 200 OK\r\n" +
  				  "Connection: close\r\n" +
  					  "Date: " + dateString + "\r\n" +
  						  "Last-Modified " + fmt.format(lastModified) + "\r\n" +
  					  "Content-Length: " + checkFile.length() + "\r\n" +
  						  "Content-Type: " + contentType + "\r\n\r\n";
  			  OutputStream outs = conn.getOutputStream();
  			  outs.write(reply.getBytes());
  		  }
  	  //returns a trace request correctly
  	  else if(command.equals(requests[1])){
  			  reply ="" + http + " 200 OK\r\n" +
  				  "Connection: close\r\n" +
  					   "Date: " + dateString + "\r\n" +
  						  "Content-Type: message/http" + "\r\n\r\n" +
							  command + " " + resource + " " + http
  								   + "\r\n";
  			  OutputStream outs = conn.getOutputStream();
  			  outs.write(reply.getBytes());
  		  }
  			else{  //The standard return for the GET command
  		  	  		reply ="" + http + " 200 OK\r\n" +
  			  		  "Connection: close\r\n" +
  					   "Date: " + dateString + "\r\n" +
  					    "Last-Modified: " + fmt.format(lastModified) + "\r\n" +
  				  		 "Content-Length: " + checkFile.length() + "\r\n" +
  					  	   "Content-Type: " + contentType + "\r\n\r\n";
  		  		  	OutputStream outs = conn.getOutputStream();
  		  			InputStream fileInStream = new FileInputStream(checkFile); 
  		  	  		outs.write(reply.getBytes());
  		  			byte xfer[] = new byte[128];
  		  			while(true){
  			  			int rc = fileInStream.read(xfer,0,128);
  			    		if(rc<=0) break;
  			    		outs.write(xfer,0,rc);
  		  		}
  			}
			//Where the request is logged
  			String logLine = "" + command + " " + "'" + fileName + "'" + " Client IP " + ip + " response: " +
			reply.substring(9,reply.indexOf("C")-2) + " - Date/time: " + fmt.format(date.getTime()) + "\r\n";
  			printLog.println(logLine);
  			conn.close();
  	  		}
		}catch (IOException e){
			System.err.println("Error reading socket");
			System.exit(1);
		}
  		}
	}