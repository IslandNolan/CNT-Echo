package dev.noaln;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class Client {


    //send packets on port?
    private Integer port;
    private BufferedReader br;
    private Scanner sc = new Scanner(System.in);
    private Socket s;

    public Client(String ip,int port) throws IOException, InterruptedException {
        s = new Socket("127.0.0.1",port);
        br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);

        System.out.println("Sending.. ");
        out.println("ping");
        out.println("date");
        out.println("uptime");
        out.println("memory");
        out.println("exit");
        br.lines().forEach(System.out::println);
    }
}
