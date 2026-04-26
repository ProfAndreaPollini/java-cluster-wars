import java.io.*;
import java.net.*;

private static final String SERVER_IP = "127.0.0.1";
private static final int PORT = 5000;
private static final String BOT_NAME = "HCK_ScannerBot";

void main() {
    IO.println("Avvio Bot di Test (Raster Scan)...");

    try (Socket socket = new Socket(SERVER_IP, PORT);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        out.println(BOT_NAME);

        String serverStatus;
        boolean movingRight = true; // Direzione attuale sulla riga

        while ((serverStatus = in.readLine()) != null) {
            if (serverStatus.startsWith("STATUS")) {
                // Parsing: STATUS|x,y|energy|E:nemici|R:risorse
                String[] parts = serverStatus.split("\\|");
                String[] coords = parts[1].split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);

                String nextMove = "WAIT";

                // LOGICA DI SCANSIONE PER RIGHE
                // Arrivato alla fine della riga a destra
                if (movingRight && x >= 39) {
                    nextMove = "MOVE_DOWN";
                    movingRight = false; // Inverti rotta per la prossima riga
                }
                // Arrivato alla fine della riga a sinistra
                else if (!movingRight && x <= 0) {
                    nextMove = "MOVE_DOWN";
                    movingRight = true; // Inverti rotta per la prossima riga
                }
                // In mezzo alla riga: continua a muoverti lateralmente
                else {
                    nextMove = movingRight ? "MOVE_RIGHT" : "MOVE_LEFT";
                }

                // Gestione del bordo inferiore: se arriviamo in fondo (39,39),
                // potremmo resettare o fermarci. Qui proviamo a risalire.
                if (y >= 39 && nextMove.equals("MOVE_DOWN")) {
                    IO.println("Scansione completata, reset posizione o attesa.");
                    nextMove = "WAIT";
                }

                out.println(nextMove);
            }

            if (serverStatus.startsWith("GAMEOVER")) break;
        }
    } catch (IOException e) {
        System.err.println("Errore: " + e.getMessage());
    }
}