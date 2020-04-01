import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import captainalm.network.oc.OCNetworkClient;
import captainalm.network.oc.OCNetworkListener;

public class Main {
	public static String ipAddress;
	public static int port;

	public static HashMap<String, String> settings = new HashMap<String, String>();
	public static String cache;
	public static List<String> addrsv4;
	public static List<String> addrsv6;

	public static void main(String[] args) {
		writeLine("Open Computers Daemon Hoster (OCDH) : (C) Captain ALM 2020.");
		writeLine("License: BSD 2-Clause.");
		try {
			addrsv4 = getInterfaceAddresses(Inet4Address.class);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		try {
			addrsv6 = getInterfaceAddresses(Inet6Address.class);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		if (args != null) {
			if (args.length < 2) {
				printUsage();
				System.exit(1);
			} else {
				decryptArgs(args);
				if (settings.containsKey("mode")) {
					if (settings.get("mode").toLowerCase().equals("h")) {
						hoster();
					} else if (settings.get("mode").toLowerCase().equals("a")) {
						accessor();
					}
				}
			}
		} else {
			printUsage();
			System.exit(1);
		}
		System.exit(0);
	}

	public static void hoster() {
		writeLine("Hosting Mode!");
		if (settings.containsKey("target")) {
			writeLine("[INFO] : Target File : " + settings.get("target"));
		}
		cache = "";
		if (settings.containsKey("cache") && settings.containsKey("target")) {
			try {
				cache = loadFile(settings.get("target"));
				writeLine("[INFO] : File Cached!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		serverRuntime();
	}

	public static void accessor() {
		writeLine("Accessor Mode!");
		serverRuntime();
	}
	
	public static void serverRuntime() {
		InetSocketAddress address = null;
		if (ipAddress == null) {
			address = new InetSocketAddress(port);
		} else {
			address = new InetSocketAddress(ipAddress, port);
		}
		writeLine("[INFO] : Address Setup!");
		List<String> wl = new ArrayList<String>();
		if (settings.containsKey("whitelist")) {
			
			wl.addAll(Arrays.asList((settings.get("whitelist").split(","))));
		}
		OCNetworkListener server = new OCNetworkListener(address, wl);
		writeLine("[INFO] : Listener Started!");
		writeLine("[INFO] : Listener 'Address:Port' : " + server.getListeningAddress().getAddress().getHostAddress()
				+ ":" + server.getListeningAddress().getPort());
		writeLine("[INFO] : Open Addresses 'Address:Port' :");
		if (ipAddress == null) {
			List<String> addrsT = new ArrayList<String>();
			addrsT.addAll(addrsv4);
			addrsT.addAll(addrsv6);
			for (String c : addrsT) {
				writeLine(c + ":" + port);
			}
			addrsT.clear();
			addrsT = null;
		} else {
			writeLine(ipAddress + ":" + port);
		}
		boolean exec = true;
		while (exec) {
			if (server.getIsThereAcceptedClient()) {
				OCNetworkClient client = server.getAcceptedClient();
				writeLine("[INFO] : Client Accepted!");
				writeLine("[INFO] : Client 'Address:Port' : " + client.getRemoteAddress().getAddress().getHostAddress()
						+ ":" + client.getRemoteAddress().getPort());
				handleProtocol(client);
				server.returnAcceptedClient(client);
				writeLine("[INFO] : Client Disposed!");
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
		server.close();
		server = null;
	}

	public static void handleProtocol(OCNetworkClient clientIn) {
		String prot = clientIn.receiveProtocol();
		if (prot.equals("1")) {
			writeLine("[INFO] : Sending...");
			String data = "";
			if (settings.containsKey("target") && !settings.containsKey("cache")) {
				writeLine("[INFO] : Sending : Loading Data...");
				try {
					data = loadFile(settings.get("target"));
				} catch (IOException e) {
					data = "";
					e.printStackTrace();
				}
			} else if (settings.containsKey("cache")) {
				writeLine("[INFO] : Sending : Retrieving Data...");
				data = cache;
			}
			writeLine("[INFO] : Sending : Sending Handshake...");
			clientIn.sendHandshake("1");
			writeLine("[INFO] : Sending : Waiting For Handshake...");
			boolean hand1Succ = clientIn.receiveHandshake("1");
			if (hand1Succ) {
				writeLine("[INFO] : Sending : Sending Data...");
				clientIn.sendData(data);
				writeLine("[INFO] : Sending : Waiting For Handshake...");
				clientIn.receiveHandshake("1");
			}
		} else if (prot.equals("2")) {
			writeLine("[INFO] : Receiving...");
			writeLine("[INFO] : Receiving : Sending Handshake...");
			clientIn.sendHandshake("1");
			writeLine("[INFO] : Receiving : Waiting For Data...");
			String data = clientIn.receiveData();
			writeLine("[INFO] : Receiving : Processing Data...");
			if (data.contains("\r") && !data.contains("\n")) {
				data = data.replace("\r", "\r\n");
			}
			if (data.contains("\n") && !data.contains("\r")) {
				data = data.replace("\n", "\r\n");
			}
			if (settings.containsKey("cache")) {
				writeLine("[INFO] : Receiving : Caching Data...");
				cache = data;
			}
			if (settings.containsKey("target")) {
				writeLine("[INFO] : Receiving : Saving Data...");
				try {
					saveFile(settings.get("target"), data);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			writeLine("[INFO] : Receiving : Sending Handshake...");
			clientIn.sendHandshake("1");
		} else if (prot.equals("3")) {
			write("[INFO] : Access Mode : ");
			clientIn.sendHandshake("1");
			String protam = clientIn.receiveProtocol();
			if (protam.equals("1") && ! settings.containsKey("writeonly")) {
				writeLine("Send");
				writeLine("[INFO] : Sending : Sending Handshake...");
				clientIn.sendHandshake("1");
				writeLine("[INFO] : Sending : Receiving Path...");
				Integer sl = clientIn.receiveSmallNumber();
				if (sl != 0) {
					Integer l = clientIn.receiveNumber(sl);
					if (l != 0) {
						String nom = clientIn.receiveData(l);
						writeLine("[INFO] : Reading : " + nom);
						try {
							String data = loadFile(nom);
							writeLine("[INFO] : Sending : Waiting For Handshake...");
							if (clientIn.receiveHandshake("1")) {
								writeLine("[INFO] : Sending : " + nom);
								clientIn.sendSmallNumber(Integer.toString(data.length()).length());
								clientIn.sendNumber(data.length());
								clientIn.sendData(data);
							}
							writeLine("[INFO] : Sending : Waiting For Handshake...");
							clientIn.receiveHandshake("1");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else if (protam.equals("2") && ! settings.containsKey("readonly")) {
				writeLine("Receive");
				writeLine("[INFO] : Receiving : Sending Handshake...");
				clientIn.sendHandshake("1");
				writeLine("[INFO] : Receiving : Receiving Path...");
				Integer sl = clientIn.receiveSmallNumber();
				if (sl != 0) {
					Integer l = clientIn.receiveNumber(sl);
					if (l != 0) {
						String nom = clientIn.receiveData(l);
						writeLine("[INFO] : Receiving : Sending Handshake...");
						clientIn.sendHandshake("1");
						writeLine("[INFO] : Receiving : " + nom);
						sl = clientIn.receiveSmallNumber();
						if (sl != 0) {
							l = clientIn.receiveNumber(sl);
							if (l != 0) {
								String data = clientIn.receiveData(l);
								writeLine("[INFO] : Writing : " + nom);
								try {
									saveFile(nom,data);
									writeLine("[INFO] : Receiving : Sending Handshake...");
									clientIn.sendHandshake("1");
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			} else if (protam.equals("3") && settings.containsKey("creation")) {
				writeLine("Creation");
				writeLine("[INFO] : Creating : Sending Handshake...");
				clientIn.sendHandshake("1");
				writeLine("[INFO] : Creating : Receiving Path...");
				Integer sl = clientIn.receiveSmallNumber();
				if (sl != 0) {
					Integer l = clientIn.receiveNumber(sl);
					if (l != 0) {
						String nom = clientIn.receiveData(l);
						writeLine("[INFO] : Creating : " + nom);
						try {
							createFile(nom);
							writeLine("[INFO] : Creating : Sending Handshake...");
							clientIn.sendHandshake("1");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else if (protam.equals("4") && settings.containsKey("deletion")) {
				writeLine("Deletion");
				writeLine("[INFO] : Deleting : Sending Handshake...");
				clientIn.sendHandshake("1");
				writeLine("[INFO] : Deleting : Receiving Path...");
				Integer sl = clientIn.receiveSmallNumber();
				if (sl != 0) {
					Integer l = clientIn.receiveNumber(sl);
					if (l != 0) {
						String nom = clientIn.receiveData(l);
						writeLine("[INFO] : Deleting : " + nom);
						try {
							deleteFile(nom);
							writeLine("[INFO] : Deleting : Sending Handshake...");
							clientIn.sendHandshake("1");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else if (protam.equals("5") && settings.containsKey("enumeration")) {
				writeLine("Enumeration");
				writeLine("[INFO] : Enumerating : Sending Handshake...");
				clientIn.sendHandshake("1");
				writeLine("[INFO] : Enumerating : Receiving Path...");
				Integer sl = clientIn.receiveSmallNumber();
				if (sl != 0) {
					Integer l = clientIn.receiveNumber(sl);
					if (l != 0) {
						String nom = clientIn.receiveData(l);
						writeLine("[INFO] : Enumerating : " + nom);
						try {
							String result = "";
							if (Files.exists(Paths.get(nom))) {
								if (Files.isDirectory(Paths.get(nom))) {
									List<Path> enr = Files.walk(Paths.get(nom), 1).collect(Collectors.toList());
									if (enr.size() > 0) {
										if (enr.size() == 1) {
											result = enr.get(0).toString();
										} else {
											for (int i=0;i<(enr.size() - 1);i++) {
												result = result + enr.get(i).toString() + "\r\n";
											}
											result = result + enr.get(enr.size() - 1).toString();
										}
									}
									enr.clear();
									enr = null;
								} else {
									result = Long.toString(Files.size(Paths.get(nom)));
								}
							}
							writeLine("[INFO] : Enumerating : Waiting For Handshake...");
							if (clientIn.receiveHandshake("1")) {
								writeLine("[INFO] : Enumerating : Sending Enumeration...");
								clientIn.sendSmallNumber(Integer.toString(result.length()).length());
								clientIn.sendNumber(result.length());
								clientIn.sendData(result);
							}
							writeLine("[INFO] : Enumerating : Waiting For Handshake...");
							clientIn.receiveHandshake("1");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				writeLine("Unknown");
				clientIn.sendHandshake("0");
			}
		}
	}

	public static String loadFile(String target) throws IOException {
		return new String(Files.readAllBytes(Paths.get(target)), StandardCharsets.ISO_8859_1);
	}

	public static void saveFile(String target, String contents) throws IOException {
		Path tp = Paths.get(target).getParent();
		if (tp != null) {
			if (! Files.exists(tp)) {Files.createDirectories(tp);}	
		}
		Files.write(Paths.get(target), contents.getBytes(StandardCharsets.ISO_8859_1));
	}
	
	public static void createFile(String target) throws IOException {
		Path tp = Paths.get(target).getParent();
		if (tp != null) {
			if (! Files.exists(tp)) {Files.createDirectories(tp);}	
		}
		Files.createFile(Paths.get(target));
	}
	
	public static void deleteFile(String target) throws IOException {
		Files.delete(Paths.get(target));
	}

	public static void decryptArgs(String[] args) {
		try {
			ipAddress = verifyInterface(args[0]);
		} catch (SocketException e1) {
			ipAddress = null;
			e1.printStackTrace();
		}
		try {
			port = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			port = 0;
			e.printStackTrace();
		}
		for (int i = 2; i < args.length; i++) {
			String carg = args[i];
			boolean hasEquals = carg.contains("=");
			boolean isSwitch = carg.startsWith("-");
			String cSwitch = "";
			String cValue = "";
			if (isSwitch && !hasEquals) {
				cSwitch = carg.substring(1).toLowerCase();
			} else if (isSwitch && hasEquals) {
				cSwitch = carg.substring(1, carg.indexOf("=")).toLowerCase();
				cValue = carg.substring(carg.indexOf("=") + 1);
			}
			if (!settings.containsKey(cSwitch)) {
				settings.put(cSwitch, cValue);
			}
		}
	}

	public static List<String> getInterfaceAddresses(Class<? extends InetAddress> of) throws SocketException {
		List<String> toret = new ArrayList<String>();
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		for (NetworkInterface netc : Collections.list(nets)) {
			if (netc.isUp()) {
				Enumeration<InetAddress> inetAddresses = netc.getInetAddresses();
				for (InetAddress cadd : Collections.list(inetAddresses)) {
					if (of.isInstance(cadd)) {
						String sadd = cadd.getHostAddress();
						if (sadd.contains("%")) {
							sadd = sadd.substring(0, sadd.indexOf("%"));
						}
						toret.add(sadd);
					}
				}
			}
		}
		return toret;
	}

	public static String verifyInterface(String in) throws SocketException {
		boolean isContained = false;
		List<String> addrsT = new ArrayList<String>();
		addrsT.addAll(addrsv4);
		addrsT.addAll(addrsv6);
		for (String c : addrsT) {
			if (c.equals(in)) {
				isContained = true;
				break;
			}
		}
		addrsT.clear();
		addrsT = null;
		if (isContained) {
			return in;
		} else {
			return null;
		}
	}

	public static void printUsage() {
		writeLine("");
		writeLine("Usage:");
		writeLine(
				"java/javaw -jar OCDH.jar <listening IP Address> <listening Port> [-mode=<MODE>] [-whitelist=<IP Address [Seperated By ,]>] [-target=<target file path>] [-cache] [-enumeration] [-creation] [-deletion] [-writeonly] [-readonly]");
		writeLine("");
		writeLine("-mode=<MODE> : allows to select a Hosting Mode.");
		writeLine("-whitelist=<IP Address [Seperated By ,]> : allows an IP Address to connect, if there is no whitelist switch then any IP Address can connect.");
		writeLine("-target=<target file path> : allows to select a file for hosting (File Host Mode Only).");
		writeLine("-cache : caches the target file once (File Host Mode Only).");
		writeLine("-enumeration : allows for file/directory size/enumeration (File Access Mode Only).");
		writeLine("-creation : allows for file/directory creation (File Access Mode Only).");
		writeLine("-deletion : allows for file/directory deletion (File Access Mode Only).");
		writeLine("-readonly : disallows write access for files (File Access Mode Only).");
		writeLine("-writeonly : disallows read access for files (File Access Mode Only).");
		writeLine("");
		writeLine("MODE:");
		writeLine("H : File Host Mode, Hosts a single file for access.");
		writeLine("A : File Access Mode, Allows file system access.");
	}

	public static void write(String stringIn) {
		System.out.print(stringIn);
	}

	public static void writeLine(String stringIn) {
		System.out.println(stringIn);
	}

	public static void writeError(String stringIn) {
		System.err.print(stringIn);
	}

	public static void writeErrorLine(String stringIn) {
		System.err.println(stringIn);
	}
}
