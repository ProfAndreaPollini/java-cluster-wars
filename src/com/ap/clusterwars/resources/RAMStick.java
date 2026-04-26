package com.ap.clusterwars.resources;

import com.ap.clusterwars.BotHandler;

import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;
public class RAMStick extends ClusterResource {
    public RAMStick(int x, int y) { super(x, y); }

    @Override
    public void applyEffect(BotHandler bot) {
       bot.setScore(bot.getScore() + 100); // La RAM aumenta il punteggio, non l'energia
        IO.println(bot.getName() + " ha scaricato dati preziosi dalla RAM.");
    }

    @Override
    public void draw(int  drawX, int drawY,int cell_size) {
        DrawCircle((int) (drawX + cell_size/2.0f), (int) (drawY+ cell_size/2.0f), cell_size/2.0f -2, GOLD); // Un cerchio dorato
    }

    @Override
    public String getType() {
        return "RAMSTICK";
    }
}