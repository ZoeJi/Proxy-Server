/* 
CSci 4131
Assignment 7
Authors: Qianying Ji - 4686347; Yahui Xiong-4943731
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ProxyServer extends Thread {
	protected Socket s; // socket connect with client
	int streamPosition = 0;
	String host;
	int port;
	OutputStream rawOut;
	InputStream rawIn;
	Socket serverConnection; // socket connect with server
	byte requestBuffer[] = new byte[8192];
	int storedCount = 0;
	int consumedCount = 0;
	Logger logger;
	String fileName;

	ProxyServer(Logger logger, Socket s, String ConfigFile) {
		System.out.println("New client.");
		this.s = s;
		this.logger = logger;
		this.fileName = ConfigFile;
	}

	protected Socket connectToServer() throws IOException {
		System.out.println("Connecting to " + host + ":" + port + "..");
		Socket socket = new Socket(host, port);
		System.out.println("Connected.");

		rawOut = socket.getOutputStream(); /*
											 * rawOut is OutputStream for Origin
											 * Server
											 */
		rawIn = socket.getInputStream();
		return socket;
	}

	/* Check the type of response header, return as String */
	protected TypeWrapper checkContentType(URL resourceURL) throws IOException {
		String responseHeaders = "SERVER RESPONSE FOR: " + resourceURL + "\n" + 
				"==================================RESPONSE HEADERS==========================================\n";
		int ContentLengthValue = 0;
		
		URLConnection connection = resourceURL.openConnection();
		boolean isContainContentLength = false;
		Map<String, List<String>> map = connection.getHeaderFields();

		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			if (entry.getKey() != null) {
				System.out.println(entry.getKey());
				if (entry.getKey().equals("Content-Length")) {
					isContainContentLength = true;
					ContentLengthValue = Integer.parseInt(connection.getHeaderField("Content-Length"));
				}
				responseHeaders = responseHeaders + entry.getKey() + ": ";
			}
			List<String> val = entry.getValue();
			for(String s : val) {
				responseHeaders = responseHeaders + s + " ";
			}
			responseHeaders = responseHeaders + "\n";
		}

		String contentType = connection.getHeaderField("Content-Type");
		
		logger.write(responseHeaders + "\n");
		
		TypeWrapper wrapper = new TypeWrapper(contentType, isContainContentLength, ContentLengthValue);
		return wrapper;
	}

	protected String errorMessageTemplate(String errorCode, String errorMessage) {
		String errorTemplate = "<?xml version='1.0' encoding='ISO-8859-1'?>\n"
				+ "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN'\n"
				+ "'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n"
				+ "<html xmlns='http://www.w3.org/1999/xhtml' lang='en' xml:lang='en'>\n"
				+ "<head>\n" + "<title>Error!!</title>\n"
				+ "<style type='text/css'>"
				+ "body { color: #000000; background-color: #FFFFFF; font-family: 'Helvetica'}\n"
				+ "p {margin-left: 3em;}\n" 
				+ "</style>\n" + "</head>\n" + "<body>\n" + "<h1>"
				+ errorCode + "</h1>\n" + "<p>" + errorMessage
				+ "</p><h2>By Qianying and Yahui</h2></body></html>";
		return errorTemplate;
	}
	
	protected void blockItem(OutputStream ostream, String errorCode, String errorMessage ) throws IOException {
		BufferedOutputStream buffout = new BufferedOutputStream(
				ostream);
		DataOutputStream output = new DataOutputStream(
				buffout);
	
		output.writeBytes("HTTP/1.1 " + errorCode + "\n\n");
		output.writeBytes(errorMessageTemplate(
				errorCode,
				errorMessage));
		output.flush();
		s.close(); // socket s
	}

	public void run() {
		StringTokenizer st;
		URL resourceURL = null;
		String requestHeaders = "==================================REQUEST HEADERS to Proxy Server==========================================\n";
		String Str406 = null;
		String noHopRequestHeaders = "==================================REQUEST HEADERS to Origin Server==========================================\n";

		/******************************************************************/
		/* Use this byte array to store the headers temporarily */
		/* These headers are printed for debugging */

		ByteArrayOutputStream headerBuf = new ByteArrayOutputStream(8096);
		PrintWriter headerWriter = new PrintWriter(headerBuf); 

		try {
			InputStream istream = s.getInputStream();
			OutputStream ostream = s.getOutputStream();

			// storedCount = istream.read(requestBuffer, 0, 8192);

			BufferedReader br = new BufferedReader(new InputStreamReader(
					istream));

			/********************************************************************************/
			/* Read Input Request Lines */
			/********************************************************************************/

			String requestLine;
			String resourcePath;
			String filePath;
			String urlPath;

			String line = "";
			if ((line = br.readLine()) != null) {
				requestLine = line;
			} else {
				throw new IOException("End of buffer!");
			}

//			System.out
//					.println("*************************************REQUEST HEADER**********************************************");
//			requestHeaders = requestHeaders + requestLine + "\n";
//			System.out.println(requestLine);
//
//			System.out
//					.println("*************************************END**********************************************");
			headerWriter.println(requestLine);
			noHopRequestHeaders = noHopRequestHeaders + requestLine + "\n";

			st = new StringTokenizer(requestLine);
			String request = st.nextToken(); /* GET, POST, HEAD */

			System.out.println("#DEBUG MESSAGE: Request Method =" + request);

			if (request.equals("GET")) {
				// System.out.println("Request = " + request);

				String uri = st.nextToken(); /* / URI */
				String protocol = st.nextToken(); /* HTTP1.1 or HTTP1.0 */

				if (uri.startsWith("http")) {
					/* It is a full URL. So get the file path */
					resourceURL = new URL(uri);
					filePath = resourceURL.getPath();
					host = resourceURL.getHost();
					port = resourceURL.getPort();
					if (port == -1) {
						port = 80;
					}
					System.out.println("#DEBUG MESSAGE:  Request URI = "
							+ filePath);
				}
				urlPath = uri;
			} else if (request.equals("HEAD")) {
				String uri = st.nextToken(); /* / URI */
				String protocol = st.nextToken(); /* HTTP1.1 or HTTP1.0 */

				if (uri.startsWith("http")) {
					/* It is a full URL. So get the file path */
					resourceURL = new URL(uri);
					filePath = resourceURL.getPath();
					host = resourceURL.getHost();
					port = resourceURL.getPort();
					if (port == -1) {
						port = 80;
					}
					System.out.println("#DEBUG MESSAGE:  Request URI = "
							+ filePath);
				}
				urlPath = uri;
			} 
			else {
				Str406 = "406";
				String uri = st.nextToken(); /* / URI */
				String protocol = st.nextToken(); /* HTTP1.1 or HTTP1.0 */

				if (uri.startsWith("http")) {
					/* It is a full URL. So get the file path */
					resourceURL = new URL(uri);
				}
				blockItem(ostream, "406 Not Acceptable", "This method is not acceptable.");
			}

			if ((line = br.readLine()) != null) {
				requestLine = line;
			} else {
				throw new IOException("End of buffer!");
			}

			st = new StringTokenizer(requestLine, ": ");
			String fieldName = st.nextToken(); /* Expect HOST field */

			if (fieldName.equals("Host")) {
				host = st.nextToken();
				String portString = new String("");

				try {
					portString = st.nextToken();
				} catch (Exception NoSuchElement) {
				}
				if (portString.length() == 0) {
					port = 80;
				} else
					port = Integer.parseInt(portString);

				System.out.println("#DEBUG MESSAGE  - Host = " + host
						+ " port number = " + port);
			}

			/*
			 * Print out request message
			 */
//			System.out
//					.println("*************************************REQUEST MESSAGE**********************************************");
			while (requestLine.length() != 0) {
				if (!(requestLine.startsWith("Connection")
						|| requestLine.startsWith("Keep-Alive")
						|| requestLine.startsWith("Proxy-Authenticate")
						|| requestLine.startsWith("Proxy-Authorization")
						|| requestLine.startsWith("TE")
						|| requestLine.startsWith("Trailers")
						|| requestLine.startsWith("Transfer-Encoding")
						|| requestLine.startsWith("Upgrade"))) {
					headerWriter.println(requestLine);
					noHopRequestHeaders = noHopRequestHeaders + requestLine + "\n";
				} 
					
				requestHeaders = requestHeaders + requestLine + "\n";
				//System.out.println(requestLine);
				if ((line = br.readLine()) != null) {
					requestLine = line;
				} else {
					throw new IOException("End of buffer!");
				}
			}

//			System.out
//					.println("*************************************END**********************************************");
			
			logger.write(requestHeaders + "\n");
			
			if(Str406 != null) {
				logger.write(resourceURL + "::" + request + " is not acceptable\n\n");
				return;
			}
			headerWriter.flush();
			logger.write(noHopRequestHeaders + "\n");	
			TypeWrapper importantInformation = checkContentType(resourceURL);

			String ConfigLine = null;

			try {

				// FileReader reads text files in the default encoding.
				FileReader fileReader = new FileReader(fileName);

				// Always wrap FileReader in BufferedReader.
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				
				while ((ConfigLine = bufferedReader.readLine()) != null) {
					if (ConfigLine.startsWith("#")) {
						continue;
					}
						
					else if ("".equals(ConfigLine.trim())) {
						continue;
					} else {
						String[] splitStr = ConfigLine.split("\\s+");
						if (splitStr.length == 1) {
							/*
							 * Block whole site
							 */							
							if (host.equals(splitStr[0])) {
								blockItem(ostream, "403 Forbidden", "You are trying to visit a blocked site!");
								logger.write(resourceURL + "::blocked" + "\n\n");
								return;
							}
						}
						else if(!splitStr[1].endsWith("*")){
							/*
							 * Block certain content-type
							 */
							if (host.equals(splitStr[0]) && (importantInformation.headerType.equals(splitStr[1]))) {
								blockItem(ostream, "403 Forbidden", "You are trying to visit a blocked site!");
								logger.write(resourceURL + "::" + importantInformation.headerType + " not allowed\n\n");
								return;
							}
						}
						else {
							String[] splitMIME = splitStr[1].split("/");
							if (host.equals(splitStr[0]) && splitMIME[0].equals("*")) {
								/*
								 * Block whole site
								 */	
								blockItem(ostream, "403 Forbidden", "You are trying to visit a blocked site!");
								logger.write(resourceURL + "::blocked" + "\n\n");
								return;
							} else if(host.equals(splitStr[0]) && (importantInformation.headerType.contains(splitMIME[0]))) {
								/*
								 * Block one of content-type categories
								 */
								blockItem(ostream, "403 Forbidden", "You are trying to visit a blocked site!");
								logger.write(resourceURL + "::" + splitMIME[0] + " not allowed\n\n");
								return;
							}
						}
					}
				}

				// Always close files.
				bufferedReader.close();
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			/********************************************************************************/
			/* Open connection to the server host */
			/********************************************************************************/

			String terminator;
			serverConnection = connectToServer();
			if (request.equals("GET")) {
				terminator = new String("Connection:close\n\n"); // WHY??????????????????????
			} else {
				terminator = new String("\n");
			}
			
			BufferedReader cookedIn = new BufferedReader(new InputStreamReader(rawIn), "UTF-8");
			BufferedWriter cookedOut = new BufferedWriter(new OutputStreamWriter(ostream));
			
			String line2 ;
			String responseLine;
			String contentLine;
			ByteArrayOutputStream headerBuf2 = new ByteArrayOutputStream(8096);
			PrintWriter headerWriter2 = new PrintWriter(headerBuf2); 
//			String resourcePath;
//			String filePath;
//			String urlPath;
			String noHopResponseHeaders = "======================================no Hop RESPONSEHEADERS to client================================\n";

			rawOut.write((headerBuf.toString()).getBytes()); /*
															 * Send request and
															 * header lines
															 */
			
			rawOut.write(terminator.getBytes()); /*
												 * No persistent connection for
												 * GET - keep life simple
												 */
//			while((line2 = cookedIn.readLine()) != null) {
//				System.out.println("[INFO]" + line2);
//			}
			
//			byte buffer[] = new byte[8192];
//			int count;
			/*
			 * Read data from Origin Server using rawIn.read. If there is data
			 * in rawIn, send it to client using ostream.write. count is used to
			 * save the bytes of data in rawIn ?
			 */

			
//			if (importantInformation.isContainContentLength) {
					if ((line2 = cookedIn.readLine()) != null) {
						responseLine = line2;
					} else {
						throw new IOException("End of buffer!");
					}

					//headerWriter.println(responseLine);
					//noHopResponseHeaders = noHopResponseHeaders + responseLine + "\n";

//					st = new StringTokenizer(responseLine);
//					String response = st.nextToken(); /* GET, POST, HEAD */

//					System.out.println("#DEBUG MESSAGE: Request Method =" + response);
					
					while (responseLine.length() != 0) {
						
						if (!(responseLine.startsWith("Connection")
								|| responseLine.startsWith("Keep-Alive")
								|| responseLine.startsWith("Proxy-Authenticate")
								|| responseLine.startsWith("Proxy-Authorization")
								|| responseLine.startsWith("TE")
								|| responseLine.startsWith("Trailers")
								|| responseLine.startsWith("Transfer-Encoding")
								|| responseLine.startsWith("Upgrade"))) {
							headerWriter2.println(responseLine);
							ostream.write(responseLine.getBytes());
							noHopResponseHeaders = noHopResponseHeaders + responseLine + "\n";
						} 
						
						//requestHeaders = requestHeaders + requestLine + "\n";
						//System.out.println(requestLine);
						if ((line2 = cookedIn.readLine()) != null) {
							responseLine = line2;
						} else {
							throw new IOException("End of buffer!");
						}
					}
					
					logger.write(noHopResponseHeaders);
					headerWriter2.println("\n\n");
					ostream.write("\n\n".getBytes());
//					String body = "";
					
					while ((line2 = cookedIn.readLine()) != null) {
						ostream.write(line2.getBytes());
//						body += line2;
//						contentLine = line2;
//						headerWriter2.println(responseLine);
					}
//					headerWriter2.flush();
					
//					ostream.write((headerBuf2.toString()).getBytes());
//					ostream.write(body.getBytes());
					
//					String hahahah = new String(buffer, StandardCharsets.US_ASCII);
//					String header = "";
//					while((header = cookedIn.readLine()) != null) {
//						cookedOut.write(header);
//						System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@"+ header);
//						logger.write("COOKEDOUT: " + header + "\n");
//					}
//					ostream.write(buffer, 0, count);
//				}
//			} else {
//				while ((count = rawIn.read(buffer, 0, 8192)) > -1) {
//					String header = "";
//					while((header = cookedIn.readLine()) != null) {
//						cookedOut.write(header);
//					}
//					ostream.write(buffer, 0, count);
//				}
//				String header = "";
//				while((header = cookedIn.readLine()) != null) {
//					cookedOut.write(header);
//				}
//				String content = "";
//				while((content = cookedIn.readLine()) != null) {
//					cookedOut.write(content);
//				}
//			}
			System.out.println("Client exit.");
			System.out
					.println("---------------------------------------------------");
			serverConnection.close();
			s.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public static void main(String args[]) throws IOException {
		if (args.length != 2)
			throw new RuntimeException("Syntax: ProxyServer configfile port-number");

		System.out.println("Starting on port " + args[1]);
		ServerSocket server = new ServerSocket(Integer.parseInt(args[1]));
		String configFile = args[0];
		Logger universalLogger = new Logger("src/log.txt");
		while (true) {
			System.out.println("Waiting for a client request");
			Socket client = server.accept();
			System.out.println("*********************************");
			System.out.println("Received request from "
					+ client.getInetAddress());
			ProxyServer c = new ProxyServer(universalLogger, client, configFile);
			c.start();
		}
	}
}