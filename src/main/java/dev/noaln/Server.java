package dev.noaln;

import dev.noaln.tools.ThreePair;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;

public class Server {

    private ServerSocket ss;
    private HashMap<String, ThreePair<BufferedReader,Socket,BufferedWriter>> connectionList = new HashMap<>();
    public Server(Integer port) throws IOException, InterruptedException {
        System.out.println("Configuring Server.. ");
        this.ss = new ServerSocket(port);
        if(port==0) { System.out.println("No port was specified in the command arguments, selecting next available port.. "); }
        String ip="",device=""; // make blank so no npe
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (iface.isLoopback() || !iface.isUp()) // get rid of loop back devices/addresses
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = ip.toLowerCase(); // just in case windows gets any smart ideas..
                    if(ip.contains("abcdef:")) continue; // filter out colon-hexadecimal to find ipv4
                    ip = addr.getHostAddress();
                    device = iface.getDisplayName();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Listening on device: "+device+" --> "+ip+":"+ss.getLocalPort());

    }
    //region Logical
    public Integer listen() throws IOException, InterruptedException {
        class HandshakeListener implements Runnable {
            private ServerSocket serverSocket;
            private Integer restartInterval = 5; //seconds to restart if process failed.
            public HandshakeListener(ServerSocket serverSocket) throws InterruptedException {
                System.out.println("Starting Client Handshake Thread..\n-- ");
                this.serverSocket = serverSocket;

            }
            @Override
            public void run() {
                try {
                    Socket s;
                    while((s = serverSocket.accept())!=null){
                        System.out.println("Established Client Connection to: "+s.getInetAddress().getHostAddress());
                        if(!connectionList.containsKey(s.getInetAddress().getHostAddress())) {
                            //Add new Client Connection, and init reader + writer.
                            BufferedReader brl = new BufferedReader(new InputStreamReader(s.getInputStream()));
                            BufferedWriter wrl = new BufferedWriter(new PrintWriter(s.getOutputStream()));
                            ThreePair<BufferedReader,Socket,BufferedWriter> pair = new ThreePair<>(brl,s,wrl);
                            //Must also store 'Client' here as a nested object in a tuple
                            connectionList.put(s.getInetAddress().getHostAddress(),pair);
                        } else continue;
                    }
                } catch (Exception e){
                    System.out.println("The Client Handshake Listener has experienced an internal error..\nThe server will no longer accept new client connections\nWas the Server Stopped with SIGINT?");
                }
            }
        }
        Thread t = new Thread(new HandshakeListener(getSs()));
        t.start();
        while(true){
            Thread.sleep(1000);
            for (String s : connectionList.keySet()) {
                String response;
                while((response = connectionList.get(s).left.readLine())!=null){
                    switch (response) {
                        case "quit" -> {
                            System.out.println("["+Main.time+" | "+connectionList.get(s).getMiddle().getInetAddress().getHostAddress()+"]\t Quit command received, server will now shut down and kill all remaining sessions.. ");
                            connectionList.get(s).getRight().write("Quit command received, server will now shut down and kill all remaining sessions.. ");

                            connectionList.values().forEach(x -> {
                                try {
                                    System.out.println("["+Main.time+" | "+connectionList.get(s).getMiddle().getInetAddress().getHostAddress()+"]\t Closing connection to client.. ");
                                    x.middle.close();
                                } catch (Exception ignored) {
                                }
                            });
                            try {
                                System.out.println("Closing Server Socket.. ");
                                this.ss.close();
                            } catch (Exception ignored) {}
                            t.stop();
                            System.exit(0);
                        }
                        case "ping" -> {
                            System.out.println("["+Main.time+" | "+connectionList.get(s).getMiddle().getInetAddress().getHostAddress()+"]\t Pong!");
                            connectionList.get(s).right.write("Pong!");
                        }
                        case "uptime" -> {
                            int difference = 0;
                            Timestamp time = new Timestamp(new Date().getTime());
                            System.out.println("["+Main.time+" | "+connectionList.get(s).getMiddle().getInetAddress().getHostAddress()+"]\t Uptime: " +
                                    (time.getSeconds() - Main.time.getSeconds()) + " second(s), since " + Main.time);

                            connectionList.get(s).getRight().write("Uptime: " + (difference = time.getSeconds() - Main.time.getSeconds()) +
                                    " " + ((difference <= 1) ? "second" : "seconds") +
                                    ", since " + Main.time);

                        }
                        default -> {
                            connectionList.get(s).getRight().write("Unknown command \"" + response + "\" ");
                            System.out.println("["+connectionList.get(s).getMiddle().getInetAddress().getHostAddress()+"] Unknown command \"" + response + "\" ");

                        }
                    }
                }
            }
        }
    }
    //endregion

    //region Getters
    public ServerSocket getSs() {
        return ss;
    }

    //endregion
    //region Setters

    public void setSs(ServerSocket ss) {
        this.ss = ss;
    }

    //endregion
}
