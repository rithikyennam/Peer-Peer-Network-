import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Random;

class OmptimallyUnChokeInterestedNeighbors implements Runnable {

    public OmptimallyUnChokeInterestedNeighbors() {
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                while (Peer.numberOfCompleteDownloads < CreateThreads.glbIntegerPeerTreeMap.size()) {

                    HashSet<Integer> preferredCandidates = new HashSet<Integer>(Peer.interestedNeighbors);
                    HashSet<Integer> clonePreferred = new HashSet<Integer>(Peer.preferredNeighbors);
                    preferredCandidates.removeAll(clonePreferred);

                    Random rand = new Random();
                    if (preferredCandidates.size() > 0) {
                        int chosenInterestedNeighbor = rand.nextInt(preferredCandidates.size());
                        Peer.optimallyUnchokedPeer = (int) preferredCandidates.toArray()[chosenInterestedNeighbor];


                        Peer.chokingMap.put(Peer.optimallyUnchokedPeer, false);
                        Socket socket = CreateThreads.glbIntegerPeerTreeMap.get(Peer.optimallyUnchokedPeer).socket;
                        if (socket == null) {
                            break;
                        }
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        dataOutputStream.write(MessageManager.createAndMakeMessage(MessageConstants.UNCHOKE_FLAG, null, -1, 0));
                        dataOutputStream.flush();
                    }
                    CreateThreads.records.logInfo("Peer " + CreateThreads.peer_identifier + " has the optimistically unchoked neighbor " + Peer.optimallyUnchokedPeer);

                    Thread.sleep(CommonConfigurations.optimistic_Unchoking_Interval * 1000);

                }


            } catch (SocketException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
