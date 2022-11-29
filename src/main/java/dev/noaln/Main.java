package dev.noaln;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

public class Main {

    protected static final Timestamp time = new Timestamp(new Date().getTime());
    private static Client c = null;
    private static Server s = null;


    public static void main(String[] args) {

        if (args.length >= 1) {
            if (args[0].toLowerCase().startsWith("-s")) {
                //Manual and lock to while loop to listen for input.
                Integer sPort;
                try {
                    sPort = Integer.parseInt(args[0].substring(2));
                } catch(NumberFormatException e) { sPort = 0; }

                try {
                    //0 will auto configure a port to use if left blank.
                    if(sPort!=0 && (sPort<2000 && sPort>8000)) { System.out.println("The specified port ("+sPort+") is out of range <2000-8000>"); System.exit(1); }
                    Server s = new Server(sPort);
                    s.listen(); //Do the listening here..

                }
                catch (IOException e){
                    System.out.println("Failed to start standalone server.. \nEnsure that the port you want to use ("+sPort+") is not currently reserved. Exiting.. ");
                    System.exit(1);
                }
                catch (Exception e){
                    System.out.println("An unknown error has occurred..\nEnsure that the port you want to use is not currently reserved.\nRestarting Standalone Server in 5 Seconds.. ");
                    main(args);
                }
            } else if (args[0].toLowerCase().startsWith("-c")) {
                //Client init.
                Integer cPort = Integer.parseInt(args[0].substring(2));
                System.out.println("Initializing Standalone Client on Port <"+cPort+">..");
                new Client();

            } else {
                System.out.println("Unrecognized argument: " + args[0]);
                System.exit(1);
            }

        } else if (args.length == 1) {
            //Bootstrap all in one client + server, then open input for client.
            System.out.println("Initializing Integrated Server+Client on port <> ");


        } else {
            System.out.println("Missing Arguments.. ");
            System.exit(1);

        }
    }
}