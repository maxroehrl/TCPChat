import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;


/**
 * A server which accepts and manages the sockets of different clients for
 * communication.
 */
public class Server extends CommunicationPartner {
    /**
     * The server socket which accepts incoming connections.
     */
    private ServerSocket serverSocket;

    /**
     * The list of connected clients.
     */
    private final List<Client> clients = new ArrayList<>();

    /**
     * Create a new server.
     *
     * @param name The name.
     * @param port The port.
     * @param chatConsumer The function which prints to the chat text area.
     * @throws IOException If the creation of the server socket fails.
     */
    public Server(String name, int port, Consumer<String> chatConsumer)
            throws IOException {
        super(name, chatConsumer);
        serverSocket = new ServerSocket(port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeConnection() {
        clients.forEach(Client::closeConnection);

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean send(String text) {
        if (text == null || text.isEmpty() || clients.isEmpty()) {
            return false;
        } else {
            sendWithoutName(name + LIMITER + text);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void sendWithoutName(String text) {
        clients.forEach(client -> client.sendWithoutName(text));
    }

    /**
     * Start waiting for incoming connections.
     *
     * @param serverPassword The server password.
     */
    public void startServerSocketListener(String serverPassword) {
        Thread serverSocketThread = new Thread(() -> {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    Client client = new Client("Unknown", socket, chatConsumer);

                    Thread t = new Thread(() ->
                            checkAuthentication(client, serverPassword));
                    t.setName("ServerClientListener");
                    t.start();
                }
            } catch (SocketException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverSocketThread.setName("ServerSocketListener");
        serverSocketThread.start();
    }

    /**
     * Initiate the connection of the client and start listening for incoming
     * messages if the password is right.
     *
     * @param client The client.
     * @param serverPassword The server password.
     */
    private void checkAuthentication(Client client, String serverPassword) {
        if (client.checkAuthentication(serverPassword)) {
            // Send the new client's name to all other clients.
            sendWithoutName(LIMITER + client.getName());

            // Send server and client names to new client.
            client.sendWithoutName(getConnectedClients());

            clients.add(client);
            printToChat(client + " has joined the conversation.");

            client.listen(Collections.unmodifiableList(clients));

            clients.remove(client);
            printToChat("Connection to " + client + " was closed.");
        } else {
            printToChat(client + " entered a wrong password.");
        }
        client.closeConnection();

        // Notify all remaining clients that this client has left.
        sendWithoutName(LIMITER + LIMITER + client.getName());
    }

    /**
     * Get the string which contains the names of all connected clients.
     *
     * @return The string with the names of connected clients.
     */
    private String getConnectedClients() {
        StringBuilder sb = new StringBuilder(name);

        for (Client client : clients) {
            sb.append(LIMITER).append(client.getName());
        }
        return sb.toString();
    }
}