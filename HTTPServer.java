/*
 * 
 * Darian Marvel - 4/30/2019
 * Making a website using Java.
 * May eventually turn into a website engine of some kind,
 * or just simple hosting software, or both.
 * 
 */

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

public class HTTPServer
{
    public static void main(String[] args) {

        new HTTPServer();

    }
    
    HttpServer server;
    
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
    
    public void out(String s) {
        System.out.println("[STANDARD] " + s);
    }
    
    public void debug(String s) {
        System.out.println("[DEBUG] " + s);
    }
    
    public static void sleep(int x) {
        try{
            Thread.sleep(x);
        }catch(Exception e) {
            
        }
    }
}