package com.ap.clusterwars.client;

import com.ap.clusterwars.ClusterWarServer;

import java.io.*;
import java.net.*;
import java.util.*;

import static com.raylib.Raylib.*;
import static com.raylib.Colors.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BotController {
    private String name;
    private int x, y, energy,viewDistance;
    private List<Entity> visibleEnemies = new CopyOnWriteArrayList<>();
    private List<Resource> visibleResources = new CopyOnWriteArrayList<>();
    private Thread networkThread;

    public BotController(String name) { this.name = name; }

    // METODO CHE GLI STUDENTI DEVONO IMPLEMENTARE
    public abstract String think();

//    public void start(String ip, int port) {
//        try (Socket s = new Socket(ip, port);
//             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
//             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
//
//            out.println(name); // Login
//            String line;
//            while ((line = in.readLine()) != null) {
//                if (line.startsWith("STATUS")) {
//                    parse(line);
//                    String move = makeDecision(); // Chiamata alla logica dello studente
//                    out.println(move);
//                }
//            }
//        } catch (IOException e) { e.printStackTrace(); }
//    }

    private void parse(String status) {
//        String[] parts = status.split("\\|");
//        // Coordinate e Energia
//        String[] coords = parts[1].split(",");
//        this.x = Integer.parseInt(coords[0]);
//        this.y = Integer.parseInt(coords[1]);
//        this.energy = Integer.parseInt(parts[2]);
//
//
//        // Parsing Nemici (E:nome,x,y;...)
//        visibleEnemies.clear();
//        String[] enemies = parts[3].substring(2).split(";");
//        for (String e : enemies) {
//            if (!e.isEmpty()) {
//                String[] d = e.split(",");
//                visibleEnemies.add(new Entity(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2])));
//            }
//        }
//
//        // Parsing Risorse (R:tipo,x,y;...)
//        visibleResources.clear();
//        String[] res = parts[4].substring(2).split(";");
//        for (String r : res) {
//            if (!r.isEmpty()) {
//                String[] d = r.split(",");
//                visibleResources.add(new Resource(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2])));
//            }
//        }
        String[] parts = status.split("\\|");
        if (parts.length < 6) return; // Protezione contro pacchetti incompleti

        // 1. Posizione e Energia (parts[1] e parts[2])
        String[] coords = parts[1].split(",");
        this.x = Integer.parseInt(coords[0]);
        this.y = Integer.parseInt(coords[1]);
        this.viewDistance = Integer.parseInt(parts[3]);
        this.energy = Integer.parseInt(parts[2]);

        // 2. Nemici (parts[3]) -> Rimuovi "E:" prima di splittare
        enemies.clear();
        String enemiesData = parts[4].substring(2); // Salta i primi 2 caratteri ("E:")
        if (!enemiesData.isEmpty()) {
            for (String e : enemiesData.split(";")) {
                String[] d = e.split(",");
                if (d.length == 3) {
                    enemies.add(new Entity(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2]),viewDistance));
                }
            }
        }

        // 3. Risorse (parts[4]) -> Rimuovi "R:" prima di splittare
        resources.clear();
        String resourcesData = parts[5].substring(2); // Salta i primi 2 caratteri ("R:")
        if (!resourcesData.isEmpty()) {
            for (String r : resourcesData.split(";")) {
                String[] d = r.split(",");
                if (d.length == 3) {
                    // d[0] = tipo, d[1] = x, d[2] = y
                    resources.add(new Resource(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2])));
                }
            }
        }
    }


    protected List<Entity> enemies = new CopyOnWriteArrayList<>();
    protected List<Resource> resources = new CopyOnWriteArrayList<>();

    // Logica per il movimento fluido locale
    protected float visualX, visualY;



    // Metodo decisionale (IA dello studente)
