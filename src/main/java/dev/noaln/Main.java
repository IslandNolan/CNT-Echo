package dev.noaln;

import java.net.Socket;
import java.sql.Timestamp;
import java.util.Date;

public class Main {

    private static final Timestamp time = new Timestamp(new Date().getTime());
    private static Client c = null;
    private static Server s = null;

    public static void main(String[] args) {

        if (args.length > 1) {
            if (args[0].toLowerCase().startsWith("-s")) {
                //Manual and lock to while loop to listen for input.
                System.out.println("Initializing Standalone Server on Port <>.. ");


            } else if (args[0].toLowerCase().startsWith("-c")) {
                //Client init.
                System.out.println("Initializing Standalone Client on Port <>.. ");


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