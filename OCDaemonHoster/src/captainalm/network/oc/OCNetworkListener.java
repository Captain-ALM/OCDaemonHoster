package captainalm.network.oc;

/**
 * 
 */
import java.io.*;
import java.net.*;

/**
 * The Open Computers Network Listener.
 * 
 * @author Captain ALM
 */
public class OCNetworkListener {
	private ServerSocket sSock;
	private Thread lThread;
	private boolean listening;
	private OCNetworkClient acceptedClient;
	private boolean cWaiting;
	private boolean cExists;
	private Object slockcl = new Object();
	private InetSocketAddress listeningAddress;

	public OCNetworkListener(InetSocketAddress addressIn) {
		lThread = new Thread(new OCNetworkListenerThread(this), "OCNetworkListenerThread");
		lThread.setDaemon(true);
		listening = false;
		listeningAddress = addressIn;
		try {
			sSock = new ServerSocket();
			sSock.setReceiveBufferSize(Short.MAX_VALUE);
		} catch (IOException e) {
			e.printStackTrace();
			sSock = null;
		}
		try {
			if (sSock == null) {
				throw new IOException("Underlying Socket Failed Construction.");
			}
			sSock.bind(addressIn, 1);
			listening = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (listening) {
			lThread.start();
		}
	}

	public OCNetworkClient getAcceptedClient() {
		OCNetworkClient toret = null;
		synchronized (slockcl) {
			if (cWaiting && acceptedClient != null) {
				cExists = true;
				cWaiting = false;
				toret = acceptedClient;
			}
		}
		return toret;
	}

	public void returnAcceptedClient(OCNetworkClient toRet) {
		synchronized (slockcl) {
			if (cExists && toRet == acceptedClient) {
				toRet.shutdown();
				toRet.close();
				toRet = null;
				cExists = false;
			}
		}
	}

	public InetSocketAddress getListeningAddress() {
		return listeningAddress;
	}

	public boolean getIsThereGottenClient() {
		return cExists;
	}

	public boolean getIsThereAcceptedClient() {
		return cWaiting;
	}

	public boolean getIsListening() {
		return listening;
	}

	public void close() {
		if (acceptedClient != null && (cWaiting == true)) {
			this.getAcceptedClient();
		}
		if (acceptedClient != null && (cExists == true)) {
			this.returnAcceptedClient(acceptedClient);
		}
		if (!sSock.isClosed()) {
			try {
				sSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		listening = false;
		while (lThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
		sSock = null;
	}

	private class OCNetworkListenerThread implements Runnable {
		private OCNetworkListener parent;

		public OCNetworkListenerThread(OCNetworkListener parentIn) {
			parent = parentIn;
		}

		@Override
		public void run() {
			while (parent.listening) {
				while (parent.cExists) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				try {
					Socket sa = parent.sSock.accept();
					sa.setReceiveBufferSize(Short.MAX_VALUE);
					sa.setSendBufferSize(Short.MAX_VALUE);
					sa.setSoTimeout(5000);
					parent.acceptedClient = new OCNetworkClient(sa);
					parent.cWaiting = true;
					while (parent.cWaiting) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
