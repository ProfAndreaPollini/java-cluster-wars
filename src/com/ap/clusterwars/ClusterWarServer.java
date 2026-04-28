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

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ClusterWarServer {
    public enum GameState { COUNTDOWN, RUNNING, FINISHED }

    private static final int PORT = 5000;
    public static final int GRID_SIZE = 80;
    public static final int NUM_RESOURCES = 120;
    public static final int CELL_SIZE = 10;

    private static ConcurrentHashMap<String, BotHandler> players = new ConcurrentHashMap<>();
    private static List<ClusterResource> resources = new CopyOnWriteArrayList<>();

    private static Map<String, Integer> globalHistory = new HashMap<>();
    private static final String HISTORY_FILE = "match_history.json";

    private GameState currentState = GameState.COUNTDOWN;
    private int timerTicks = 10; // 3 secondi per il countdown iniziale
    private int matchDuration = 180; // 3 minuti di gioco (180 secondi)
    private final int POST_MATCH_PAUSE = 20; // 20 secondi per vedere la classifica

    private int currentMin = 0;
    private int currentMax = GRID_SIZE - 1;

    public ClusterWarServer() {
        loadHistory();
        generateResources(NUM_RESOURCES);
    }


    private void loadHistory() {
        try {
            if (!Files.exists(Paths.get(HISTORY_FILE))) return;

            String content = Files.readString(Paths.get(HISTORY_FILE));
            // Parsing manuale "ultra-light" se non vuoi librerie esterne
            // Cerca i pattern "ip": "..." e "score": ...
            content = content.replace("[", "").replace("]", "").replace("{", "");
            String[] entries = content.split("},");

            for (String entry : entries) {
                try {
                    String ip = entry.split("\"ip\": \"")[1].split("\"")[0];
                    int score = Integer.parseInt(entry.split("\"score\": ")[1].split("\\n|\\r|,")[0].trim());
                    globalHistory.put(ip, score);
                } catch (Exception e) { /* riga malformata o vuota */ }
            }
            System.out.println("Storico caricato: " + globalHistory.size() + " IP trovati.");
        } catch (IOException e) {
            System.err.println("Impossibile caricare lo storico.");
        }
    }
    private void saveResultsToJson() {

        for (BotHandler b : players.values()) {
            globalHistory.put(b.getClientIP(), b.getScore());
        }

        StringBuilder json = new StringBuilder("[\n");
        int count = 0;
        for (Map.Entry<String, Integer> entry : globalHistory.entrySet()) {
            json.append("  {\n");
            json.append("    \"ip\": \"").append(entry.getKey()).append("\",\n");
            json.append("    \"score\": ").append(entry.getValue()).append("\n");
            json.append("  }");
            if (++count < globalHistory.size()) json.append(",");
            json.append("\n");
        }
        json.append("]");

        try {
            Files.writeString(Paths.get(HISTORY_FILE), json.toString());
            System.out.println("Storico salvato su disco.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public GameState getCurrentState() { return currentState; }
    public int getTimer() { return timerTicks; }

    private void generateResources(int numResources) {
        Random r = new Random();
//        for (int i = 0; i < numResources; i++) {
//            int x = r.nextInt(GRID_SIZE);
//            int y = r.nextInt(GRID_SIZE);
//
//            // Decidiamo che tipo di risorsa creare (es. 70% Server, 30% RAM)
//            double chance = r.nextDouble();
//
//
//            if (chance < 0.70) {
//                resources.add(new ServerNode(x, y));
//            } else {
//                resources.add(new RAMStick(x, y));
//            }
//        }
        int created = 0;
        while (created < numResources) {
            int x = r.nextInt(GRID_SIZE);
            int y = r.nextInt(GRID_SIZE);

            // Controlla se la cella è già occupata da un'altra risorsa
            boolean occupied = resources.stream().anyMatch(res -> res.getX() == x && res.getY() == y);

            if (!occupied) {
                double chance = r.nextDouble();
                if (chance < 0.70) resources.add(new ServerNode(x, y));
                else resources.add(new RAMStick(x, y));
                created++;
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
//            if (newHandler.getEnergy() == 0) {
//                newHandler.setEnergy(100);
//                newHandler.setScore(oldData.getScore()/2);
//            } else {
//                newHandler.setScore(oldData.getScore()); // Se hai un campo score
//            }
            newHandler.setVisualX(oldData.getVisualX());
            newHandler.setVisualY(oldData.getVisualY());

            System.out.println("[RECONNECT] Bot " + name + " è tornato in gioco!");
        }

        String clientIP = newHandler.getClientIP();

        // 1. Controlliamo se è una riconnessione a caldo (stesso nome in questa manche)
        if (players.containsKey(name)) {
            BotHandler oldData = players.get(name);
            newHandler.setScore(oldData.getScore());
            newHandler.setEnergy(oldData.getEnergy());
            System.out.println("[RECONNECT] " + name + " ha ripreso la sessione.");
        }
        // 2. Controlliamo se è un nuovo accesso ma l'IP ha punti da manche precedenti
        else if (globalHistory.containsKey(clientIP)) {
            int oldScore = globalHistory.get(clientIP);
            newHandler.setScore(oldScore);
            System.out.println("[HISTORY] IP " + clientIP + " ha recuperato " + oldScore + " punti.");
        }

        players.put(name, newHandler);

    }

    public void removePlayer(String name) {
        players.remove(name);
    }

    public void updateGameState() {

        if (currentState == GameState.COUNTDOWN) {
            if (timerTicks > 0) {
                timerTicks--;
            } else {
                currentState = GameState.RUNNING;
                currentMax = GRID_SIZE-1;
                currentMin = 0;
                timerTicks = matchDuration; // Carica il timer del match
            }
            return; // Non muovere nulla durante il countdown
        }

        if (currentState == GameState.RUNNING) {
            timerTicks--;
            if (timerTicks <= 0) {
                currentState = GameState.FINISHED;
                timerTicks = POST_MATCH_PAUSE;
                saveResultsToJson(); // Salvataggio a fine match
            }

            // Se il match dura 180s, iniziamo a stringere a 90s
            if (timerTicks < 90 && timerTicks % 10 == 0) {
                // Ogni 10 secondi la mappa si stringe di 1 cella per lato
                if (currentMin < currentMax - 5) { // Lasciamo almeno un 5x5 al centro
                    currentMin++;
                    currentMax--;
                    System.out.println("[STORM] La zona si restringe! Range: " + currentMin + "-" + currentMax);
                }
            }


            // 1. Applichiamo i movimenti
            for (BotHandler b : players.values()) {
                if (b.isDead()) {
                    b.handleRespawn(); // Gestisce il countdown
                    continue; // Salta il movimento e l'attacco per questo tick
                }
                // --- CONTROLLO DANNO DA ZONA ---
                if (b.getX() < currentMin || b.getX() > currentMax ||
                        b.getY() < currentMin || b.getY() > currentMax) {
                    b.takeDamage(10); // 10 danni a ogni tick fuori zona!
                }

                // --- NUOVI COMANDI ---

                // 1. SCAN: Aumenta la vista per questo turno (costo: 5 energia)
                if (b.getLastAction().equals("SCAN")) {
                    if (b.getEnergy() >= 5) {
                        b.setEnergy(b.getEnergy() - 5);
                        b.setViewDistance(20); // Raddoppia la vista temporaneamente
                    }
                } else {
                    b.setViewDistance(7); // Reset della vista se non usa SCAN
                }

                // 2. SHIELD: Protezione dai danni (costo: 2 HP per tick)
                if (b.getLastAction().equals("SHIELD")) {
                    if (b.getEnergy() > 2) {
                        b.setEnergy(b.getEnergy() - 2);
                        b.setShielded(true); // Devi aggiungere questo boolean in BotHandler
                    }
                } else {
                    b.setShielded(false);
                }

                // 3. REPAIR: Converte Score in Energia (costo: 50 RAM)
                if (b.getLastAction().equals("REPAIR")) {
                    if (b.getScore() >= 50 && b.getEnergy() < 100) {
                        b.setScore(b.getScore() - 50);
                        b.setEnergy(Math.min(100, b.getEnergy() + 20));
                    }
                }

                if (b.getLastAction().equals("WAIT")) {
                    b.setEnergy(Math.min(100, b.getEnergy() + 1));
                }

                if (b.getLastAction().startsWith("DASH:")) {
                    if (b.getEnergy() >= 10) {
                        b.setEnergy(b.getEnergy() - 10);
                        var dir = b.getLastAction().substring(5);
                        b.setLastAction(String.format("MOVE_%s", dir));

                        // Esegue il movimento 3 volte invece di una
                        for(int i=0; i<3; i++) b.executeMove(GRID_SIZE);
                    }
                }

                b.executeMove(GRID_SIZE);
                // 2. Controllo risorse
                resources.removeIf(p -> {
                    if (p.getX() == b.getX() && p.getY() == b.getY()) {
                        b.setEnergy(Math.min(100, b.getEnergy() + 30));
                        b.registerCollection();
                        p.applyEffect(b);
                        return true;
                    }
                    return false;
                });
            }

            // Rimuoviamo anche le risorse che finiscono fuori zona
            resources.removeIf(p -> p.getX() < currentMin || p.getX() > currentMax ||
                    p.getY() < currentMin || p.getY() > currentMax);

            // Se le risorse scendono sotto il 30% del totale iniziale
            if (resources.size() < (NUM_RESOURCES * 0.3)) {
                int toSpawn = 5; // Ne aggiungiamo un gruppetto alla volta
                generateResources(toSpawn);
                System.out.println("[SERVER] Risorse scarse! Spawnate " + toSpawn + " nuove risorse.");
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

            var sb = new StringBuilder();
            sb.append("ZONE:").append(currentMin).append(",").append(currentMax).append("|");

            for (BotHandler b : players.values()) {
                String resString = buildResourceString(b);
                b.sendStatus(players.values(), sb.toString()+resString);
            }

        }

        if (currentState == GameState.FINISHED) {
            if (timerTicks > 0) {
                timerTicks--;
            } else {
                // IL CICLO RICOMINCIA
                resetMatch();
                currentState = GameState.COUNTDOWN;
                timerTicks = 10; // Nuovo countdown
            }
        }
    }



    private void resetMatch() {
        System.out.println("--- RESET MATCH: Rigenerazione arena ---");

        // 1. Puliamo e rigeneriamo le risorse
        resources.clear();
        generateResources(NUM_RESOURCES);

        // 2. Resettiamo i bot connessi per la nuova manche
        for (BotHandler b : players.values()) {
            b.setEnergy(100);
            b.setDead(false);
            // Posizione casuale di partenza
            Random r = new Random();
            b.setX(r.nextInt(GRID_SIZE));
            b.setY(r.nextInt(GRID_SIZE));
            b.setVisualX(b.getX());
            b.setVisualY(b.getY());

            // Nota: NON resettiamo b.getScore() perché vogliamo che i punti
            // siano cumulativi tra le manche, come salvato nel JSON.
        }
    }

    private String buildResourceString(BotHandler bot) {
        StringBuilder sb = new StringBuilder("R:");
        for (ClusterResource p : resources) {
            if ( bot.distance(p) <bot.getViewDistance()) {
                sb.append(p.getType()).append(",").append(p.getX()).append(",").append(p.getY()).append(";");
            }
        }
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

//                    String clientIP = clientSocket.getInetAddress().getHostAddress();
//
//
//                    long activeConnections = players.values().stream()
//                            .filter(b -> b.getClientIP().equals(clientIP))
//                            .count();
//
//                    if (activeConnections >= 1) { // Limite di 1 bot per postazione
//                        System.out.println("[SECURITY] Rifiutata connessione multipla da: " + clientIP);
//                        clientSocket.close(); // Chiudi subito senza creare il thread
//                        continue;
//                    }
                    // BotHandler deve avere accesso a serverLogic o alla mappa dei player
                    new Thread(new BotHandler(this,clientSocket)).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    public List<ClusterResource> getResources() {
        return resources;
    }

    public int getCurrentMin() {
        return currentMin;
    }

    public int getCurrentMax() {
        return currentMax;
    }

    public List<BotHandler> getPlayers() {
        var playerList = new ArrayList<>(players.values());
        return playerList;
    }
}
