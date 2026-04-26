package com.ap.clusterwars.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.raylib.Colors.GOLD;

public  class ParticleAnimation {
    List<Particle> particles = new CopyOnWriteArrayList<>();


    public void update() {
        particles.forEach(Particle::update);
        particles.removeIf(p -> p.life <= 0);
    }

    public void add(Particle p) {
        particles.add(p);
    }


    public void draw(){
        for (Particle p : particles) {
            p.draw();
        }
    }

    // Metodo per esplosione (Bot colpito)
    public static  ParticleAnimation createExplosion(int x, int y, com.raylib.Raylib.Color color) {
        var anim = new ParticleAnimation();
        for (int i = 0; i < 15; i++) {
            anim.add(new Particle(x, y, color));
        }
        return anim;
    }

    // Metodo per Power-Up (Raccolta risorsa)
    public static  ParticleAnimation  createPowerUpEffect(float x, float y) {
        var anim = new ParticleAnimation();
        for (int i = 0; i < 10; i++) {
            Particle p = new Particle(x, y, GOLD);
            p.vy = -Math.abs(p.vy) * 2; // Le particelle dei powerup volano verso l'alto
            anim.add(p);
        }
        return anim;
    }
}
