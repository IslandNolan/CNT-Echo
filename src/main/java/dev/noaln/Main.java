package dev.noaln;

import java.io.IOException;
import java.time.LocalDateTime;

public class Main {

    protected static final LocalDateTime time = LocalDateTime.now();


    public static void main(String[] args) {

        /*
            Arg Examples:
                -c8000 -s8000
                client port 8000, server port 8000
                if both args are absent, integrated server will be started with automatic port selection
                if one or the other is present, jar will start in Standalone Client/Server Mode

         */
        if (args.length >= 1) {
            if (args[0].toLowerCase().startsWith("-s")) {
                //Manual and lock to while loop to listen for input.
                int sPort;
                try {
                    sPort = Integer.parseInt(args[0].substring(2));
                } catch(NumberFormatException e) { sPort = 0; }

                try {
                    //0 will autoconfigure a port to use if left blank.
                    if(sPort!=0 && (sPort<2000 && sPort>8000)) { System.out.println("The specified port ("+sPort+") is out of range <2000-8000>"); System.exit(1); }
                    Server s = new Server(sPort);
                    s.listen(); //Do the listening here

                }
                catch (IOException e){
                    System.out.println("Failed to start standalone server.. \nEnsure that the port you want to use ("+sPort+") is not currently reserved. Exiting.. ");
                    System.exit(1);
                }
                catch (Exception e){
                    e.printStackTrace();
                    System.out.println("An unknown error has occurred..\nEnsure that the port you want to use is not currently reserved. Exiting.. ");
                }
            } else if (args[0].toLowerCase().startsWith("-c")) {
                //Client init.
                int cPort = Integer.parseInt(args[0].substring(2));
                System.out.println("Initializing Standalone Client on Port <"+cPort+">..");
                new Client(); //Do stuff

            } else {
                System.out.println("Unrecognized argument: " + args[0]);
                System.exit(1);
            }
        } else {
            //Integrated Automatic Server + Client...
            System.out.println("Implement Integrated Client+Server on Automatic Port Select later");
        }
    }
}