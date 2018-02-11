package net;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Used to simplify communication over a Serial port. Using the RXTX-library
 * (rxtx.qbang.org), one connection per instance of this class can be handled.
 * In addition to handling a connection, information about the available Serial
 * ports can be received using this class.
 * 
 * A separate {@link Thread} is started to handle messages that are being
 * received over the Serial interface.
 * 
 * @author Raphael Blatter (raphael@blatter.sg)
 * @author heavily using code examples from the RXTX-website (rxtx.qbang.org)
 */
public class Network {
	public InputStream inputStream;
	private OutputStream outputStream;
	/**
	 * The status of the connection.
	 */
	private boolean connected = false;
	/**
	 * The Thread used to receive the data from the Serial interface.
	 */
	private Thread reader;
	private SerialPort serialPort;
	/**
	 * Communicating between threads, showing the {@link #reader} when the
	 * connection has been closed, so it can {@link Thread#join()}.
	 */
	private boolean end = false;

	/**
	 * Link to the instance of the class implementing {@link net.Network_iface}.
	 */
	private Network_iface contact;
	/**
	 * <b>int</b> identifying the specific instance of the Network-class. While
	 * having only a single instance, 'id' is irrelevant. However, having more
	 * than one open connection (using more than one instance of {@link Network}
	 * ), 'id' helps identifying which Serial connection a message or a log
	 * entry came from.
	 */
	private int id;

	private int[] tempBytes;
	int numTempBytes = 0, numTotBytes = 0;

	/**
	 * @param id
	 *            <b>int</b> identifying the specific instance of the
	 *            Network-class. While having only a single instance,
	 *            {@link #id} is irrelevant. However, having more than one open
	 *            connection (using more than one instance of Network),
	 *            {@link #id} helps identifying which Serial connection a
	 *            message or a log entry came from.
	 * 
	 * @param contact
	 *            Link to the instance of the class implementing
	 *            {@link net.Network_iface}.
	 */
	public Network(int id, Network_iface contact) {
		this.contact = contact;
		this.id = id;
		tempBytes = new int[1024];
	}
	/**
	 * Just as {@link #Network(int, Network_iface)}, but with a default
	 * {@link #id} of 0. This
	 * constructor may mainly be used if only one Serial connection is needed at
	 * any time.
	 * 
	 * @see #Network(int, Network_iface)
	 */
	public Network(Network_iface contact) {
		this(0, contact);
	}

