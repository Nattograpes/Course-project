package sample;

import java.net.ServerSocket;
import java.net.Socket;

public class MyServer {

    public void start(){
        try {
            ServerSocket serverSocket = new ServerSocket(2020);
            while (true){
                Socket conn = serverSocket.accept();
                System.out.println("Connect: " + conn.getInetAddress().getHostName());
                new ServerTask(conn).run();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MyServer().start();
    }
}
