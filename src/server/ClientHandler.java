package server;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import common.User;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Authentication auth;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private boolean isLoggedIn = false;

    //Constructor saves the client's socket
    public ClientHandler(Socket clientSocket, Authentication auth) {
        this.clientSocket = clientSocket;
        this.auth = auth;
    }

    @Override
    public void run() {

        try (Socket socket = clientSocket) {

            //Create output stream first so it sends stream header
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            //Keep listening for objects that are getting passed in
            while (true) {

                //For now just assume that we are reciving the credentials
                //as two strings.

                //1. get username

            	//Check if the object that was passed is a String
                Object usernameObj = inputStream.readObject();
                if (!(usernameObj instanceof String)) {
                    System.out.println("Expected username String, got: " + usernameObj);
                    break;
                }
                String username = (String) usernameObj;

                //2. get password

                //Check if the object that was passed is a String
                Object passwordObj = inputStream.readObject();
                if(!(passwordObj instanceof String)) {
                	System.out.println("Expected password String, got: " + passwordObj);
                    break;
                }

                String password = (String) passwordObj;

                System.out.println("Ok, just received login attempt from username: " + username);

                //3. pass username and password to authentication
                User loggedInUser = auth.validateCredentials(username, password);

                //This will either send the client a User object if user logged in successfully otherwise null.
                outputStream.writeObject(loggedInUser);
                outputStream.flush();
                outputStream.reset();

                break;

                //4. attempt to send back a user object to the client




                //This is where I plan to write the route system for our APIS


                //Cases for when the object is a user
                //1. Authenitcating

                //Cases for when the object is a Message
                //1. Ask chat manager to either perserve
                //2. Route
                }

        } catch (EOFException e) {
            System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
