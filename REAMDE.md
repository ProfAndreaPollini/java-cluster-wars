# 🛡️ Cluster War - API & Protocol Documentation

Benvenuti nell'arena di **Cluster War**, un gioco di strategia e programmazione in rete. Il vostro obiettivo è sviluppare un Bot capace di navigare in una griglia 40x40, raccogliere risorse e sopravvivere agli attacchi dei bot avversari.

---

## ⏱️ Ciclo della Partita (Game Loop)
Il server gestisce il tempo in modo autonomo. La partita è un ciclo infinito di "Manche":

1. **COUNTDOWN (10s):** Fase di attesa. È il momento di connettere i bot. Nessuna azione è permessa.
2. **RUNNING (180s):** Il match è attivo. I bot si muovono, attaccano e raccolgono punti.
3. **FINISHED (20s):** Il tempo è scaduto. Viene mostrata la classifica finale e i dati vengono salvati sul database JSON. Al termine, il server torna allo stato di Countdown.

---

## 🔌 Connessione e Registrazione
Il server è in ascolto all'indirizzo IP del professore sulla porta `5000`.

### Handshake iniziale
Appena connessi, il bot deve inviare una singola riga di testo con il proprio nome:
- Prefisso **`HCK_`**: Bot della fazione *Hacker* (Colore Rosso).
- Prefisso **`SEC_`**: Bot della fazione *Security* (Colore Blu).

> **Nota sulla persistenza:** Il server identifica il bot tramite il suo **indirizzo IP**. Se ti disconnetti, i tuoi punti rimangono salvati e verranno ricaricati automaticamente al tuo rientro (anche tra manche diverse).

---

## 📥 Protocollo di Ricezione (Server -> Bot)
Ad ogni "tick" del server (circa ogni secondo), riceverai una stringa di stato:

`STATUS|x,y|energy|viewDist|E:nemici|R:risorse`

### Esempio di stringa:
`STATUS|12,25|80|10|E:SEC_Guard,13,25;HCK_Ghost,10,20;|R:SERVER,12,26;RAMSTICK,15,30;`

| Campo | Descrizione |
| :--- | :--- |
| **x,y** | La tua posizione attuale (0-39). |
| **energy** | La tua energia attuale (0-100). |
| **viewDist** | Il tuo raggio visivo attuale (celle di distanza). |
| **E:nemici** | Bot nemici visibili nel tuo raggio (`Nome,x,y;`). |
| **R:risorse** | Risorse visibili nel tuo raggio (`Tipo,x,y;`). |

---

## 📤 Protocollo di Invio (Bot -> Server)
Dopo aver ricevuto lo `STATUS`, il bot deve rispondere con **un solo comando** per turno:

### 1. Movimento
Sposta il bot di una cella nella direzione indicata.
- `MOVE_UP`
- `MOVE_DOWN`
- `MOVE_LEFT`
- `MOVE_RIGHT`

### 2. Attacco
Colpisce una cella specifica. Se un nemico si trova su quelle coordinate, subisce **20 danni**.
- `ATTACK:x,y` (Esempio: `ATTACK:13,25`)

### 3. Attesa e Rigenerazione
Il bot rimane fermo per analizzare l'ambiente o recuperare sistemi.
- `WAIT`
- **Effetto:** Ogni comando `WAIT` incrementa la tua energia di **+1** (fino a un massimo di 100).

---

## 🔋 Meccaniche di Sopravvivenza

### Risorse (Power-ups)
Passare sopra una cella con una risorsa la attiva immediatamente:
- **`SERVER` (ServerNode):** Ripristina immediatamente **+30 Energia**.
- **`RAMSTICK` (RAMStick):** Aumenta il tuo **Punteggio (Score)** in classifica.

### Energia e Morte
- Se l'energia scende a **0**, il bot viene considerato distrutto e il server **chiuderà la connessione**.
- Per rientrare, dovrai riavviare il tuo script.

---

## 🔒 Sicurezza e Limitazioni
Il server implementa misure di protezione per garantire il fair play:
- **IP Limit:** Massimo 1 connessione per postazione (indirizzo IP).
- **Flood Protection:** Massimo 10 messaggi al secondo. Se superato, il bot viene espulso per spam.
- **Payload Limit:** I comandi più lunghi di 50 caratteri vengono ignorati.

---

**Buona fortuna, Hacker. Che il miglior algoritmo vinca!**