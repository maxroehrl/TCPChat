import java.util.function.Consumer;

/**
 * A communication partner which can be a client or a server.
 */
public abstract class CommunicationPartner {
    /**
     * The string which separates the different parts of a sent text.
     */
    public static final String LIMITER = ":";

    /**
     * The name.
     */
    protected String name;

    /**
     * The function which prints to the chat text area.
     */
    protected Consumer<String> chatConsumer;

    /**
     * Create a communication partner.
     *
     * @param name The name.
     * @param chatConsumer The function which prints to the chat text area.
     */
    public CommunicationPartner(String name, Consumer<String> chatConsumer) {
        this.name = name;
        this.chatConsumer = chatConsumer;
    }

    /**
     * Close all connections.
     */
    public abstract void closeConnection();

    /**
     * Send a text to all connected communication partners.
     *
     * @param text The text.
     * @return If the text was sent.
     */
    public abstract boolean send(String text);

    /**
     * Get the name.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the client.
     *
     * @return The string representation of the client.
     */
    @Override
    public String toString() {
        return "\"" + name + "\"";
    }

    /**
     * Print a text to the chat area.
     *
     * @param text The text.
     */
    protected void printToChat(String text) {
        chatConsumer.accept(text);
    }
}
