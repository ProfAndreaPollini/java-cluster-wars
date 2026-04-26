package com.ap.clusterwars.resources;

import com.ap.clusterwars.BotHandler;
import com.raylib.Raylib;

import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;
public class ServerNode extends ClusterResource {
    public ServerNode(int x, int y) { super(x, y); }

    @Override
    public void applyEffect(BotHandler bot) {
       bot.setEnergy(Math.min(100, bot.getEnergy() + 50));
        IO.println(bot.getName() + " ha ripristinato i sistemi al server.");
    }

    @Override
    public void draw(int  drawX, int drawY,int cell_size) {
        DrawRectangle(drawX, drawY,cell_size-2, cell_size-2, GREEN); // Rettangolo solido
    }

    @Override
    public String getType() {
        return "SERVER";
    }
}