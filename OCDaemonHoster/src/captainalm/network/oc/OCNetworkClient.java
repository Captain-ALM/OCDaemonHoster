package captainalm.network.oc;

/**
 * 
 */
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * The Open Computers Network Client.
 * 
 * @author Captain ALM
 */
public class OCNetworkClient {
	private Socket sock;
	private InputStream strmIn;
	private OutputStream strmOut;
	private SocketAddress remoteAddress;
	private SocketAddress localAddress;
	private boolean connected;

	protected OCNetworkClient(Socket socketIn) throws IOException {
		sock = socketIn;
		if (sock != null) {
			if (sock.isConnected()) {
				remoteAddress = sock.getRemoteSocketAddress();
				localAddress = sock.getLocalSocketAddress();
				strmIn = sock.getInputStream();
				strmOut = sock.getOutputStream();
				connected = true;
			} else {
				connected = false;
			}
		} else {
			connected = false;
		}
	}

	public InputStream getReceivingStream() {
		return strmIn;
	}

	public OutputStream getSendingStream() {
		return strmOut;
	}

	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) remoteAddress;
	}

	public InetSocketAddress getLocalAddress() {
		return (InetSocketAddress) localAddress;
	}

	public Socket getSocket() {
		return sock;
	}

	public boolean sendHandshake(String chIn) {
		if (chIn == null) {
			return false;
		}
		if (strmOut != null && chIn.length() == 1) {
			try {
				strmOut.write(chIn.substring(0, 1).getBytes(StandardCharsets.ISO_8859_1));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return false;
	}

	public String receiveProtocol() {
		if (strmIn != null) {
			try {
				String prot = new String(new byte[] { (byte) strmIn.read() }, StandardCharsets.ISO_8859_1);
				return prot;
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return "";
	}

	public boolean receiveHandshake(String chIn) {
		if (chIn == null) {
			return false;
		}
		if (strmIn != null && chIn.length() == 1) {
			try {
				boolean test = new String(new byte[] { (byte) strmIn.read() }, StandardCharsets.ISO_8859_1)
						.equals(chIn.substring(0, 1));
				return test;
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return false;
	}

	public boolean sendData(String data) {
		if (data == null) {
			return false;
		}
		if (strmOut != null && data.length() > 0) {
			try {
				strmOut.write(data.getBytes(StandardCharsets.ISO_8859_1));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return false;
	}

	public String receiveData() {
		String toret = "";
		if (strmIn != null) {
			try {
				int lout = 0;
				while (strmIn.available() < 1 && lout < 50) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
					lout++;
				}
				int len = strmIn.available();
				byte[] bufferIn = new byte[len];
				int res = strmIn.read(bufferIn, 0, len);
				if (res == -1) {
					connected = false;
				} else {
					connected = true;
				}
				toret = new String(bufferIn, StandardCharsets.ISO_8859_1);
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
				toret = "";
			}
		}
		return toret;
	}
	
	public boolean sendSmallNumber(Integer numIn) {
		if (strmOut != null && numIn > -1 && numIn < 10) {
			String numStr = numIn.toString();
			try {
				strmOut.write(numStr.substring(0, 1).getBytes(StandardCharsets.ISO_8859_1));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return false;
	}
	
	public boolean sendNumber(Integer numIn) {
		if (strmOut != null) {
			String numStr = numIn.toString();
			try {
				strmOut.write(numStr.getBytes(StandardCharsets.ISO_8859_1));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return false;
	}
	
	public Integer receiveSmallNumber() {
		if (strmIn != null) {
			try {
				return Integer.parseInt(new String(new byte[] { (byte) strmIn.read() }, StandardCharsets.ISO_8859_1));
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
			}
		}
		return 0;
	}
	
	public Integer receiveNumber(Integer lIn) {
		Integer toret = 0;
		if (strmIn != null) {
			try {
				int len = lIn;
				byte[] bufferIn = new byte[len];
				int res = strmIn.read(bufferIn, 0, len);
				if (res == -1) {
					connected = false;
				} else {
					connected = true;
				}
				toret = Integer.parseInt(new String(bufferIn, StandardCharsets.ISO_8859_1));
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				connected = false;
				toret = 0;
			}
		}
		return toret;
	}
	
	public String receiveData(Integer lIn) {
		String toret = "";
		if (strmIn != null) {
			try {
				int len = lIn;
				byte[] bufferIn = new byte[len];
				int res = strmIn.read(bufferIn, 0, len);
				if (res == -1) {
					connected = false;
				} else {
					connected = true;
				}
				toret = new String(bufferIn, StandardCharsets.ISO_8859_1);
			} catch (IOException e) {
				e.printStackTrace();
				connected = false;
				toret = "";
			}
		}
		return toret;
	}

	public void invokeConnectionCheck() {
		try {
			int res = strmIn.read();
			if (res == -1) {
				connected = false;
			} else {
				connected = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			connected = false;
		}
	}

	public boolean getIsConnected() {
		return connected;
	}

	public void shutdown() {
		if (!sock.isInputShutdown()) {
			try {
				sock.shutdownInput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		connected = false;
		strmIn = null;
		if (!sock.isOutputShutdown()) {
			try {
				sock.shutdownOutput();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		strmOut = null;
		connected = false;
	}

	public void close() {
		if (!sock.isClosed()) {
			try {
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		connected = false;
		sock = null;
	}
}
