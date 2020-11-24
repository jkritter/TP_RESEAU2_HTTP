
package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Web server.
 *
 * @author Julien KRITTER & Armand LEULLIER
 */
public class WebServer {

    /**
     * WebServer constructor.
     */
    protected void start() {
        ServerSocket s;

        System.out.println("Webserver starting up on port 80");
        System.out.println("(press ctrl-c to exit)");
        try {
            // create the main server socket
            s = new ServerSocket(3000);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return;
        }

        System.out.println("Waiting for connection");
        for (; ; ) {
            try {
                // wait for a connection
                Socket remote = s.accept();
                // remote is now the connected socket
                System.out.println("Connection, sending data.");
                BufferedInputStream in = new BufferedInputStream(remote.getInputStream());
                PrintWriter out = new PrintWriter(remote.getOutputStream());

                String header = new String();

                int bcur = '\0', bprec = '\0';
                boolean newline = false;
                while((bcur = in.read()) != -1 && !(newline && bprec == '\r' && bcur == '\n')) {
                    if(bprec == '\r' && bcur == '\n') {
                        newline = true;
                    } else if(!(bprec == '\n' && bcur == '\r')) {
                        newline = false;
                    }
                    bprec = bcur;
                    header += (char) bcur;
                }

                String[] request = header.split(" ");
                String requestType = request[0];
                String requestValue = "";
                if (request.length > 1) {
                    requestValue = "." + request[1];
                }

                if (requestType.equals("GET")) {
                    httpGet(out, requestValue);
                } else if (requestType.equals("POST")) {
                    httpPost(out, requestValue);
                } else if (requestType.equals("HEAD")) {
                    httpHead(out);
                } else if (requestType.equals("PUT")) {
                    httpPut(in, out, requestValue);
                } else if (requestType.equals("DELETE")) {
                    httpDelete(out, requestValue);
                } else {
                    out.println(makeHeader("501 Not Implemented", requestValue));
                    out.flush();
                }
                remote.close();
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }

    /**
     * GET request.
     *
     * @param out out stream
     * @param filename file to get
     */
    public void httpGet(PrintWriter out, String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename.replace("?=", "")));
            out.println(makeHeader("200 OK", filename));
            char[] buf = new char[256];
            int bytesRead;
            while ((bytesRead = reader.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
            out.flush();
            reader.close();

        } catch (Exception e) {
            out.println(makeHeader("404 Not Found", filename));
            out.flush();
            System.out.println(e);
        }
        out.flush();
    }

    /**
     * POST request.
     *
     * @param out out stream
     * @param filename file to post
     */
    public void httpPost(PrintWriter out, String filename) {
        System.out.println("post");
    }

    /**
     * HEAD request.
     *
     * @param out out stream
     */
    public void httpHead(PrintWriter out) {
        out.println(makeHeader("200 OK", null));
        out.flush();
    }

    /**
     * PUT request.
     *
     * @param in in stream
     * @param out out stream
     * @param filename file to put
     */
    public void httpPut(BufferedInputStream in, PrintWriter out, String filename) {
        try {
            File resource = new File(filename.replace("?=", ""));
            boolean fileExisted = resource.exists();
            BufferedWriter fileOut = new BufferedWriter(new FileWriter(resource));
            /*String line = ".";
            while (line != null && line!= "") {
                line = in.readLine();
                fileOut.write(line);
            }*/

            char[] buf = new char[256];
            int bytesRead;
            while (in.available() > 0) {
                bytesRead = in;
                out.write(buf, 0, bytesRead);
            }
            fileOut.flush();
            fileOut.close();

            System.out.println("A");
            if(fileExisted) {
                out.println(makeHeader("204 No Content", filename));
            } else {
                out.println(makeHeader("201 Created", filename));
            }
            out.flush();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * DELETE request.
     *
     * @param out out stream
     * @param filename file to delete
     */
    public void httpDelete(PrintWriter out, String filename) {
        try {
            File file = new File(filename.replace("?=", ""));
            if (file.delete()) {
                out.println(makeHeader("200 OK", filename));
            } else {
                out.println(makeHeader("404 Not Found", filename));
            }
            out.flush();
        } catch (Exception e) {
            System.out.println(e);
            out.println(makeHeader("500 Internal Error", filename));
            out.flush();
        }
    }

    /**
     * Response header.
     *
     * @param status response status
     * @return header
     */
    protected String makeHeader(String status, String filename) {
        String header = "HTTP/1.0 " + status + "\r\n";
        if(filename.endsWith(".html"))
            header += "Content-Type: text/html\r\n";
        else if(filename.endsWith(".jpg") || filename.endsWith(".jpeg"))
            header += "Content-Type: image/jpg\r\n";
        else if(filename.endsWith(".png"))
            header += "Content-Type: image/png\r\n";
        else if(filename.endsWith(".mp4"))
            header += "Content-Type: video/mp4\r\n";
        else if(filename.endsWith(".mp3"))
            header += "Content-Type: audio/mp3\r\n";
        else if(filename.endsWith(".css"))
            header += "Content-Type: text/css\r\n";
        else
            header += "Content-Type: text/html\r\n";
        header += "Server: Bot\r\n";
        header += "\r\n";
        System.out.println("Header :" + header);
        return header;
    }

    /**
     * Start the application.
     *
     * @param args Command line parameters are not used.
     */
    public static void main(String args[]) {
        WebServer ws = new WebServer();
        ws.start();
    }
}
