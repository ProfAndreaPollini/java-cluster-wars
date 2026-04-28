import com.ap.clusterwars.client.BotController;
import com.ap.clusterwars.client.Resource;

private static final String SERVER_IP = "127.0.0.1";
private static final int PORT = 5000;
private static final String BOT_NAME = "HCK_TestBot_3"; // Prefisso HCK per la fazione Hacker
//
//void main() {
//    IO.println("Avvio Bot di Test...");
//
//    try (Socket socket = new Socket(SERVER_IP, PORT);
//         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//
//        IO.println("Connesso al server!");
//
//        // 1. Fase di Registrazione: inviamo il nome del bot
//        out.println(BOT_NAME);
//
//        String serverStatus;
//        Random rand = new Random();
//        String[] moves = {"MOVE_UP", "MOVE_DOWN", "MOVE_LEFT", "MOVE_RIGHT", "WAIT"};
//
//        // 2. Loop di gioco: il bot resta in ascolto dei messaggi del server
//        while ((serverStatus = in.readLine()) != null) {
//            IO.println("Ricevuto dal Server: " + serverStatus);
//
//            // Esempio di parsing minimale (giusto per vedere i propri dati)
//            // STATUS|x,y|energy|E:nemici|R:risorse
//            if (serverStatus.startsWith("STATUS")) {
//                String[] parts = serverStatus.split("\\|");
//                String coords = parts[1];
//                String energy = parts[2];
//
//                IO.println("[" + BOT_NAME + "] Posizione: " + coords + " | Energia: " + energy);
//
//                // 3. Logica decisionale (Randomica per il test)
//                String nextMove = moves[rand.nextInt(moves.length)];
//
//                // Invio del comando al server
//                IO.println("Invio comando: " + nextMove);
//                out.println(nextMove);
//            }
//
//            if (serverStatus.startsWith("GAMEOVER")) {
//                IO.println("Partita terminata dal server.");
//                break;
//            }
//        }
//
//    } catch (IOException e) {
//        System.err.println("Errore di connessione: " + e.getMessage());
//    }
//}

public static class MyBot extends BotController {

    public MyBot(String name) {
        super(name);
    }


    @Override
    public String think() {
        // Logica di test: cerca la risorsa più vicina, altrimenti muoviti a caso
        if (!getResources().isEmpty()) {
            Resource target = getResources().get(0); // Prende la prima risorsa vista

            if (target.x() > getX()) return "MOVE_RIGHT";
            if (target.x() < getX()) return "MOVE_LEFT";
            if (target.y() > getY()) return "MOVE_DOWN";
            if (target.y() < getY()) return "MOVE_UP";
        }

        // Se non vede risorse, si muove a caso
        String[] moves = {"DASH:UP", "DASH:DOWN", "DASH:LEFT", "DASH:RIGHT"};
        return moves[(int) (Math.random() * moves.length)];
    }

    public static void main(String[] args) {
        // Avvia il bot e la sua console grafica locale

    }
}

void main() {
    new MyBot(BOT_NAME).start(SERVER_IP, PORT);
}