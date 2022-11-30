package dev.noaln;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.*;
import java.lang.management.MemoryManagerMXBean;
import java.net.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Server {

    private Boolean forceLocalConnection;
    private final ServerSocket ss;
    private final HashMap<String, MutablePair<BufferedReader,Socket>> connectionList = new HashMap<>();
    public Server(Integer port,Boolean forceLocalConnection) throws IOException {
        System.out.println("Configuring Server.. ");
        this.ss = new ServerSocket(port);
        this.forceLocalConnection = forceLocalConnection;
        if(port==0) { System.out.println("No port was specified in the command arguments, selecting next available port.. "); }
        String ip="127.0.0.1",device="Loopback"; // make blank so no npe
        if(!this.forceLocalConnection) {
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();

                    if (networkInterface.isLoopback() || !networkInterface.isUp()) // get rid of loop back devices/addresses
                        continue;

                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        ip = ip.toLowerCase(); // just in case windows gets any smart ideas.
                        if (ip.contains("abc-def:")) continue; // filter out colon-hexadecimal to find ipv4
                        ip = addr.getHostAddress();
                        device = networkInterface.getDisplayName();
                    }
                }
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
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
    public void sendMessage(String key,String message,Boolean omitPrefix,Boolean omitHost){
        try {
            connectionList.get(key).right.getOutputStream().write(("     "+message+((!omitPrefix) ? "\n>> " : "\n")).getBytes());
            StringBuilder consoleMessage = new StringBuilder("[" + Main.time + ((omitHost) ? (" | " + connectionList.get(key).right.getInetAddress().getHostAddress()) : "") + "]    ");
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
            public void createConnection(Socket s) throws IOException {
                //Add new Client Connection, and init reader + writer.
                BufferedReader brl = new BufferedReader(new InputStreamReader(s.getInputStream()));
                s.setKeepAlive(true);
                MutablePair<BufferedReader,Socket> pair = new MutablePair<>(brl,s);
                //Must also store 'Client' here as a nested object in a tuple
                connectionList.put(s.getInetAddress().getHostAddress(),pair);
                logMessage(s,("Established Client Connection to: "+s.getInetAddress().getHostAddress()));
            }
            @Override
            public void run() {
                try {
                    Socket s;
                    while((s = serverSocket.accept())!=null){
                        //Checks to make sure all clients have a valid connection and the database of keys is not wrong

                        //noinspection BusyWait
                        Thread.sleep(1000);
                        String key = s.getInetAddress().getHostAddress();
                        if(connectionList.containsKey(key)) {
                            if(connectionList.get(key).right.isConnected()){
                                logMessage(s,"Rejected Connection.. Already Exists.. ");
                                s.getOutputStream().write("You have a pre-existing active connection from this machine.. You may only have one at a time".getBytes());
                                s.close();
                            }
                            else {
                                //approve to join here, and overwrite
                                logMessage(s,"Overriding dead connection to Host.. ");
                                createConnection(s);
                            }
                        }
                        else {
                            createConnection(s);
                        }
                    }
                }
                catch (SocketException ignored){ }
                catch (Exception e){
                    e.printStackTrace();
                    System.out.println("""
                            The Client Handshake Listener has experienced an internal error..
                            The server will no longer accept new client connections""");
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
                            case "help" -> {
                                logMessage(connectionList.get(s).right, """
                                        Displaying Help dialogue for:"""+" "+s);
                                String helpDialogue = """
                                       \n
                                       --------------------------------------Help---------------------------------------
                                       
                                       disconnect,quit  : Disconnect current client and leave server active.
                                       shutdown,exit    : Kill the server, and all active connections.
                                       ping             : Send response 'Pong!' to test the latency of the connection
                                       uptime           : Display current server up-time
                                       date             : Display the current Date in Timestamp Format
                                       memory           : Display the current memory usage of the server
                                       help             : Show this dialogue
                                       
                                       ---------------------------------------------------------------------------------
                                        
                                        """;
                                //Don't use sendMessage here because timestamp/host/additional info is not required.
                                connectionList.get(s).right.getOutputStream().write((helpDialogue+"\n").getBytes());

                            }
                            case "date" -> {
                                LocalDateTime temp = LocalDateTime.now();
                                sendMessage(hostAddress,"Date: "+temp.toString(),true,!forceLocalConnection);
                            }
                            case "memory" -> {
                                sendMessage(hostAddress,"Memory Use: "+getMemory(),true,!forceLocalConnection);

                            }
                            case "disconnect","quit" -> {
                                logMessage(connectionList.get(s).right, "Disconnecting Client.. ");
                                connectionList.get(s).right.close();
                                connectionList.get(s).left.close(); //Potential Issue Here..
                                itr.remove();
                                s = null;
                            }
                            case "shutdown", "exit" -> {
                                sendMessage(hostAddress,
                                        "Server Shutting down.. ",true,!forceLocalConnection);
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
                                sendMessage(hostAddress, "Ping: Pong!",true,!forceLocalConnection);

                            case "uptime" -> {
                                LocalDateTime time = LocalDateTime.now();
                                Duration diff = Duration.between(Main.time, time);
                                long days = diff.toDays();
                                long hours = diff.toHours() - 24 * days;
                                long months = diff.toMinutes() - 60 * diff.toHours();
                                long seconds = diff.getSeconds() - 60 * diff.toMinutes();
                                String diffString = ("Uptime: " + days + "d " + hours + "h " + months + "m " + seconds + "s, since " + Main.time);
                                sendMessage(hostAddress, diffString,true,!forceLocalConnection);

                            }
                            default ->
                                sendMessage(hostAddress, ("Unknown command \"" + response + "\" "),true,!forceLocalConnection);
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

    private static double bytesToMiB(long bytes) {
        return ((double) bytes / 1024L * 1024L);
    }
    public static String getMemory() {
        DecimalFormat df = new DecimalFormat("##.###", new DecimalFormatSymbols(Locale.ENGLISH));
        double usedMiB = bytesToMiB(Runtime.getRuntime().maxMemory())-Runtime.getRuntime().freeMemory();
        double maxMiB = bytesToMiB(Runtime.getRuntime().maxMemory());
        return  "("+df.format((usedMiB/maxMiB)*100)+"%) - ["+df.format(usedMiB) + " / " +df.format(maxMiB) + "]";
    }


    //endregion

    //region Getters
    public ServerSocket getSs() {
        return ss;
    }
    //endregion
}
