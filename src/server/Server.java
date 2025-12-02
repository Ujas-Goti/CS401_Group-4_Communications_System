import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

// Basic multi-threaded server that handles multiple clients.
// Each client gets its own thread so several users can connect at the same time.
public class Server {

    public static void main(String[] arguments) {

        int portNumber = 1234; // default port

        // If the user typed a port number when running the program, try to use it.
        if (arguments.length > 0) {
            try {
            	// Get the port number that was passed in
                portNumber = Integer.parseInt(arguments[0]);
            } catch (NumberFormatException exception) {
                System.out.println("Invalid port number. Using default 1234.");
            }
        }
        
        // Define a server socket in the the client
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started on port: " + portNumber);

            // This loop waits forever for new clients to connect.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getRemoteSocketAddress());

                // Give this client its own thread
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}