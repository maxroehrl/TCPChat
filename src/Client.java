import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


/**
 * A client to communicate with.
 */
public class Client extends CommunicationPartner {
    /**
     * The socket connection.
     */
    private Socket socket;

    /**
     * The output stream of the socket.
     */
    private PrintStream outputStream;

    /**
     * The input stream of this socket.
     */
    private BufferedReader inputStream;

    /**
     * Create a new Client.
     *
     * @param name The name of the client.
     * @param socket The socket.
     * @param chatConsumer The function which prints to the chat text area.
     * @throws IOException If an I/O error occurs when creating the
     *             input or output stream, the socket is closed or the socket is
     *             not connected.
     */
    public Client(String name, Socket socket, Consumer<String> chatConsumer)
            throws IOException {
        super(name, chatConsumer);
        this.socket = socket;
        outputStream = new PrintStream(socket.getOutputStream());
        inputStream = new BufferedReader(new InputStreamReader(
                new DataInputStream(socket.getInputStream())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean send(String text) {
        if (text == null || text.isEmpty()) {
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
        outputStream.println(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() &&
                socket.equals(((Client) o).socket);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return socket.hashCode();
    }

    /**
     * Get the strings in the next line of the input stream.
     *
     * @param limit The threshold of strings.
     * @return The strings in the next line.
     * @throws IOException If an I/O error occurs while reading the line.
     */
    private String[] getInputLine(int limit) throws IOException {
        String received = inputStream.readLine();

        if (received == null) {
            throw new IOException("Read line returned null!");
        }
        return received.split(LIMITER, limit);
    }

    /**
     * Authenticate to the server with the password and setup the connection.
     *
     * @param password The password of the server to connect to.
     * @param closeConnectionCallback The action which runs if the connection
     *                                gets closed.
     */
    public void authenticate(String password,
                             Runnable closeConnectionCallback) {
        Thread clientListenerThread = new Thread(() -> {
            // Send this client's name and password to the server.
            sendWithoutName(name + LIMITER + password);

            // Server sends back its name and the names of other clients if the
            // password is right.
            String[] serverAndClientNames = {};

            try {
                serverAndClientNames = getInputLine(0);
            } catch (IOException ignored) {
            }

            if (serverAndClientNames.length == 0) {
                printToChat("Error: Password was wrong!");
            } else {
                String serverName = serverAndClientNames[0];
                printToChat("Connection to \"" + serverName +
                        "\" was established.");
                printToChat("You are now connected with: " +
                        Arrays.toString(serverAndClientNames));

                listen(null);

                printToChat("Connection to \"" + serverName + "\" was closed.");
            }
            closeConnection();
            closeConnectionCallback.run();
        });
        clientListenerThread.setName("ClientListener");
        clientListenerThread.start();
    }

    /**
     * Check if the entered password is right and exchange names between the
     * communication partners.
     *
     * @param serverPassword The server password.
     * @return If the entered password of the client was right.
     */
    protected boolean checkAuthentication(String serverPassword) {
        String clientPassword;

        try {
            // Client sends name and password first.
            String[] nameAndPassword = getInputLine(2);
            name = nameAndPassword[0];
            clientPassword = nameAndPassword[1];
        } catch (IOException e) {
            return false;
        }

        return serverPassword.isEmpty() ||
                serverPassword.equals(clientPassword);
    }

    /**
     * Listen for incoming messages until the connection gets closed by the
     * message partner.
     *
     * @param otherClients An optional list of other clients to forward received
     *                     messages to.
     */
    protected void listen(List<Client> otherClients) {
        try {
            String received;

            while ((received = inputStream.readLine()) != null) {
                if (received.startsWith(LIMITER, 1)) {
                    // Show if a message partner has left.
                    printToChat("\"" +
                            received.substring(1 + LIMITER.length()) +
                            "\" has left the conversation.");
                } else if (received.startsWith(LIMITER, 0)) {
                    // Show if a new message partner is connected.
                    printToChat("\"" + received.substring(LIMITER.length()) +
                            "\" has joined the conversation.");
                } else {
                    // Print all received messages to the chat text area.
                    printToChat(received.replace(LIMITER, ": "));

                    // Forward received messages to all other clients.
                    if (otherClients != null) {
                        for (Client otherClient : otherClients) {
                            if (this != otherClient) {
                                otherClient.sendWithoutName(received);
                            }
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}