	/**
	 * This method is used to get a list of all the available Serial ports
	 * (note: only Serial ports are considered). Any one of the elements
	 * contained in the returned {@link Vector} can be used as a parameter in
	 * {@link #connect(String)} or {@link #connect(String, int)} to open a
	 * Serial connection.
	 * 
	 * @return A {@link Vector} containing {@link String}s showing all available
	 *         Serial ports.
	 */
	@SuppressWarnings("unchecked")
	public Vector<String> getPortList() {
		Enumeration<CommPortIdentifier> portList;
		Vector<String> portVect = new Vector<String>();
		portList = CommPortIdentifier.getPortIdentifiers();

		CommPortIdentifier portId;
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				portVect.add(portId.getName());
			}
		}
		contact.writeLog(id, "found the following ports:");
		for (int i = 0; i < portVect.size(); i++) {
			contact.writeLog(id, ("   " + (String) portVect.elementAt(i)));
		}

		return portVect;
	}

	/**
	 * Just as {@link #connect(String, int)}, but using 115200 bps as a default
	 * speed of the connection.
	 * 
	 * @param portName
	 *            The name of the port the connection should be opened to (see
	 *            {@link #getPortList()}).
	 * @return <b>true</b> if the connection has been opened successfully,
	 *         <b>false</b> otherwise.
	 * @see #connect(String, int)
	 */
	public boolean connect(String portName) {
		return connect(portName, 115200);
	}

	/**
	 * Opening a connection to the specified Serial port, using the specified
	 * speed. After opening the port, messages can be sent using
	 * {@link #writeSerial(String)} and received data will be packed into
	 * packets
	 * {@link net.Network_iface#parseInput(int, int, byte[])}.
	 * 
	 * @param portName
	 *            The name of the port the connection should be opened to (see
	 *            {@link #getPortList()}).
	 * @param speed
	 *            The desired speed of the connection in bps.
	 * @return <b>true</b> if the connection has been opened successfully,
	 *         <b>false</b> otherwise.
	 */
	public boolean connect(String portName, int speed) {
		CommPortIdentifier portIdentifier;
		boolean conn = false;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			if (portIdentifier.isCurrentlyOwned()) {
				contact.writeLog(id, "Error: Port is currently in use");
			} else {
				serialPort = (SerialPort) portIdentifier.open("RTBug_network",
						2000);
				serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				inputStream = serialPort.getInputStream();
				outputStream = serialPort.getOutputStream();

				SerialReader sreadrer = new SerialReader(inputStream);
				reader = (new Thread(sreadrer));
				end = false;
				reader.start();
				connected = true;
				contact.writeLog(id, "connection on " + portName
						+ " established");
				conn = true;
			}
		} catch (NoSuchPortException e) {
			contact.writeLog(id, "the connection could not be made");
			e.printStackTrace();
		} catch (PortInUseException e) {
			contact.writeLog(id, "the connection could not be made");
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			contact.writeLog(id, "the connection could not be made");
			e.printStackTrace();
		} catch (IOException e) {
			contact.writeLog(id, "the connection could not be made");
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * A separate class to use as the {@link net.Network#reader}. It is run as a
	 * separate {@link Thread} and manages the incoming data, and
	 * forwarding them using
	 * {@link net.Network_iface#parseInput(int, int, byte[])}.
	 * 
	 */
	private class SerialReader implements Runnable {
		InputStream in;

		public SerialReader(InputStream in) {
			this.in = in;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int len;
			try {
				while (!end) {
					if (in.available() > 0) {
						if ((len = this.in.read(buffer)) > -1) {
								contact.parseInput(id, len,buffer);
						}
					}
				}
			} catch (IOException e) {
				end = true;
				try {
					outputStream.close();
					inputStream.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				serialPort.close();
				connected = false;
				contact.networkDisconnected(id);
				contact.writeLog(id, "connection has been interrupted");
			}
		}
	}

	/**
	 * Simple function closing the connection held by this instance of
	 * {@link net.Network}. It also ends the Thread {@link net.Network#reader}.
	 * 
	 * @return <b>true</b> if the connection could be closed, <b>false</b>
	 *         otherwise.
	 */
	public boolean disconnect() {
		boolean disconn = true;
		end = true;
		try {
			reader.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			disconn = false;
		}
		try {
			outputStream.close();
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			disconn = false;
		}
		serialPort.close();
		connected = false;
		contact.networkDisconnected(id);
		contact.writeLog(id, "connection disconnected");
		return disconn;
	}

	/**
	 * @return Whether this instance of {@link net.Network} has currently an
	 *         open connection of not.
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * This method is included as a legacy. Depending on the other side of the
	 * Serial port, it might be easier to send using a String.
	 * 
	 * If a connection is open, a {@link String} can be sent over the Serial
	 * port using this function. If no connection is available, <b>false</b> is
	 * returned and a message is sent using
	 * {@link net.Network_iface#writeLog(int, String)}.
	 * 
	 * @param message
	 *            The {@link String} to be sent over the Serial connection.
	 * @return <b>true</b> if the message could be sent, <b>false</b> otherwise.
	 */
	public boolean writeSerial(String message) {
		boolean success = false;
		if (isConnected()) {
			try {
				outputStream.write(message.getBytes());
				success = true;
			} catch (IOException e) {
				disconnect();
			}
		} else {
			contact.writeLog(id, "No port is connected.");
		}
		return success;
	}

	/**
	 * If a connection is open, an <b>int</b> between 0 and 255
	 * can be sent over the Serial port using this
	 * function. If no connection is available, <b>false</b>
	 * is returned and a message is sent using
	 * {@link net.Network_iface#writeLog(int, String)}.
	 * 
	 * @param numBytes
	 *            The number of bytes to send over the Serial port.
	 * @param message
	 *            [] The array of<b>byte</b>s to be sent over the Serial
	 *            connection (between 0 and 256).
	 * @return <b>true</b> if the message could be sent, <b>false</b> otherwise
	 *         .
	 */
	public boolean writeSerial(int numBytes, byte message[]) {
		boolean success = true;
		if ( isConnected()) {
			try {
				outputStream.write(message,0,numBytes);
//				System.out.print("out: len: " +  numBytes + ", message: ");
//				for( int i = 0; i<numBytes;i++)
//					System.out.print(String.format("%02x", message[i]) + " ");
//				System.out.println();
			} catch (IOException e) {
				success = false;
				disconnect();
			}
		} else {
			contact.writeLog(id, "No port is connected.");
		}
		return success;
	}
}
