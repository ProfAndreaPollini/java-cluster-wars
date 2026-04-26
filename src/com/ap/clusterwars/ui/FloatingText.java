package com.ap.clusterwars.ui;

import com.raylib.Raylib;
import static com.raylib.Raylib.*;

public class FloatingText {
    String text;
    float x, y;
    float alpha = 1.0f;
    float speedY = 0.8f; // Velocità di risalita
    com.raylib.Raylib.Color color;

    public FloatingText(String text, float x, float y, com.raylib.Raylib.Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void update() {
        y -= speedY;    // Sale verso l'alto
        alpha -= 0.015f; // Svanisce lentamente (circa 60 frame)
    }

    public void draw() {
        // Applichiamo l'alpha al colore originale
        com.raylib.Raylib.Color c = ColorAlpha(color, alpha);
        DrawText(text, (int)x, (int)y, 15, c);
    }

    public boolean isDead() {
        return alpha <= 0;
    }
}