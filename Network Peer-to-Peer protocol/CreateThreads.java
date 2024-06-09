import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class CreateThreads implements Runnable {

    static final String INFO_CFG = "PeerConfigurationInfo.cfg";

    static int peer_identifier;
    static TreeMap<Integer, Peer> glbIntegerPeerTreeMap;
    static LoggingUtility records;
    static HashMap<Integer, byte[]> filesHashMap;


    public CreateThreads(int peer_identifier) throws Exception {
        //Peer directories are created
        CreateThreads.peer_identifier = peer_identifier;
        glbIntegerPeerTreeMap = getPeerInformation();

        // Creating Peer directories
        try {
            String fileName = CommonConfigurations.nameOfTheFile;
            FileProcessingUtility.generateNewDirectories(peer_identifier, CommonConfigurations.nameOfTheFile);
            records = new LoggingUtility(String.valueOf(CreateThreads.peer_identifier),"log");


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    @Override
    public void run() {
        // TODO Auto-generated method stub

        new Thread(new StartupClient()).start();
        new Thread(new StartupServer()).start();
        new Thread(new DeterminePreferredNeighbors()).start();
        new Thread(new OmptimallyUnChokeInterestedNeighbors()).start();


    }

    private class StartupServer implements Runnable {

        byte[] handshakePacket = new byte[32];

        @Override
        public void run() {
            try {
                // Wait for new connections at designated port
                int port = glbIntegerPeerTreeMap.get(peer_identifier).port;
                ServerSocket serverSocket = new ServerSocket(port);
                records.logInfo("Server: " + CreateThreads.peer_identifier + " Started on Port Number :" + port);
                boolean newPeers = false;
                for (Map.Entry<Integer, Peer> neighbor : glbIntegerPeerTreeMap.entrySet()) {
                    if (newPeers) {
                        Socket socket = serverSocket.accept();
                        ObjectInputStream serverInputStream = new ObjectInputStream(socket.getInputStream());
                        ObjectOutputStream serverOutputStream = new ObjectOutputStream(socket.getOutputStream());

                        serverInputStream.read(handshakePacket);


                        serverOutputStream.write(MessageManager.generateHandShake(peer_identifier));
                        serverOutputStream.flush();

                        records.logInfo("Peer :" + peer_identifier + " makes a connection to" + neighbor.getKey());
                        neighbor.getValue().startMessageExchange(socket);
                    }
                    if (peer_identifier == neighbor.getKey())
                        newPeers = true;


                }

                serverSocket.close();

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private class StartupClient implements Runnable {

        @Override
        public void run() {
            try {
                for (Map.Entry<Integer, Peer> peer : glbIntegerPeerTreeMap.entrySet()) {

                    if (peer.getKey() == peer_identifier)
                        break;
                    Peer neighbor = peer.getValue();
                    Socket socket = new Socket(neighbor.hostName, neighbor.port);
                    // Input and output Streams
                    ObjectOutputStream clientOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream clientInputStream = new ObjectInputStream(socket.getInputStream());

                    //creating the handshake header and writing it into output stream
                    byte[] handshakePacket = MessageManager.generateHandShake(peer_identifier);
                    clientOutputStream.write(handshakePacket);
                    clientOutputStream.flush();


                    //Reading handshake packet from the server and authenticating it
                    clientInputStream.readFully(handshakePacket);
                    String messageHeader = UtilityClass.returnStringFromBytes(handshakePacket, 0, 17);
                    String messagePeerID = UtilityClass.returnStringFromBytes(handshakePacket, 28, 31);


                    if (messageHeader.equals("P2PFILESHARINGPROJ") && Integer.parseInt(messagePeerID) == peer.getKey()) {
                        //log.logInfo("Client received back handshake from the " + peer.getKey());
                        neighbor.startMessageExchange(socket);
                    } else {
                        socket.close();
                    }

                }

            } catch (IOException exception) {

                exception.printStackTrace();

            }
        }
    }

    public static ArrayList<String> readPeerConfig() throws IOException {
        ArrayList<String> getPeerInfoConfig = UtilityClass.returnFileLines(INFO_CFG);
        return getPeerInfoConfig;

    }
    // Method for Creation of tree map
    public static TreeMap<Integer, Peer> generateTreeMap(ArrayList<String> getPeerInfoConfig ) throws Exception {
        int val=0;
        TreeMap<Integer, Peer> integerPeerTreeMap = new TreeMap<>();
        while (getPeerInfoConfig.size() > val)
        {
            String currentString = getPeerInfoConfig.get(val);
            String[] splitWords = currentString.split(" ");
            integerPeerTreeMap.put( Integer.valueOf(splitWords[0]), new Peer(Integer.parseInt(splitWords[0]), splitWords[1],
                    Integer.valueOf(splitWords[2]), Integer.parseInt(splitWords[3])));
            val++;
        }
        return integerPeerTreeMap;
    }

    public static TreeMap<Integer, Peer> getPeerInformation() throws Exception {
        ArrayList<String> getPeerInfoConfig = readPeerConfig();
        return generateTreeMap(getPeerInfoConfig);
    }

}
