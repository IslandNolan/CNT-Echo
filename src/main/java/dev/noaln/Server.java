package dev.noaln;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Server {

    private final ServerSocket ss;
    private final HashMap<String, MutablePair<BufferedReader,Socket>> connectionList = new HashMap<>();
    public Server(Integer port) throws IOException {
        System.out.println("Configuring Server.. ");
        this.ss = new ServerSocket(port);
        if(port==0) { System.out.println("No port was specified in the command arguments, selecting next available port.. "); }
        String ip="",device=""; // make blank so no npe
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) // get rid of loop back devices/addresses
                    continue;

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = ip.toLowerCase(); // just in case windows gets any smart ideas.
                    if(ip.contains("abc-def:")) continue; // filter out colon-hexadecimal to find ipv4
                    ip = addr.getHostAddress();
                    device = networkInterface.getDisplayName();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Listening on device: "+device+" --> "+ip+":"+ss.getLocalPort());

    }
    //region Logical

    public void logMessage(Socket s,String message){
        try {
            StringBuilder consoleMessage = new StringBuilder("[" + Main.time + " | " + s.getInetAddress().getHostAddress() + "] ");
            while(consoleMessage.length()!=60) { consoleMessage.append(" "); }
            System.out.println(consoleMessage+message);

        } catch (Exception ignored){
            System.out.println("[ERROR] Failed to send response to Client.. ");
        }
    }
    public void sendMessage(String key,String message){
        try {
            connectionList.get(key).right.getOutputStream().write(("     "+message+"\n>> ").getBytes());
            StringBuilder consoleMessage = new StringBuilder("[" + Main.time + " | " + connectionList.get(key).right.getInetAddress().getHostAddress() + "]    ");
            while(consoleMessage.length()!=60) { consoleMessage.append(" "); }
            System.out.println(consoleMessage+message);

        } catch (Exception ignored){
            System.out.println("[ERROR] Failed to send response to Client: "+key);
        }
    }
    public void listen() throws IOException, InterruptedException {
        class HandshakeListener implements Runnable {

            private final ServerSocket serverSocket;
            public HandshakeListener(ServerSocket serverSocket) {
                System.out.println("Starting Client Handshake Thread..\n-- ");
                this.serverSocket = serverSocket;
            }
            @Override
            public void run() {
                try {
                    Socket s;
                    while((s = serverSocket.accept())!=null){
                        //noinspection BusyWait
                        Thread.sleep(1000);
                        logMessage(s,("Established Client Connection to: "+s.getInetAddress().getHostAddress()));
                        s.getOutputStream().write((">> ").getBytes());
                        if(!connectionList.containsKey(s.getInetAddress().getHostAddress())) {
                            //Add new Client Connection, and init reader + writer.
                            BufferedReader brl = new BufferedReader(new InputStreamReader(s.getInputStream()));
                            MutablePair<BufferedReader,Socket> pair = new MutablePair<>(brl,s);
                            //Must also store 'Client' here as a nested object in a tuple
                            connectionList.put(s.getInetAddress().getHostAddress(),pair);
                        }
                        else {
                            //Reject duplicate clients later
                            s.close();
                        }
                    }

                }
                catch (SocketException ignored){}
                catch (Exception e){
                    e.printStackTrace();
                    System.out.println("""
                            The Client Handshake Listener has experienced an internal error..
                            The server will no longer accept new client connections
                            Was the Server Stopped with SIGINT?""");
                }
            }
        }
        Thread handshake = new Thread(new HandshakeListener(getSs()));
        handshake.start();
        while(true){
            //noinspection BusyWait
            Thread.sleep(100);
            Iterator<String> itr = connectionList.keySet().iterator();
            String s = (itr.hasNext() ? itr.next() : null);
            while(s != null) {
                try {
                    String response, hostAddress = connectionList.get(s).right.getInetAddress().getHostAddress();
                    while (connectionList.containsKey(s) && (response = connectionList.get(s).left.readLine()) != null) {
                        switch (response) {
                            case "disconnect","quit" -> {
                                logMessage(connectionList.get(s).right, "Disconnecting Client.. ");
                                connectionList.get(s).right.close();
                                connectionList.get(s).left.close(); //Potential Issue Here..
                                itr.remove();
                                s = null;
                            }
                            case "shutdown", "exit" -> {
                                sendMessage(hostAddress,
                                        "Server Shutting down.. ");
                                String finalS = s;
                                connectionList.values().forEach(x -> {
                                    try {
                                        logMessage(connectionList.get(finalS).right,"Closing connection to client.. ");
                                        x.right.close();
                                    } catch (Exception ignored) {
                                    }
                                });
                                try {
                                    handshake.interrupt();
                                    System.out.println("Closing Server Socket.. ");
                                    this.ss.close();
                                } catch (Exception ignored) {
                                }
                                System.exit(0);
                            }
                            case "ping" ->
                                sendMessage(hostAddress, "Pong!");

                            case "uptime" -> {
                                LocalDateTime time = LocalDateTime.now();
                                Duration diff = Duration.between(Main.time, time);
                                long days = diff.toDays();
                                long hours = diff.toHours() - 24 * days;
                                long months = diff.toMinutes() - 60 * diff.toHours();
                                long seconds = diff.getSeconds() - 60 * diff.toMinutes();
                                String diffString = ("Uptime: " + days + "d " + hours + "h " + months + "m " + seconds + "s, since " + Main.time);
                                sendMessage(hostAddress, diffString);

                            }
                            default ->
                                sendMessage(hostAddress, ("Unknown command \"" + response + "\" "));
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if(!itr.hasNext()) { itr = connectionList.keySet().iterator(); }
                else {
                    s = itr.next();
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
}
