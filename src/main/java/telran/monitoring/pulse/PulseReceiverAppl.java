package telran.monitoring.pulse;

import java.net.*;
import java.util.Arrays;
import java.util.logging.*;
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.regions.Regions;

public class PulseReceiverAppl {
    private static final int PORT = 5000;
    private static final int MAX_BUFFER_SIZE = 1500;
    static DatagramSocket socket;
    static AmazonDynamoDB client = AmazonDynamoDBAsyncClientBuilder.standard()
        .withRegion(Regions.US_EAST_1) 
        .build();
    static DynamoDB dynamo = new DynamoDB(client);
    static Table table = dynamo.getTable("pulse_values");
    static Logger logger = Logger.getLogger(PulseReceiverAppl.class.getName());
    private static final String LOGGING_LEVEL = System.getenv().getOrDefault("LOGGING_LEVEL","INFO");
    private static final int MAX_THRESHOLD_PULSE_VALUE = Integer.parseInt(System.getenv().getOrDefault("MAX_THRESHOLD_PULSE_VALUE", "210"));
    private static final int MIN_THRESHOLD_PULSE_VALUE = Integer.parseInt(System.getenv().getOrDefault("MIN_THRESHOLD_PULSE_VALUE", "40"));
    private static final int WARN_MAX_PULSE_VALUE = Integer.parseInt(System.getenv().getOrDefault("WARN_MAX_PULSE_VALUE", "180"));
    private static final int WARN_MIN_PULSE_VALUE = Integer.parseInt(System.getenv().getOrDefault("WARN_MIN_PULSE_VALUE", "55"));

    public static void main(String[] args) throws Exception {
        socket = new DatagramSocket(PORT);
        byte[] buffer = new byte[MAX_BUFFER_SIZE];
        LogManager.getLogManager().reset();
		try {
			logger.setLevel(Level.parse(LOGGING_LEVEL));
		} catch (IllegalArgumentException e) {
			logger.setLevel(Level.INFO); 
            logger.warning("Invalid LOGGING_LEVEL specified, defaulting to INFO");
		}
		Handler handlerConsole = new ConsoleHandler();
		handlerConsole.setFormatter(new SimpleFormatter());
		handlerConsole.setLevel(Level.FINEST);
		logger.addHandler(handlerConsole);
        
        
        logger.config("LOGGING_LEVEL: " + LOGGING_LEVEL);
        logger.config("MAX_THRESHOLD_PULSE_VALUE: " + MAX_THRESHOLD_PULSE_VALUE);
        logger.config("MIN_THRESHOLD_PULSE_VALUE: " + MIN_THRESHOLD_PULSE_VALUE);
        logger.config("WARN_MAX_PULSE_VALUE: " + WARN_MAX_PULSE_VALUE);
        logger.config("WARN_MIN_PULSE_VALUE: " + WARN_MIN_PULSE_VALUE);
        
        logger.info("Using DynamoDB table: "+ table.getTableName());
        
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, MAX_BUFFER_SIZE);
            socket.receive(packet);
            processReceivedData(buffer, packet);
        }
    }

    private static void processReceivedData(byte[] buffer, DatagramPacket packet) {
        String json = new String(Arrays.copyOf(buffer, packet.getLength()));
        logger.fine("Received SensorDataobject:"+ json);
        JSONObject jo = new JSONObject(json);
        int pulseValue = jo.getInt("value");
        Long patientId = jo.getLong("patientId");
        Long timestamp = jo.getLong("timestamp");
        logger.finer("Storing data - patientId: " + patientId + ", timestamp: " + timestamp);
   
        table.putItem(new PutItemSpec().withItem(Item.fromJSON(json)));
        if (pulseValue > WARN_MAX_PULSE_VALUE && pulseValue <= MAX_THRESHOLD_PULSE_VALUE) {
            logger.warning("Warning: High pulse value: " + pulseValue);
        } else if (pulseValue < WARN_MIN_PULSE_VALUE && pulseValue >= MIN_THRESHOLD_PULSE_VALUE) {
            logger.warning("Warning: Low pulse value: " + pulseValue);
        } else if (pulseValue > MAX_THRESHOLD_PULSE_VALUE) {
            logger.severe("Severe: Pulse value exceeds maximum threshold: " + pulseValue);
        } else if (pulseValue < MIN_THRESHOLD_PULSE_VALUE) {
            logger.severe("Severe: Pulse value below minimum threshold: " + pulseValue);
        }
    }
}
