import java.net.*;
import java.io.*;

public class HTTPhead  {

  protected String host, file;
  protected String urlString;
  protected int port;
  protected DataInputStream in;
  protected DataOutputStream out;

  public HTTPhead (String textURL) throws IOException {
    urlString = textURL;
    Socket socket = null;
    parseURL (textURL);
    socket = connectToServer();
    try {
      MakeGetRequest ();
    } finally {
      socket.close ();
    }
  }

  protected void parseURL (String textURL) throws MalformedURLException {
    URL url = new URL (textURL);
    host = url.getHost ();
    port = url.getPort ();
    if (port == -1)
      port = 80;
    file = url.getFile ();
  }

  protected Socket connectToServer () throws IOException {
    System.out.println ("Connecting to " + host + ":" + port + "..");
    Socket socket = new Socket (host, port);
    System.out.println ("Connected.");

    OutputStream rawOut = socket.getOutputStream ();
    InputStream rawIn = socket.getInputStream ();
    BufferedOutputStream buffOut = new BufferedOutputStream (rawOut);
    out = new DataOutputStream (buffOut);
    in = new DataInputStream (rawIn);

    return socket;
  }

  protected void MakeGetRequest () throws IOException {
    System.out.println ("Sending request..");
    out.writeBytes ("HEAD " + urlString + " HTTP/1.1\n");
    out.writeBytes ("HOST: " + host +"\n");
    out.writeBytes ("CONNECTION: close "  +"\n\n");
    out.flush ();
    
    System.out.println ("Waiting for response..");
    String input;
    while ((input = in.readLine ()) != null)
      System.out.println (input);
  }

  public static void main (String args[]) throws IOException {
    DataInputStream keyboard = new DataInputStream (System.in);
    while (true) {
      String textURL;
      System.out.print ("Enter a URL: ");
      System.out.flush ();
      if ((textURL = keyboard.readLine ()) == null)
        break;

      try {
        new HTTPhead (textURL);
      } catch (IOException ex) {
        ex.printStackTrace ();
        continue;
      }

      System.out.println ("- OK -");
    }
    System.out.println ("exit");
  }
}
