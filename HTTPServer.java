/*
 *
 * Darian Marvel - 4/30/2019
 * Making a website using Java.
 * May eventually turn into a website engine of some kind,
 * simple hosting software, or both.
 *
 */

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

import Logging.Logger;

public class HTTPServer
{
    public static void main(String[] args) {

        System.out.println("Running Server...");

        HTTPServer serv = new HTTPServer();

        serv.out("Found " + args.length + " arguments");

        for(String s : args) serv.passArgument(s);
    }

    HttpServer server;

    Logger toLog = new Logger("Server", "standard");
    Logger debugLog = new Logger("Server", "debug");
    Logger connectLog = new Logger("Server", "connect");

    public HTTPServer() {
        try{
            out("Starting...");

            server = HttpServer.create( new InetSocketAddress(80) , 0);
            server.createContext( "/", new HTTPHandler(this) );
            server.start();

            out("Started");
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void passArgument(String s) {
      out("Handling argument " + s);
      if( s.equalsIgnoreCase("nosize") ) {
        HTTPHandler.fileSizes=false;
        HTTPHandler.folderSizes=false;
      }
    }

    public void out(String s) {
        System.out.println("[STANDARD] " + s);

        toLog.log(s);
    }

    public void debug(String s) {
        System.out.println("[DEBUG] " + s);

        debugLog.log(s);
    }

    public void connectLog(String s) {
        connectLog.log(s);
    }

    public void log(String s) {
        debugLog.log("[LOG] " + s);
    }

    public static void sleep(int x) {
        try{
            Thread.sleep(x);
        }catch(Exception e) {

        }
    }
}
