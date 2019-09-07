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

  //Site-Wide style setup (For default pages and basic looks, does not effect file display)
  static final String siteStyle = "" //Readability Counts!
                                  + "<style type = \"text/css\">"
                                  + "body {"
                                  + "background-color: rgb(20, 20, 20);"
                                  + "}"
                                  + "p {"
                                  + "color: rgb(255,255,255);"
                                  + "background-color: rgb(50,50,50);"
                                  + "}"
                                  + "h1 {"
                                  + "color: rgb(255,255,255);"
                                  + "background-color: DarkRed;"
                                  + "text-align: center;"
                                  + "}"
                                  + "h2 {"
                                  + "color: rgb(255,255,255);"
                                  + "background-color: DarkCyan;"
                                  + "text-align: center;"
                                  + "}"
                                  + "</style>"
                                  ;

    static final int bufferSize = 0x10000;

    //Boolean Config
    public static boolean fileSizes = true;
    public static boolean folderSizes = true;

    HTTPServer server;
    String baseDir;

    public HTTPHandler(HTTPServer serv) {
        server = serv;

        out("Setting up handler...");

        baseDir = "Files";
        new File(baseDir).mkdir();

        out("Using buffer size " + bufferSize);
        out("Using baseDir " + baseDir);
    }

    public String getBaseDir() {
      return baseDir;
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

    public void handle2(HttpExchange t) throws Exception {
        //HTTPServer.sleep(100);

        InetSocketAddress addr = t.getRemoteAddress();

        connectLog( "Request from " + addr.toString()  );

        URI uri = t.getRequestURI();

        String path = uri.getPath();

        connectLog("Request: " + path);

        while( path.indexOf("/") == 0 ) {
            path = path.substring(1);
        }

        while( path.indexOf("\\") == 0 ) {
            path = path.substring(1);
        }

        //Prevent path traversal attack (Linux, has no effect on windows)
        if( path.indexOf("..") > -1 ) {
            t.close();
            return;
        }

        t.sendResponseHeaders(200, 0);

        Headers h = t.getResponseHeaders();

        OutputStream os = t.getResponseBody();

        connectLog(path);

        if( !new File(baseDir).exists() ) {
            h.set("Content-Type", "text/plain");
            os.write( siteStyle.getBytes() );

            writeh1(os, "Site Error");
            writeh2(os, "Base directory does not exist");
            t.close();
            throw new Exception("Base directory does not exist");
            //return;
        }

        File file = new File( baseDir + "/" + path );
        connectLog( file.getCanonicalPath() );
        if( !file.exists() ) {
            h.set("Content-Type", "text/plain");
            os.write( siteStyle.getBytes() );

            writeh1(os, "File Not Found");
            writeh2(os, "Does Not Exist");
            t.close();
            return;
        }
        else if( file.isDirectory() ) {
            h.set("Content-Type", "text/plain");

            File[] files = file.listFiles();

            os.write( siteStyle.getBytes() );

            writeh1(os, "Files - /" + file.getPath() );
            if(folderSizes) writeh2(os, "Total Size: " + sizeToString(file));

            //Fix that wierd thing...
            //references target file by <current folder> / <file>
            //because otherwise the current directory gets messed up.
            String name = file.getName();
            if( uri.toString().equals("/") ) name = "";

            for(File f : files) {
                //If it is a hidden file, don't show
                //Hidden files have names that start with a "."
                if(f.getName().indexOf(".") == 0) continue;

                //List the file, and say whether or not it is a folder
                //Also, say how large the file is.
                if( !f.isDirectory() ) writeLine( os, "File: " + getLink( name + "/" + f.getName(), f.getName() ) + " " + sizeToString(f) );
                else writeLine( os, "Folder: " + getLink( name + "/" + f.getName(), f.getName() ) + " " + sizeToString(f) );
            }
        }
        else {

            if( file.getName().indexOf(".") == -1) {
                serveText(file, os, t);
            }
            else {
                String ext = file.getName().substring( file.getName().lastIndexOf(".") );
                //Possibly make method "endsWith" instead?

                //out(ext);

                if( ext.equalsIgnoreCase(".png") ) {
                    servePNG(file, os, t);
                }
                else if( ext.equalsIgnoreCase(".jpg") ) {
                    serveJPEG(file, os, t);
                }
                else if( ext.equalsIgnoreCase(".gif") ) {
                    serveGIF(file, os, t);
                }
                else if (ext.equalsIgnoreCase(".html") ) {
                    serveHTML(file, os, t);
                }
                else if(ext.equalsIgnoreCase(".zip") || ext.equalsIgnoreCase(".pdf") || ext.equalsIgnoreCase(".rar") || ext.equalsIgnoreCase(".gz")) {
                    serveDown(file, os, t);
                }
                else serveText(file, os, t);
            }

        }
        os.flush();
        //os.close();
        t.close();
    }

    public long getSize(File file) {
        if(!file.exists()) {
            return -1; //-1 is more helpful than 0
        }

        long len = 0;

        if(file.isDirectory()) {
            File[] files = file.listFiles();
            for(File f : files) {
                len += getSize(f);
            }
            return len;
        }

        len = file.length();

        return len;
    }

    public String sizeToString(File f) {
        if(f.isDirectory() && !folderSizes) return "";
        if(!f.isDirectory() && !fileSizes) return "";

        return sizeToString( getSize(f) );
    }

    public String sizeToString(long num) {
        String toRet = "";

        String size = "B";

        if(num >= 1024) {
            num /= 1024;
            size = "KB";
        }

        if(num >= 1024) {
            num /= 1024;
            size = "MB";
        }

        if(num >= 1024) {
            num /= 1024;
            size = "GB";
        }

        //Really should NEVER be needed, but...
        //...here we are.
        if(num >= 1024) {
            num /= 1024;
            size = "TB";
        }

        toRet = num + " " + size;

        return toRet;
    }

    public void servePNG(File file, OutputStream os, HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "image/png");

        FileInputStream fs;
        try{
            fs = new FileInputStream( file );
            final byte[] buffer = new byte[bufferSize];
            int count = 0;
            while ( ( count = fs.read(buffer) ) >= 0) {
                os.write(buffer, 0, count);
                os.flush();
            }

            fs.close();
        }catch(Exception e) {
            writeLine(os, "Could not access the requested file");
            //writeLine(os, e.getMessage() );
            e.printStackTrace();
        }
    }

    public void serveJPEG(File file, OutputStream os, HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "image/jpeg");

        FileInputStream fs;
        try{
            fs = new FileInputStream( file );
            final byte[] buffer = new byte[bufferSize];
            int count = 0;
            while ( ( count = fs.read(buffer) ) >= 0) {
                os.write(buffer, 0, count);
                os.flush();
            }

            fs.close();
        }catch(Exception e) {
            writeLine(os, "Could not access the requested file");
            //writeLine(os, e.getMessage() );
            e.printStackTrace();
        }
    }

    public void serveGIF(File file, OutputStream os, HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "image/gif");

        FileInputStream fs;
        try{
            fs = new FileInputStream( file );
            final byte[] buffer = new byte[bufferSize];
            int count = 0;
            while ( ( count = fs.read(buffer) ) >= 0) {
                os.write(buffer, 0, count);
                os.flush();
            }

            fs.close();
        }catch(Exception e) {
            writeLine(os, "Could not access the requested file");
            //writeLine(os, e.getMessage() );
            e.printStackTrace();
        }
    }

    public void serveDown(File file, OutputStream os, HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "application/force-download");
        t.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

        FileInputStream fs;
        try{
            fs = new FileInputStream( file );
            final byte[] buffer = new byte[bufferSize];
            int count = 0;
            while ( ( count = fs.read(buffer) ) >= 0) {
                os.write(buffer, 0, count);
                os.flush();
            }

            fs.close();
        }catch(Exception e) {
            writeLine(os, "Could not access the requested file");
            //writeLine(os, e.getMessage() );
            e.printStackTrace();
        }
    }

    public void serveHTML(File file, OutputStream os, HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/html");

        FileInputStream fs;
        try{
            fs = new FileInputStream( file );
            final byte[] buffer = new byte[bufferSize];
            int count = 0;
            while ( ( count = fs.read(buffer) ) >= 0) {
                os.write(buffer, 0, count);
                os.flush();
            }

            fs.close();
        }catch(Exception e) {
            writeLine(os, "Could not access the requested file");
            //writeLine(os, e.getMessage() );
            e.printStackTrace();
        }
    }

    public void serveText(File file, OutputStream os, HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/plain");

        String style ="<pre style=\"word-wrap: break-word; white-space: pre-wrap\">";

        os.write( style.getBytes() );

        FileInputStream fs;
        try{
            fs = new FileInputStream( file );
            final byte[] buffer = new byte[bufferSize];
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

    public void write(OutputStream os, String s) throws IOException{
        os.write( s.getBytes() );
    }

    public void writeLine(OutputStream os, String s) throws IOException {
        String toWrite = "<p> " + s + " </p>";
        os.write( toWrite.getBytes() );
    }

    public void writeh1(OutputStream os, String s) throws IOException {
        String toWrite = "<h1> " + s + " </h1>";
        os.write( toWrite.getBytes() );
    }

    public void writeh2(OutputStream os, String s) throws IOException {
        String toWrite = "<h2> " + s + " </h2>";
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

    public void log(String s) {
        server.log("[HTTPHANDLER] " + s);
    }

    public void connectLog(String s) {
        server.connectLog("[HTTPHANDLER] " + s);
    }
}
