import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private boolean isLoggedIn = false;

    // Constructor saves the client's socket
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        try (Socket socket = clientSocket) {

            // Create output stream first so it sends stream header
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Keep listening for objects that are getting passed in
            while (true) {

                Object receivedObject = inputStream.readObject();

                // This is where I plan to write the route system for our APIS
                
                }
              
         }

        } catch (EOFException exception) {
            System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
}
