import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

class DeterminePreferredNeighbors implements Runnable {

    public DeterminePreferredNeighbors() {
    }
    public DeterminePreferredNeighbors(String peerInfo) {
    }
    @Override
    public void run() {
        synchronized (this) {

            try {
                while (Peer.numberOfCompleteDownloads < CreateThreads.glbIntegerPeerTreeMap.size()) {

                    int KNeighbors = CommonConfigurations.countOfPrefNeighbours;
                    Peer.preferredNeighbors.clear();


                    if (Peer.interestedNeighbors.size() > KNeighbors) {
                        int i = 0;
                        for (HashMap.Entry<Integer, Double> e : Peer.lastIntervalDownloadSpeeds.entrySet()) {
                            Peer.preferredNeighbors.add(e.getKey());
                            i++;
                            if (i >= KNeighbors) {
                                break;
                            }
                        }
                    } else {
                        for (Integer peerID : Peer.interestedNeighbors) {
                            Peer.preferredNeighbors.add(peerID);
                        }
                    }

                    Peer.lastIntervalDownloadSpeeds.replaceAll((key, value) -> 0.0);

                    for (HashMap.Entry<Integer, Boolean> pair : Peer.chokingMap.entrySet()) {
                        Socket socket = CreateThreads.glbIntegerPeerTreeMap.get(pair.getKey()).socket;
                        if (socket == null) {
                            continue;
                        }
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        if (Peer.preferredNeighbors.contains(pair.getKey())) {
                            dataOutputStream.write(MessageManager.createAndMakeMessage(MessageConstants.UNCHOKE_FLAG, null, -1, 0));
                            dataOutputStream.flush();
                            Peer.chokingMap.put(pair.getKey(), false);

                        } else {


                            dataOutputStream.write(MessageManager.createAndMakeMessage(MessageConstants.CHOKE_FLAG, null, -1, 0));
                            dataOutputStream.flush();
                            Peer.chokingMap.put(pair.getKey(), true);

                        }
                    }
                    CreateThreads.records.logInfo("Peer " + CreateThreads.peer_identifier +" has the preferred neighbors " + Peer.preferredNeighbors.toString());

                    Thread.sleep(CommonConfigurations.unchoking_Interval * 1000);

                }

            }
            catch (SocketException e)
            {}
            catch (Exception e) {

                e.printStackTrace();
            }
            System.exit(0);
        }
    }
}