//    public abstract String makeDecision();

    public void start(String ip, int port) {
        // 1. Avvia il Thread di Rete
        networkThread = new Thread(() -> networkLoop(ip, port));
        networkThread.start();

        // 2. Avvia la Visualizzazione Locale (Raylib)
        renderLoop();
    }

    private void networkLoop(String ip, int port) {
        try (Socket s = new Socket(ip, port);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            out.println(name);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("STATUS")) {
                    parse(line);
                    out.println(think());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderLoop() {
        InitWindow(800, 850, "Client View: " + name);
        SetTargetFPS(60);

        while (!WindowShouldClose()) {
            // Interpolazione locale per il movimento fluido
            visualX += (x - visualX) * 0.1f;
            visualY += (y - visualY) * 0.1f;

            BeginDrawing();
            ClearBackground(new Color().r((byte)20).g((byte)20).b((byte)25).a((byte)255));

            // Disegna Mini-Mappa (Solo quello che il bot conosce)
            drawMiniMap();

//            // Info Stato
//            DrawText("SYSTEM STATUS: ONLINE", 10, 310, 15, LIME);
//            DrawText("POS: " + x + "," + y, 10, 330, 15, WHITE);
//            DrawText("ENERGY: " + energy + "%", 10, 350, 15, energy > 20 ? SKYBLUE : RED);
//
//            // Barra energia
//            DrawRectangle(10, 370, 150, 10, DARKGRAY);
//            DrawRectangle(10, 370, (int)(1.5 * energy), 10, LIME);

            int uiY = 650; // Spostiamo i testi sotto la minimappa ingrandita
            DrawText("SYSTEM STATUS: ONLINE", 50, uiY, 25, LIME);
            DrawText("POS: " + x + "," + y, 50, uiY + 40, 20, WHITE);
            DrawText("ENERGY: " + energy + "%", 50, uiY+60, 20, energy > 20 ? SKYBLUE : RED);

            // Barra energia più grande
            DrawRectangle(50, uiY + 80, 700, 20, DARKGRAY);
            DrawRectangle(50, uiY + 80, (int)(7.0 * energy), 20, energy > 20 ? SKYBLUE : RED);

            EndDrawing();
        }
        CloseWindow();
        System.exit(0);
    }

    private void drawMiniMap() {
        int viewSize = 500; // Ingrandita da 300 a 700
        int offsetX = 50;
        int offsetY = 20;

        // Scala: 700 pixel / 40 celle = 17.5 pixel per cella
        float scale = viewSize / (float)ClusterWarServer.GRID_SIZE;

        // 1. Sfondo dell'arena locale (grigio molto scuro per profondità)
        DrawRectangle(offsetX, offsetY, viewSize, viewSize, new Color().r((byte)25).g((byte)25).b((byte)30).a((byte)255));
        DrawRectangleLines(offsetX, offsetY, viewSize, viewSize, DARKGRAY);

        // 2. Raggio visivo
        float radiusPx = viewDistance * scale;
        DrawCircleLines(
                (int)(offsetX + visualX * scale),
                (int)(offsetY + visualY * scale),
                radiusPx, ColorAlpha(LIME, 0.4f)
        );

        // 3. Risorse (leggermente più grandi visto che c'è spazio)
        for (Resource r : resources) {
            switch(r.type()) {
                case "SERVER" -> DrawCircle((int) (offsetX + r.x() * scale), (int) (offsetY + r.y() * scale), 5, GOLD);
                case "RAMSTICK" -> DrawCircle((int) (offsetX + r.x() * scale), (int) (offsetY + r.y() * scale), 5, RED);
            }

        }

        // 4. Nemici
        for (Entity e : enemies) {
            DrawRectangle((int)(offsetX + e.x() * scale - 4), (int)(offsetY + e.y() * scale - 4), 10, 10, RED);
        }

        // 5. Se Stesso (Bot principale)
        // Usiamo DrawPoly per fare un triangolo o una forma più "da player"
        DrawRectangle((int)(offsetX + visualX * scale - 6), (int)(offsetY + visualY * scale - 6), 12, 12, GREEN);
        DrawRectangleLines((int)(offsetX + visualX * scale - 6), (int)(offsetY + visualY * scale - 6), 12, 12, WHITE);
    }

//    private void drawMiniMap() {
//        int viewSize = 600; // Dimensione del riquadro mini-mappa
//        int offsetX = 50;   // Margine sinistro
//        int offsetY = 5;    // Margine superiore
//
//        // Calcoliamo la scala corretta: se la griglia è 40x40,
//        // ogni cella occupa 300/40 = 7.5 pixel.
//        // Usiamo float per precisione durante il calcolo.
//        float scale = viewSize / (float) ClusterWarServer.GRID_SIZE;
//
//        // 1. Disegna il contenitore
//        DrawRectangleLines(offsetX, offsetY, viewSize, viewSize, DARKGRAY);
//
//        // 2. Disegna il raggio visivo (Cerchio di luce)
//        // Nota: viewDistance è in celle, quindi va moltiplicata per scale
//        float radiusPx = viewDistance * scale;
//        DrawCircleLines(
//                (int)(offsetX + visualX * scale),
//                (int)(offsetY + visualY * scale),
//                radiusPx, ColorAlpha(LIME, 0.3f)
//        );
//
//        // 3. Disegna Risorse (usando la stessa scala)
//        for (Resource r : resources) {
//            DrawCircle(
//                    (int)(offsetX + r.x() * scale),
//                    (int)(offsetY + r.y() * scale),
//                    3, GOLD
//            );
//        }
//
//        // 4. Disegna Nemici (usando la stessa scala)
//        for (Entity e : enemies) {
//            DrawRectangle(
//                    (int)(offsetX + e.x() * scale),
//                    (int)(offsetY + e.y() * scale),
//                    5, 5, RED
//            );
//        }
//
//        // 5. Disegna Se Stesso (Il Bot)
//        DrawRectangle(
//                (int)(offsetX + visualX * scale - 4), // -4 per centrare il quadratino da 8
//                (int)(offsetY + visualY * scale - 4),
//                8, 8, GREEN
//        );
//    }

//    private void drawMiniMap() {
//        // Disegna un'area di scansione (es. 15x15 celle attorno al bot)
//        int viewSize = 300;
//        int viewPx = viewDistance * 10; // Scala la distanza in pixel
//        DrawRectangleLines(50, 5, viewSize, viewSize, DARKGRAY);
//
//        DrawCircleLines((int)visualX * 10 + 50, (int)visualY * 10 + 5, viewPx, ColorAlpha(LIME, 0.3f));
//
//        // Disegna Risorse che il bot "vede"
//        for (Resource r : resources) {
//            // Coordinate relative per centrare il bot nella sua vista
//            DrawCircle(50 + r.x() * 7, 5 + r.y() * 7, 3, GOLD);
//        }
//
//        // Disegna Nemici
//        for (Entity e : enemies) {
//            DrawRectangle(50 + e.x() * 7, 5 + e.y() * 7, 5, 5, RED);
//        }
//
//        // Disegna Se Stesso
////        DrawRectangle((int)(50 + visualX * 7), (int)(5 + visualY * 7), 6, 6, GREEN);
//
//        // 3. Disegna il Bot al centro della sua vista
//        DrawRectangle((int)(50 + visualX * 10), (int)(5 + visualY * 10), 8, 8, GREEN);
//    }

    // Getter per gli studenti
    public int getX() { return x; }
    public int getY() { return y; }
    public int getEnergy() { return energy; }
    public List<Entity> getEnemies() { return visibleEnemies; }
    public List<Resource> getResources() { return visibleResources; }
}