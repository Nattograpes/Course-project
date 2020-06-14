package sample;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class test {
    public test() throws Exception {
        URL url = new URL("http://google.com");
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        connection.connect();
    }

    public static void main(String[] args) {
        try {
            new test();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
