import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.Socket;


/**
 * A TCP based chat GUI where messages can be sent from client to a server for
 * message distribution.
 */
public class TCPChat extends JFrame {
    /**
     * The chat text area where the messages are displayed.
     */
    private final JTextArea chatTextArea = new JTextArea();

    /**
     * This can be a server or a client instance.
     */
    private CommunicationPartner communicationPartner;

    /**
     * Creates a new TCP Chat instance.
     */
    public TCPChat() {
        super("TCP Chat");
        Container root = getContentPane();
        root.setLayout(new BorderLayout());

        // Add the chat text area with a scrollbar.
        chatTextArea.setEditable(false);
        JScrollPane scrollBar = new JScrollPane(chatTextArea);
        root.add(scrollBar, BorderLayout.CENTER);

        // Add the input field.
        JTextField inputTextField = new JTextField();
        inputTextField.addActionListener(e -> processInput(inputTextField));
        root.add(inputTextField, BorderLayout.PAGE_END);

        // Close connection when the frame gets closed.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeConnection();
            }
        });

        setJMenuBar(makeMenuBar());
        setMinimumSize(new Dimension(400, 300));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
        setVisible(true);
    }

    /**
     * Make a menu bar with menu items to manage connections to other entities.
     *
     * @return The menu bar.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Start");

        menu.add(makeMenuItem("Connect to server",
                this::showClientConnectDialog));
        menu.add(makeMenuItem("Host server", this::showServerHostDialog));
        menu.add(makeMenuItem("Close connection", this::closeConnection));

        menuBar.add(menu);
        return menuBar;
    }

    /**
     * Make a menu item.
     *
     * @param name The displayed name.
     * @param clicked The action which gets called when the item is clicked.
     * @return The menu item.
     */
    private JMenuItem makeMenuItem(String name, Runnable clicked) {
        JMenuItem connect = new JMenuItem(name);
        connect.addActionListener(e -> clicked.run());
        return connect;
    }

    /**
     * Process the entered text in the input text field.
     *
     * @param inputTextField The input text field.
     */
    private void processInput(JTextField inputTextField) {
        String inputText = inputTextField.getText();
        inputTextField.setText("");

        if (communicationPartner != null &&
                communicationPartner.send(inputText)) {
            appendToChat(communicationPartner.getName() + ": " + inputText);
        } else if (!inputText.isEmpty()) {
            appendToChat("Error: No connection to send the message!");
        }
    }

    /**
     * Close all connections and reset the instance.
     */
    private void closeConnection() {
        if (communicationPartner == null) {
            appendToChat("Error: No connection to close!");
        } else {
            communicationPartner.closeConnection();
            communicationPartner = null;
        }
    }

    /**
     * Show a dialog to enter name, password and port for server hosting.
     */
    private void showServerHostDialog() {
        if (communicationPartner instanceof Client) {
            appendToChat("Error: Currently connected to another server!");
        } else if (communicationPartner instanceof Server) {
            appendToChat("Error: Already hosting server!");
        } else {
            showDialog(false);
        }
    }

    /**
     * Show a dialog to enter name, password, host and port to connect to a
     * server.
     */
    private void showClientConnectDialog() {
        if (communicationPartner instanceof Client) {
            appendToChat("Error: Already connected to another server!");
        } else if (communicationPartner instanceof Server) {
            appendToChat("Error: Currently hosting server!");
        } else {
            showDialog(true);
        }
    }

    /**
     * Show a dialog to enter name, password, host and port to connect to a
     * server or host a server.
     *
     * @param clientMode If the user wants to connect to a server.
     */
    private void showDialog(boolean clientMode) {
        JDialog dialog = new JDialog(this, "TCP Chat", true);

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 2;
        c.weighty = 5;

        Container root = dialog.getContentPane();
        root.setLayout(gridBagLayout);

        int gridY = 0;
        JTextField nameField = addTextInputField("Name", "Max", c,
                gridBagLayout, root, gridY++);
        JTextField passwordField = addTextInputField("Password", "", c,
                gridBagLayout, root, gridY++);
        // TODO If !clientMode hostField is not needed.
        JTextField hostField = addTextInputField("Host", "127.0.0.1", c,
                gridBagLayout, root, gridY++);
        JTextField portField = addTextInputField("Port", "4444", c,
                gridBagLayout, root, gridY++);

        addConnectButton(() -> connectButtonClicked(clientMode, dialog,
                nameField, passwordField, hostField, portField),
                c, gridBagLayout, root, gridY);

        dialog.setMinimumSize(new Dimension(200, 170));
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }

    /**
     * Check if the input is valid and host a server or connect to one
     * depending on the clientMode flag.
     *
     * @param clientMode If client mode was selected.
     * @param dialog The dialog.
     * @param nameField The name field.
     * @param passwordField The password field.
     * @param hostField The host field.
     * @param portField The port field.
     */
    private void connectButtonClicked(boolean clientMode, JDialog dialog,
                                      JTextField nameField,
                                      JTextField passwordField,
                                      JTextField hostField,
                                      JTextField portField) {
        String name = nameField.getText();
        String password = passwordField.getText();
        String host = hostField.getText();
        int port = getPort(portField.getText());

        if (name.contains(CommunicationPartner.LIMITER)) {
            appendToChat("Error: Name must not contain \"" +
                    CommunicationPartner.LIMITER + "\"!");
        } else {
            if (clientMode) {
                connectToServer(name, password, host, port);
            } else {
                hostServer(name, password, port);
            }

            if (communicationPartner != null) {
                dialog.dispose();
            }
        }
    }

    /**
     * Add connect button to the parent dialog.
     *
     * @param clicked The clicked action.
     * @param c The grid bag constraints.
     * @param gridBagLayout The grid bag layout.
     * @param parent The parent dialog.
     * @param gridY The y offset in the grid bag layout.
     */
    private static void addConnectButton(Runnable clicked, GridBagConstraints c,
                                         GridBagLayout gridBagLayout,
                                         Container parent, int gridY) {
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> clicked.run());
        c.gridx = 0;
        c.gridy = gridY;
        c.gridwidth = 2;
        gridBagLayout.setConstraints(connectButton, c);
        parent.add(connectButton);
    }

    /**
     * Add a label and a text field in a row of a grid bag layout.
     *
     * @param name The text of the label.
     * @param defaultValue The default text of the text field.
     * @param c The grid bag constraints.
     * @param gridBagLayout The grid bag layout.
     * @param parent The parent dialog.
     * @param gridY The y offset in the grid bag layout.
     * @return The text field.
     */
    private static JTextField addTextInputField(String name,
                                                String defaultValue,
                                                GridBagConstraints c,
                                                GridBagLayout gridBagLayout,
                                                Container parent, int gridY) {
        JLabel hostLabel = new JLabel(name + ':', SwingConstants.CENTER);
        c.gridx = 0;
        c.gridy = gridY;
        gridBagLayout.setConstraints(hostLabel, c);
        parent.add(hostLabel);

        JTextField hostTextField = new JTextField(defaultValue);
        c.gridx = 1;
        gridBagLayout.setConstraints(hostTextField, c);
        parent.add(hostTextField);
        return hostTextField;
    }

    /**
     * Connect to a server.
     *
     * @param name The name of the client.
     * @param password The password of the server.
     * @param host The ip address of the server.
     * @param port The port of the server.
     */
    private void connectToServer(String name, String password, String host,
                                 int port) {
        try {
            Socket socket = new Socket(host, port);
            communicationPartner = new Client(name, socket,
                    this::invokeAppendToChat);

            ((Client) communicationPartner).authenticate(password,
                    () -> communicationPartner = null);
        } catch (ConnectException e) {
            appendToChat("Error: Connection refused!");
        } catch (IOException e) {
            appendToChat("Error: Connection setup failed!");
            e.printStackTrace();
        }
    }

    /**
     * Host a new server for clients to connect.
     *
     * @param name The name of the server.
     * @param password The password of the server.
     * @param port The port to host the server
     */
    private void hostServer(String name, String password, int port) {
        try {
            communicationPartner = new Server(name, port,
                    this::invokeAppendToChat);
            ((Server) communicationPartner).startServerSocketListener(password);
            appendToChat("Server was started at port " + port + ".");
        } catch (BindException e) {
            appendToChat("Error: Port already in use!");
        } catch (IOException e) {
            appendToChat("Error: Connection setup failed!");
            e.printStackTrace();
        }
    }

    /**
     * Append a message to the chat text area.
     * This method can be called from any thread.
     *
     * @param text The message.
     */
    private void invokeAppendToChat(String text) {
        SwingUtilities.invokeLater(() -> appendToChat(text));
    }

    /**
     * Append a message to the chat text area.
     * This method should only be called from the EDT.
     *
     * @param text The message.
     */
    private void appendToChat(String text) {
        chatTextArea.setText(chatTextArea.getText() + text +
                System.lineSeparator());
    }

    /**
     * Get the integer port of the port string if the string is valid.
     *
     * @param portString The port string.
     * @return The port integer.
     */
    private static int getPort(String portString) {
        try {
            int port = Integer.parseInt(portString);

            if (port < 0 || port > 0xFFFF) {
                throw new IllegalArgumentException("Error: Port value out of " +
                        "range!");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error: Port was malformed!");
        }
    }

    /**
     * Create a new TCP Chat instance.
     *
     * @param args Arguments are ignored.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TCPChat::new);
    }
}
