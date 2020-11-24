
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
                BufferedOutputStream out = new BufferedOutputStream(remote.getOutputStream());

                String header = new String();

                int currentByte = '\0', previousByte = '\0';
                boolean newline = false;
                while ((currentByte = in.read()) != -1 && !(newline && previousByte == '\r' && currentByte == '\n')) {
                    if (previousByte == '\r' && currentByte == '\n') {
                        newline = true;
                    } else if (!(previousByte == '\n' && currentByte == '\r')) {
                        newline = false;
                    }
                    previousByte = currentByte;
                    header += (char) currentByte;
                }

                String[] request = header.split(" ");
                String requestType = request[0];
                String requestValue = "";
                if (request.length > 1) {
                    requestValue = "./resources" + request[1].replace("?=", "");
                }
                if (requestType.equals("GET")) {
                    httpGet(out, requestValue);
                } else if (requestType.equals("PUT")) {
                    httpPut(in, out, requestValue);
                } else if (requestType.equals("POST")) {
                    httpPost(in, out, requestValue);
                } else if (requestType.equals("HEAD")) {
                    httpHead(out);
                } else if (requestType.equals("DELETE")) {
                    httpDelete(out, requestValue);
                } else {
                    out.write(makeHeader("501 Not Implemented", requestValue).getBytes());
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
     * @param out      output stream
     * @param filename file to get
     */
    public void httpGet(BufferedOutputStream out, String filename) {
        try {
            File resource = new File(filename);
            boolean fileExisted = resource.exists();
            if (fileExisted) {
                out.write(makeHeader("200 OK", filename).getBytes());
            } else {
                out.write(makeHeader("404 Not Found", filename).getBytes());
            }
            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(resource));

            byte[] buffer = new byte[256];
            int nbRead;
            while ((nbRead = reader.read(buffer)) != -1) {
                out.write(buffer, 0, nbRead);
            }
            reader.close();
            out.flush();
        } catch (Exception e) {
            try {
                out.write(makeHeader("500 Internal Error", filename).getBytes());
                out.flush();
                System.out.println(e);
            } catch (Exception e2) {
                System.out.println(e2);
            }
        }
    }

    /**
     * POST request.
     *
     * @param in       input stream
     * @param out      output stream
     * @param filename file to post
     */
    public void httpPost(BufferedInputStream in, BufferedOutputStream out, String filename) {
        try {
            File resource = new File(filename);
            boolean fileExisted = resource.exists();

            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource, fileExisted));

            byte[] buffer = new byte[256];
            while (in.available() > 0) {
                int nbRead = in.read(buffer);
                fileOut.write(buffer, 0, nbRead);
            }
            fileOut.flush();
            fileOut.close();

            if (fileExisted) {
                out.write(makeHeader("200 OK", filename).getBytes());
            } else {
                out.write(makeHeader("201 Created", filename).getBytes());
            }
            out.flush();
        } catch (Exception e) {
            try {
                out.write(makeHeader("500 Internal Error", filename).getBytes());
                out.flush();
                System.out.println(e);
            } catch (Exception e2) {
                System.out.println(e2);
            }
        }
    }

    /**
     * HEAD request.
     *
     * @param out output stream
     */
    public void httpHead(BufferedOutputStream out) {
        try {
            out.write(makeHeader("200 OK", null).getBytes());
            out.flush();
        } catch (Exception e) {
            try {
                out.write(makeHeader("500 Internal Error", null).getBytes());
                out.flush();
                System.out.println(e);
            } catch (Exception e2) {
                System.out.println(e2);
            }
        }
    }

    /**
     * PUT request.
     *
     * @param in       input stream
     * @param out      output stream
     * @param filename file to put
     */
    public void httpPut(BufferedInputStream in, BufferedOutputStream out, String filename) {
        try {
            File resource = new File(filename);
            boolean fileExisted = resource.exists();

            BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(resource));

            byte[] buffer = new byte[256];
            while (in.available() > 0) {
                int nbRead = in.read(buffer);
                fileOut.write(buffer, 0, nbRead);
            }
            fileOut.flush();
            fileOut.close();

            if (fileExisted) {
                out.write(makeHeader("204 No Content", filename).getBytes());
            } else {
                out.write(makeHeader("201 Created", filename).getBytes());
            }
            out.flush();
        } catch (Exception e) {
            try {
                out.write(makeHeader("500 Internal Error", filename).getBytes());
                out.flush();
                System.out.println(e);
            } catch (Exception e2) {
                System.out.println(e2);
            }
        }
    }

    /**
     * DELETE request.
     *
     * @param out      output stream
     * @param filename file to delete
     */
    public void httpDelete(BufferedOutputStream out, String filename) {
        try {
            File file = new File(filename.replace("?=", ""));
            if (file.delete()) {
                out.write(makeHeader("200 OK", filename).getBytes());
            } else {
                out.write(makeHeader("404 Not Found", filename).getBytes());
            }
            out.flush();
        } catch (Exception e) {
            try {
                out.write(makeHeader("500 Internal Error", filename).getBytes());
                out.flush();
                System.out.println(e);
            } catch (Exception e2) {
                System.out.println(e2);
            }
        }
    }

    /**
     * Response header.
     *
     * @param status   response status
     * @param filename file name
     * @return header
     */
    protected String makeHeader(String status, String filename) {
        String header = "HTTP/1.0 " + status + "\r\n";
        if (filename != null) {
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))
                header += "Content-Type: image/jpg\r\n";
            else if (filename.endsWith(".png"))
                header += "Content-Type: image/png\r\n";
            else if (filename.endsWith(".mp4"))
                header += "Content-Type: video/mp4\r\n";
            else if (filename.endsWith(".mp3"))
                header += "Content-Type: audio/mp3\r\n";
            else if (filename.endsWith(".css"))
                header += "Content-Type: text/css\r\n";
            else
                header += "Content-Type: text/html\r\n";
        }
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
