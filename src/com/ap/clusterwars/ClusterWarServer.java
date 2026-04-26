package com.ap.clusterwars;

import com.ap.clusterwars.resources.ClusterResource;
import com.ap.clusterwars.resources.RAMStick;
import com.ap.clusterwars.resources.ServerNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClusterWarServer {

    private static final int PORT = 5000;
    public static final int GRID_SIZE = 40;
    public static final int NUM_RESOURCES = 30;
    public static final int CELL_SIZE = 20;

    private static ConcurrentHashMap<String, BotHandler> players = new ConcurrentHashMap<>();
    private static List<ClusterResource> resources = new CopyOnWriteArrayList<>();



    public ClusterWarServer() {
        generateResources(NUM_RESOURCES);
    }

    private void generateResources(int numResources) {
        Random r = new Random();
        for (int i = 0; i < numResources; i++) {
            int x = r.nextInt(GRID_SIZE);
            int y = r.nextInt(GRID_SIZE);

            // Decidiamo che tipo di risorsa creare (es. 70% Server, 30% RAM)
            double chance = r.nextDouble();


            if (chance < 0.70) {
                resources.add(new ServerNode(x, y));
            } else {
                resources.add(new RAMStick(x, y));
            }
        }
    }



    public void addPlayer(String name, BotHandler newHandler) {
        if (players.containsKey(name)) {
            // RICONNESSIONE: Recuperiamo i dati dal vecchio handler
            BotHandler oldData = players.get(name);

            // Trasferiamo lo stato dal vecchio al nuovo
            newHandler.setX(oldData.getX());
            newHandler.setY(oldData.getY());
            newHandler.setEnergy(oldData.getEnergy());
            newHandler.setScore(oldData.getScore()); // Se hai un campo score
            newHandler.setVisualX(oldData.getVisualX());
            newHandler.setVisualY(oldData.getVisualY());

            System.out.println("[RECONNECT] Bot " + name + " è tornato in gioco!");
        }
        players.put(name, newHandler);
    }

    public void removePlayer(String name) {
        players.remove(name);
    }

    public void updateGameState() {
        // 1. Applichiamo i movimenti
        for (BotHandler b : players.values()) {
            if (b.isDead()) {
                b.handleRespawn(); // Gestisce il countdown
                continue; // Salta il movimento e l'attacco per questo tick
            }
            b.executeMove(GRID_SIZE);
            // 2. Controllo risorse
            resources.removeIf(p -> {
                if (p.getX() == b.getX() && p.getY() == b.getY()) {
                    b.setEnergy(Math.min(100, b.getEnergy() + 30));
                    b.registerCollection();
                    return true;
                }
                return false;
            });
        }

        // 3. Risolviamo attacchi
        for (BotHandler b : players.values()) {
            if (b.getLastAction() != null && b.getLastAction().startsWith("ATTACK:")) {
                try {
                    String[] parts = b.getLastAction().split(":")[1].split(",");
                    int tx = Integer.parseInt(parts[0]);
                    int ty = Integer.parseInt(parts[1]);
                    for (BotHandler victim : players.values()) {
                        if (victim.getX() == tx && victim.getY() == ty && victim != b) {
//                            victim.setEnergy(victim.getEnergy() - 20);
                            victim.takeDamage(20);
                            IO.println(b.getName() + " colpisce " + victim.getName());
                            victim.registerHit();
                        }
                    }
                } catch (Exception e) { /* Comando malformato */ }
            }
        }

        // 4. Inviamo lo STATUS a tutti per il prossimo turno
        String resString = buildResourceString();
        for (BotHandler b : players.values()) {
            b.sendStatus(players.values(), resString);
        }
    }

    private String buildResourceString() {
        StringBuilder sb = new StringBuilder("R:");
        for (ClusterResource p : resources) sb.append(p.getType()).append(",").append(p.getX()).append(",").append(p.getY()).append(";");
        return sb.toString();
    }
//
//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//        // Disegna Risorse
//        g.setColor(Color.GREEN);
//        for (Point p : resources) g.fillRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE-2, CELL_SIZE-2);
//
//        // Disegna Bot
//        for (BotHandler b : players.values()) {
//            g.setColor(b.isHacker ? Color.RED : Color.CYAN);
//            g.fillRect(b.x * CELL_SIZE, b.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
//            g.setColor(Color.WHITE);
//            g.drawString(b.name + " (" + b.energy + ")", b.x * CELL_SIZE, b.y * CELL_SIZE - 5);
//        }
//    }

    /**
     * Avvia il thread che accetta le connessioni dai client
     */
    public void start() {
        // Thread per accettare connessioni
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Network Server attivo sulla porta " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // BotHandler deve avere accesso a serverLogic o alla mappa dei player
                    new Thread(new BotHandler(this,clientSocket)).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public List<ClusterResource> getResources() {
        return resources;
    }

    public List<BotHandler> getPlayers() {
        var playerList = new ArrayList<>(players.values());
        return playerList;
    }
}
