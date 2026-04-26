package com.ap.clusterwars.resources;

import com.ap.clusterwars.BotHandler;

public abstract class ClusterResource {
    private int x, y;
    private boolean active = true;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ClusterResource(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Metodo astratto: ogni risorsa decide cosa dare al bot
    public abstract void applyEffect(BotHandler bot);



    public abstract void draw(int  drawX, int drawY,int cell_size);

    public abstract String getType();
}