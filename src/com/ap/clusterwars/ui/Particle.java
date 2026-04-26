package com.ap.clusterwars.ui;

import static com.raylib.Raylib.*;

public class Particle {
    public float x, y;
    public float vx, vy;
    float life = 1.0f; // Da 1.0 a 0.0
    com.raylib.Raylib.Color color;

    public Particle(float x, float y, com.raylib.Raylib.Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        // Direzione casuale
        float angle = (float) (Math.random() * Math.PI * 2);
        float speed = (float) (Math.random() * 3 + 1);
        this.vx = (float) Math.cos(angle) * speed;
        this.vy = (float) Math.sin(angle) * speed;
    }

    public void update() {
        x += vx;
        y += vy;
        life -= 0.02f; // Svanisce in circa 50 frame
    }

    public void draw() {
        // Applichiamo la trasparenza in base alla vita residua
        com.raylib.Raylib.Color c = ColorAlpha(color, life);
        DrawRectangle((int)x, (int)y, 3, 3, c);
    }
}
