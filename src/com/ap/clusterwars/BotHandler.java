package com.ap.clusterwars;

import com.ap.clusterwars.resources.ClusterResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Random;

public class BotHandler implements Runnable {
    private final Socket socket;
    private final String clientIP;
    private String name;
    private int x, y, energy = 100;
    private boolean isHacker;
    private String lastAction = "WAIT";
    private PrintWriter out;
    private ClusterWarServer gameServer;
    private float visualX, visualY;
    private int score;
    private boolean justHit = false;
    private boolean justCollected = false;
    private boolean isDead = false;
    private int respawnTimer = 0;
    private int viewDistance = 10;

    private long lastMessageTime = 0;
    private int messageCount = 0;
    private final int MAX_MESSAGES_PER_SECOND = 10; // Un bot onesto non invia più di 1-2 messaggi


    public boolean isDead() {
        return isDead;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    private float lerpSpeed = 0.1f; // Velocità di scorrimento (0.1 = 10% del percorso a frame)

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        if(energy <= 0) {
            setDead();
            registerHit();
        }
    }

    public boolean isHacker() {
        return isHacker;
    }

    public void setHacker(boolean hacker) {
        isHacker = hacker;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public BotHandler(ClusterWarServer server, Socket s) {
        this.socket = s;
        Random r = new Random();
        this.x = r.nextInt(0, ClusterWarServer.GRID_SIZE);
        this.y = r.nextInt(0,ClusterWarServer.GRID_SIZE);
        gameServer = server;
        this.visualX = x ;
        this.visualY = y ;
        this.clientIP = socket.getInetAddress().getHostAddress();
    }

    private void disconnect() {
        try {
            // Chiudere la socket interrompe immediatamente l'in.readLine() nel metodo run()
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Errore durante la disconnessione di " + name);
        }
    }

    public String getClientIP() { return clientIP; }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            // Primo messaggio: Nome del bot (es: HCK_Killer o SEC_Guard)
            this.name = in.readLine();
            this.isHacker = name.startsWith("HCK");
            gameServer.addPlayer(name, this);

            String line;
            while ((line = in.readLine()) != null) {
                // --- ANTI-FLOOD LOGIC ---
                long currentTime = System.currentTimeMillis();

                // Se è passato meno di un secondo dall'ultimo controllo, incrementa
                if (currentTime - lastMessageTime < 1000) {
                    messageCount++;
                } else {
                    messageCount = 0; // Reset ogni secondo
                    lastMessageTime = currentTime;
                }

                if (messageCount > MAX_MESSAGES_PER_SECOND) {
                    System.err.println("[FLOOD] Kick " + name + " per spam comandi.");

                    break; // Esci dal loop e chiudi la socket nel finally

                }
                // -----------------------

                this.lastAction = line.toUpperCase(); // Salva l'intenzione per il prossimo tick
            }
        } catch (IOException e) {
            System.out.println(name + " disconnesso.");
        } finally {
//            gameServer.removePlayer(name); // scommenta per avere respawn alla connessione al 100%
            this.lastAction = "IDLE";
            disconnect();
        }
    }

    public float getVisualX() {
        return visualX;
    }

    public void setVisualX(float visualX) {
        this.visualX = visualX;
    }

    public float getVisualY() {
        return visualY;
    }

    public void setVisualY(float visualY) {
        this.visualY = visualY;
    }

    public void executeMove(int limit) {
        if (lastAction.equals("MOVE_UP") && y > 0) y--;
        else if (lastAction.equals("MOVE_DOWN") && y < limit-1) y++;
        else if (lastAction.equals("MOVE_LEFT") && x > 0) x--;
        else if (lastAction.equals("MOVE_RIGHT") && x < limit-1) x++;
        energy = Math.max(0, energy - 1); // Muoversi stanca
        if (energy <= 0) {
            setDead();
        }
    }

    public void sendStatus(Collection<BotHandler> allBots, String resString) {
        if (isDead) return; // non inviare nulla

        // Costruisce la stringa STATUS|x,y|energy|E:nemici|R:risorse
        StringBuilder sb = new StringBuilder("STATUS|").append(x).append(",").append(y)
                .append("|").append(energy).append("|").append(viewDistance).append("|E:");
        for (BotHandler other : allBots) {
            if (other != this && dist(other) < getViewDistance()) { // Raggio visivo
                sb.append(other.name).append(",").append(other.x).append(",").append(other.y).append(";");
            }
        }

        sb.append("|").append(resString);
        out.println(sb.toString());
    }

    public void updateVisuals(int cell_size) {
        float targetX = x ;
        float targetY = y ;

        // Linear Interpolation (Lerp): avvicina la posizione visiva al target
        visualX += ((targetX - visualX) * lerpSpeed);
        visualY +=  ((targetY - visualY) * lerpSpeed);
    }


    // registra collisioni
    public void registerHit() {
        this.justHit = true;
    }
    public boolean checkWasHit() {
        if (justHit) {
            justHit = false; // Reset immediato dopo il controllo
            return true;
        }
        return false;
    }

    // Metodo per il server quando raccoglie una risorsa
    public void registerCollection() {
        this.justCollected = true;
    }

    public boolean checkWasCollected() {
        if (justCollected) {
            justCollected = false;
            return true;
        }
        return false;
    }

    public void setDead(){
        setDead(true);
    }

    public void setDead(boolean dead) {
        isDead = dead;
        this.respawnTimer = 15;
        energy = 100;
        //disconnect();
    }

    public void takeDamage(int amount) {
        if (isDead) return; // Non puoi colpire chi è già morto

        this.energy -= amount;
        if (this.energy <= 0) {
            setDead();
//            this.energy = 0;
//            this.isDead = true;
//            this.respawnTimer = 5; // Resta "esploso" per 5 secondi
            this.registerHit();    // Trigger per l'esplosione nel visualizzatore
        } else {
            this.registerHit();    // Colpo normale
        }
    }

    public void handleRespawn() {
        if (isDead) {
            respawnTimer--;
            if (respawnTimer <= 0) {
                isDead = false;
                energy = 100; // Torna in vita pieno
                // Opzionale: teletrasporta in un punto casuale
                this.x = new Random().nextInt(40);
                this.y = new Random().nextInt(40);
            }
        }
    }

    private double dist(BotHandler o) { return Math.sqrt(Math.pow(x-o.x, 2) + Math.pow(y-o.y, 2)); }

    public double distance(ClusterResource r) {
        return  Math.sqrt(Math.pow(x-r.getX(), 2) + Math.pow(y-r.getY(), 2));
    }
}