
import com.ap.clusterwars.BotHandler;
import com.ap.clusterwars.ClusterWarServer;
import com.ap.clusterwars.resources.ClusterResource;
import com.ap.clusterwars.ui.FloatingText;
import com.ap.clusterwars.ui.Particle;
import com.ap.clusterwars.ui.ParticleAnimation;

import static com.ap.clusterwars.ClusterWarServer.CELL_SIZE;
import static com.ap.clusterwars.ClusterWarServer.GRID_SIZE;
import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;



List<ParticleAnimation> particleAnimations = new CopyOnWriteArrayList<>();
List<FloatingText> floatingTexts = new CopyOnWriteArrayList<>();

void main() {
    final int SIDEBAR_WIDTH = 250;
    final int ARENA_OFFSET_X = SIDEBAR_WIDTH; // Il campo inizia dopo la sidebar
    final int SCREEN_WIDTH = 800 + SIDEBAR_WIDTH;
    final int SCREEN_HEIGHT = 800; // Alziamo a 800 per far stare la griglia 40x20

    double lastUpdateTime = GetTime();
    double tickRate = 1.0; // 1 secondo

    var gameServer = new ClusterWarServer();



    InitWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Cluster War 2D - Cyber Arena");
    SetTargetFPS(60);



    gameServer.start();

    while (!WindowShouldClose()) {
        double currentTime = GetTime();
        if (currentTime - lastUpdateTime >= tickRate) {
            gameServer.updateGameState(); // Qui risolvi movimenti e attacchi
            lastUpdateTime = currentTime;
        }

        // --- AGGIORNAMENTO GRAFICO (Ogni frame - 60 FPS) ---
        for (BotHandler bot : gameServer.getPlayers()) {
            bot.updateVisuals(CELL_SIZE); // Calcola lo scivolamento
        }

        for (var anim: particleAnimations) {
            anim.update();
        }

        floatingTexts.forEach(FloatingText::update);
        floatingTexts.removeIf(FloatingText::isDead);
// --- PARTE B: RENDERING (Eseguito a 60 FPS) ---
//        BeginDrawing();
//        ClearBackground(BLACK);
//
//        // Disegna la griglia di sfondo (estetica hacker)
//        for (int i = 0; i < GRID_SIZE; i++) {
//            DrawLine(i * CELL_SIZE, 0, i * CELL_SIZE, 800, DARKGRAY);
//            DrawLine(0, i * CELL_SIZE, 800, i * CELL_SIZE, DARKGRAY);
//        }
//
//        // Disegna le risorse (Polimorfismo: chiama res.draw())
//        for (ClusterResource res : gameServer.getResources()) {
//            if (res.isActive()) res.draw();
//        }
//
//        // Disegna i Bot degli studenti
//        for (BotHandler bot : gameServer.getPlayers()) {
//            Color color = bot.isHacker() ? RED : SKYBLUE;
//
//
//            // Corpo del bot
//            float botSize = CELL_SIZE * 0.8f; // Il bot occupa l'80% della cella
//            float offset = (CELL_SIZE - botSize) / 2; // Spazio per centrare
////            DrawRectangle(bot.getVisualX() , bot.getVisualY() , CELL_SIZE, CELL_SIZE, color);
////            Vector2 pos = new Vector2();
////            pos.x(bot.getVisualX()*CELL_SIZE + offset);
////            pos.y(bot.getVisualY()*CELL_SIZE + offset);
//            var botX = (int) (bot.getVisualX()*CELL_SIZE + offset);
//            var botY = (int) (bot.getVisualY()*CELL_SIZE + offset);
//
//
//
//            DrawRectangle(
//                    botX,
//                    botY,
//                    (int) botSize,
//                    (int) botSize,
//                    color
//            );
//            var sb = new StringBuilder();
//            sb.append("BOT [").append(bot.getName()).append("]").append(bot.getVisualX()).append(" ").append(bot.getVisualY())
//                    .append("[ ").append(botX).append(", ").append(botY).append("]\n");
//            IO.println(sb.toString());
//
//            // Nome sopra il bot
//            DrawText(bot.getName(), botX , botY  - 12, 10, WHITE);
//
//            // Piccola barra dell'energia sotto il bot
//            float energyRatio = bot.getEnergy() / 100.0f;
//            DrawRectangle(botX , botY  + CELL_SIZE - 2,
//                    (int)(CELL_SIZE * energyRatio), 2, GREEN);
//        }
//
//        // Overlay con classifica o info (Opzionale)
//        DrawFPS(10, 10);
//        DrawText("SISTEMA ATTIVO - ROUND TICK: " + (int)currentTime, 10, 30, 20, LIME);
//
//        EndDrawing();
        BeginDrawing();
        ClearBackground(BLACK);
        // --- 1. DISEGNO SIDEBAR (Leaderboard) ---
        DrawRectangle(0, 0, SIDEBAR_WIDTH, SCREEN_HEIGHT, ColorAlpha(DARKGRAY, 0.3f));
        DrawLine(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH, SCREEN_HEIGHT, LIME);

        DrawText("RANKING", 20, 20, 25, GOLD);
        DrawText("Fazione: Hacker/Security", 20, 50, 10, GRAY);

        // Ordiniamo i player per score (o energia) per la classifica
        var sortedPlayers = gameServer.getPlayers();
//        sortedPlayers.sort((a, b) -> Integer.compare(b.getEnergy(), a.getEnergy())); // Sostituisci con getScore() se disponibile
        sortedPlayers.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        int yOffset = 90;
        for (int i = 0; i < Math.min(sortedPlayers.size(), 15); i++) {
            BotHandler bot = sortedPlayers.get(i);
            Color teamColor = bot.isHacker() ? RED : SKYBLUE;

//            String entry = String.format("%d. %-12s %3d%% | %4d", i + 1, bot.getName(), bot.getEnergy(),bot.getScore());
            String entry = String.format("%d. %s - Punti: %d [E: %d]", i+1, bot.getName(), bot.getScore(), bot.getEnergy());
            DrawText(entry, 20, yOffset, 18, teamColor);
            yOffset += 30;
        }

        // --- 2. DISEGNO ARENA (Traslata di ARENA_OFFSET_X) ---

        // Griglia traslata
        for (int i = 0; i <= GRID_SIZE; i++) {
            DrawLine(ARENA_OFFSET_X + i * CELL_SIZE, 0, ARENA_OFFSET_X + i * CELL_SIZE, SCREEN_HEIGHT, DARKGRAY);
            DrawLine(ARENA_OFFSET_X, i * CELL_SIZE, ARENA_OFFSET_X + 800, i * CELL_SIZE, DARKGRAY);
        }

        // Risorse traslate
        for (ClusterResource res : gameServer.getResources()) {
            if (res.isActive()) {
                int drawX = ARENA_OFFSET_X + (res.getX() * CELL_SIZE); // 20 è la CELL_SIZE
                int drawY =  (res.getY() * CELL_SIZE);
                res.draw(drawX, drawY, CELL_SIZE);
            }
        }

        // Bot traslati
        for (BotHandler bot : gameServer.getPlayers()) {

            Color color = bot.isHacker() ? RED : SKYBLUE;
            float botSize = CELL_SIZE * 0.8f;
            float offset = (CELL_SIZE - botSize) / 2;

            // Applichiamo ARENA_OFFSET_X alle coordinate grafiche
            int botX = (int) (ARENA_OFFSET_X + bot.getVisualX() * CELL_SIZE + offset);
            int botY = (int) (bot.getVisualY() * CELL_SIZE + offset);


            if (bot.isDead() && bot.checkWasHit()) {
                var anim = new ParticleAnimation();
                // ESPLOSIONE MASSICCIA: più particelle e più grandi
                for(int i=0; i<50; i++) {
                    anim.add(new Particle(botX, botY, ORANGE));
                }
                // Magari un testo "REBOOTING..." sopra la posizione
                floatingTexts.add(new FloatingText("SYSTEM CRASH!", botX, botY, RED));
            }

            if(bot.isDead()) continue;



            DrawRectangle(botX, botY, (int) botSize, (int) botSize, color);
            DrawText(bot.getName(), botX, botY - 12, 10, WHITE);

            float energyRatio = bot.getEnergy() / 100.0f;
            DrawRectangle(botX, botY + (int)botSize, (int)(botSize * energyRatio), 2, GREEN);

            if (bot.checkWasHit()) {
                particleAnimations.add(ParticleAnimation.createExplosion(botX, botY, RED));
            }

            if (bot.checkWasCollected()) {
                particleAnimations.add(ParticleAnimation.createPowerUpEffect(botX, botY));
            }
        }

        for (var anim: particleAnimations) {
            anim.draw();
        }

        for (FloatingText ft : floatingTexts) {
            ft.draw();
        }
        // Overlay info
        DrawFPS(SCREEN_WIDTH - 80, 10);
        EndDrawing();
    }
    CloseWindow();

}


