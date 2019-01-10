/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stresstest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author hanv
 */
public class StressTestReplicator {

    private final List<BaseStressClient> clients;
    private final ScheduledThreadPoolExecutor generator;

    private String clientClassName;     // name of the client class
    private int generationSpeed = 250;  // interval between each client is connection
    private int totalCCU = 1;          // # of CCU
    public static int fromId = 1;   
    // # from userId
//    public static String zone = "IwinZone";
//    public static String host = "127.0.0.1";
//    public static int port = 9933;
    public static String balancerUrl = "http://10.8.34.5:18103/balancer/get-member";

    private Class<?> clientClass;
    private ScheduledFuture<?> generationTask;

    public StressTestReplicator(Properties config) {
        clients = new LinkedList<>();
        generator = new ScheduledThreadPoolExecutor(1);

        clientClassName = "stresstest.TestGameBaiCao";

        try {
            generationSpeed = Integer.parseInt(config.getProperty("generationSpeed"));
        } catch (NumberFormatException e) {
        }

        try {
            totalCCU = Integer.parseInt(config.getProperty("totalCCU"));
        } catch (NumberFormatException e) {
        }
        
        try {
            fromId = Integer.parseInt(config.getProperty("fromId"));
        } catch (NumberFormatException e) {
            System.out.println("error read fromId");
        }
        
        try {
            balancerUrl = config.getProperty("balancerUrl");
        } catch (NumberFormatException e) {
            System.out.println("error read balancerUrl");
        }
        
        System.out.printf("%s, %s, %s\n", clientClassName, generationSpeed, totalCCU);

        try {
            // Load main client class
            clientClass = Class.forName(clientClassName);

            // Prepare generation
            generationTask = generator.scheduleAtFixedRate(new GeneratorRunner(), 0, generationSpeed, TimeUnit.MILLISECONDS);
        } catch (ClassNotFoundException e) {
            System.out.println("Specified Client class: " + clientClassName + " not found! Quitting.");
        }
    }

    public void handleClientDisconnect(BaseStressClient client) {
        synchronized (clients) {
            clients.remove(client);
        }

        if (clients.isEmpty()) {
            System.out.println("===== TEST COMPLETE =====");
            System.exit(0);
        }
    }
    
    private JsonObject readBalancer() throws Exception {
        URL obj = new URL(balancerUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
//        System.out.println("balancer response: " + response.toString());
        
        JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject().getAsJsonObject("svgame");
        return json;
    }

    public static void main(String[] args) throws Exception {
        String defaultCfg = args.length > 0 ? args[0] : "config.properties";

        Properties props = new Properties();
        props.load(new FileInputStream(defaultCfg));

        new StressTestReplicator(props);
    }

    //=====================================================================
    private class GeneratorRunner implements Runnable {

        @Override
        public void run() {
            try {
                if (clients.size() < totalCCU) {
                    startupNewClient();
                } else {
                    generationTask.cancel(true);
                }
            } catch (Exception e) {
                System.out.println("ERROR Generating client: " + e.getMessage());
            }
        }

        private void startupNewClient() throws Exception {
            BaseStressClient client = (BaseStressClient) clientClass.newInstance();

            synchronized (clients) {
                clients.add(client);
            }

            client.setShell(StressTestReplicator.this);

            JsonObject json = readBalancer();
            String host = json.get("ip").getAsString();
            int port = json.get("port").getAsInt();
            String zone = json.get("zn").getAsString();
//            String host = "127.0.0.1";
//            int port=9933;
//            String zone="IwinZone";
            System.out.println("User join : " + (fromId ));
            client.startUp(fromId, host, port, zone);
            fromId++;
        }
    }
}
