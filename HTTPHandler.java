/*
 * 
 * Darian Marvel - 4/30/2019
 * Making a website using Java.
 * This class handles requests sent to the website.
 * 
 */

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

public class HTTPHandler implements HttpHandler {

    HTTPServer server;

    public HTTPHandler(HTTPServer serv) {
        server = serv;
    }

    public void handle(HttpExchange t) throws IOException {
        try{
            handle2(t);
        }catch(Exception e) {
            debug("Exception on exchange");
            e.printStackTrace();
            t.close();
        }
    }

    public void handle2(HttpExchange t) throws IOException {
        //HTTPServer.sleep(100);

        InetSocketAddress addr = t.getRemoteAddress();

        out( "Request from " + addr.toString()  );

        URI uri = t.getRequestURI();

        String path = uri.getPath();

        out("Request: " + path);

        while( path.indexOf("/") == 0 ) {
            path = path.substring(1);
        }

        while( path.indexOf("\\") == 0 ) {
            path = path.substring(1);
        }
        
        if( path.indexOf("..") != -1 ) {
            t.close();
            return;
        }

        t.sendResponseHeaders(200, 0);

        Headers h = t.getResponseHeaders();
        h.set("Content-Type", "text/plain");

        OutputStream os = t.getResponseBody();

        writeLine(os, "Java Site " + uri.toString() );

        out(path);

        File file = new File( "Files/" + path );
        if( !file.exists() ) {
            writeLine(os, "File not found");
            t.close();
            return;
        }
        else if( file.isDirectory() ) {
            File[] files = file.listFiles();
            
            String name = file.getName();
            if( uri.toString().equals("/") ) name = "";
            
            for(File f : files) {
                if( !f.isDirectory() ) writeLine( os, "File: " + getLink( name + "/" + f.getName(), f.getName() ) );
                else writeLine( os, "Folder: " + getLink( name + "/" + f.getName(), f.getName() ) );
            }
        }
        else {
            
            String style ="<pre style=\"word-wrap: break-word; white-space: pre-wrap\">";
            
            os.write( style.getBytes() );

            FileInputStream fs;
            try{
                fs = new FileInputStream( file );
                final byte[] buffer = new byte[0x10000];
                int count = 0;
                while ( ( count = fs.read(buffer) ) >= 0) {
                    os.write(buffer, 0, count);
                    os.flush();
                }

                fs.close();
                
                write(os, "</pre>");
            }catch(Exception e) {
                writeLine(os, "Could not access the requested file");
                //writeLine(os, e.getMessage() );
                e.printStackTrace();
            }

        }

        os.flush();

        //os.close();
        t.close();
    }
    
    public void write(OutputStream os, String s) throws IOException{
        os.write( s.getBytes() );
    }

    public void writeLine(OutputStream os, String s) throws IOException {
        String toWrite = "<p> " + s + " </p>";
        os.write( toWrite.getBytes() );
    }
    
    public String getLink(String link, String say) {
        String toRet = "<a href=\"";
        toRet += link + "\">";
        toRet += say + "</a>";
        return toRet;
    }

    public void out(String s) {
        server.out("[HTTPHANDLER] " + s);
    }

    public void debug(String s) {
        server.debug("[HTTPHANDLER] " + s);
    }
}