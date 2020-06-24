package sample;

import net.sf.cglib.asm.$ClassWriter;

import java.io.*;
import java.net.Socket;

public class ServerTask implements Runnable{
    private Socket conn;
    private File htmlFile;
    private FileInputStream fin;
    private OutputStream out;
    private String path = "./src/html/";

    public ServerTask(Socket conn){
        this.conn = conn;
    }

    @Override
    public void run() {
        try {
            String url = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
            String method = url.split(" ")[0];
            String source = url.split(" ")[1];
            if (source.equals("/")) {
                source = "index.html";
            } else if (source.endsWith("?")){
                source = source.substring(0, source.length() - 1);
            }
            System.out.println(method + " " + source);
            String newPath = path + source;
            htmlFile = new File(newPath);
            if (htmlFile.isDirectory()){
                path = newPath + "/";
                newPath = newPath + "/index.html";
                htmlFile = new File(newPath);
            }
            fin = new FileInputStream(htmlFile);
            out = conn.getOutputStream();
            out.write("HTTP/1.1 200 OK\r\n".getBytes());
            out.write("content-type:text/html\r\n".getBytes());
            out.write("set-cookie:id=123456\r\n".getBytes());
            out.write(("content-length:" + htmlFile.length() + "\r\n").getBytes());
            out.write("\r\n".getBytes());
            out.write(fin.readAllBytes());
            out.flush();
            out.close();
            fin.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
