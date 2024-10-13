package telran.monitoring.pulse;
import java.net.*;
import java.util.Arrays;

import org.json.JSONObject;
public class PulseReceiverAppl {
private static final int PORT = 5000;
private static final int MAX_BUFFER_SIZE = 1500;
static DatagramSocket socket;
	public static void main(String[] args) throws Exception {
		socket = new DatagramSocket(PORT);
		byte [] buffer = new byte[MAX_BUFFER_SIZE];
		while(true) {
			DatagramPacket packet = new DatagramPacket(buffer, PORT);
			socket.receive(packet);
			processReceivedData(buffer,packet);
		}
	}
	private static void processReceivedData(byte[] buffer, DatagramPacket packet) {
		String json = new String(Arrays.copyOf(buffer, packet.getLength()));
		JSONObject js = new JSONObject(json);
		long pat =js.getLong("patientId");
		if (pat ==1){
			System.out.println(json);
		}
		
	}

}
