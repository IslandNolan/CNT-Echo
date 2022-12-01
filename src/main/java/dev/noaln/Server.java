package dev.noaln;

import org.apache.commons.lang3.tuple.MutablePair;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final ServerSocket ss;
    private final ConcurrentHashMap<String, MutablePair<BufferedReader,Socket>> connectionMap = new ConcurrentHashMap<>();
    public Server(Integer port) throws IOException {
        System.out.println("Configuring Server.. ");
        this.ss = new ServerSocket(port);
        if(port==0) { System.out.println("No port was specified in the command arguments, selecting next available port.. "); }
        System.out.println("Listening on port: "+ss.getLocalPort());
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
    public void sendMessage(String key,String message,Boolean omitPrefix){
        try {
            connectionMap.get(key).right.getOutputStream().write(("     "+message+((!omitPrefix) ? "\n>> " : "\n")).getBytes());
            StringBuilder consoleMessage = new StringBuilder("[" + LocalDateTime.now() + " | " + connectionMap.get(key).right.getInetAddress().getHostAddress() +"]");
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
                connectionMap.put(s.getInetAddress().getHostAddress(),pair);
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
                        if(connectionMap.containsKey(key)) {
                            if(connectionMap.get(key).right.isConnected()){
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
            Iterator<String> itr = connectionMap.keySet().iterator();
            String s = (itr.hasNext() ? itr.next() : null);
            while(s != null) {
                try {
                    String response, hostAddress = connectionMap.get(s).right.getInetAddress().getHostAddress();
                    while (connectionMap.containsKey(s) && (response = connectionMap.get(s).left.readLine()) != null) {
                        response = response.stripLeading();
                        response = response.stripTrailing();
                        switch (response) {
                            case "help" -> {
                                logMessage(connectionMap.get(s).right, """
                                        Displaying Help dialogue for:"""+" "+s);
                                String helpDialogue = """
                                       \n
                                       --------------------------------------Help---------------------------------------
                                       
                                       disconnect       : Disconnect current client and leave server active.
                                       quit,exit        : Kill the server, and all active connections.
                                       ping             : Send response 'Pong!' to test the latency of the connection
                                       uptime           : Display current server up-time
                                       date             : Display the current Date in Timestamp Format
                                       memory           : Display the current memory usage of the server
                                       help             : Show this dialogue
                                       
                                       ---------------------------------------------------------------------------------
                                        
                                        """;
                                //Don't use sendMessage here because timestamp/host/additional info is not required.
                                connectionMap.get(s).right.getOutputStream().write((helpDialogue+"\n").getBytes());

                            }
                            case "date" -> {
                                sendMessage(hostAddress, new BufferedReader(new InputStreamReader(Runtime.getRuntime()
                                        .exec("date").getInputStream())).readLine().trim() ,true);
                            }
                            case "memory" -> {
                                BufferedReader br = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("free").getInputStream()));
                                String res;
                                while((res = br.readLine())!=null){
                                    sendMessage(hostAddress,res,true);
                                }
                            }
                            case "disconnect" -> {
                                logMessage(connectionMap.get(s).right, "Disconnecting Client.. ");
                                connectionMap.get(s).right.close();
                                connectionMap.get(s).left.close(); //Potential Issue Here..
                                itr.remove();
                                s = null;
                            }
                            case "quit", "exit" -> {
                                sendMessage(hostAddress,
                                        "Server Shutting down.. ",true);
                                String finalS = s;
                                connectionMap.values().forEach(x -> {
                                    try {
                                        logMessage(connectionMap.get(finalS).right,"Closing connection to client.. ");
                                        x.right.close();
                                    } catch (Exception ignored) {
                                    }
                                });
                                try {
                                    handshake.interrupt();
                                    System.out.println("Closing Server Socket.. ");
                                    this.ss.close();
                                } catch (Exception ignored) { } //I know this is bad, doesn't matter though, server shutting down
                                System.exit(0);
                            }
                            case "ping" ->
                                sendMessage(hostAddress, "Ping: Pong!",true);

                            case "uptime" -> {
                                sendMessage(hostAddress, new BufferedReader(new InputStreamReader(Runtime.getRuntime()
                                        .exec("uptime").getInputStream())).readLine().trim() ,true);
                            }
                            default ->
                                sendMessage(hostAddress, ("Unknown command \"" + response + "\" "),true);
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                if(!itr.hasNext()) { itr = connectionMap.keySet().iterator(); }
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
