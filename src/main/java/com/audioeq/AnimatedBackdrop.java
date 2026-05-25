package com.audioeq;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Animated background for the landing experience.
 */
public class AnimatedBackdrop extends Canvas {
    private final List<Orb> orbs = new ArrayList<>();
    private final Random random = new Random();
    private AnimationTimer timer;

    public AnimatedBackdrop() {
        for (int i = 0; i < 12; i++) {
            orbs.add(new Orb());
        }

        timer = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double delta = (now - last) / 1_000_000_000.0;
                last = now;
                update(delta);
                draw();
            }
        };
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void update(double delta) {
        double w = getWidth();
        double h = getHeight();

        for (Orb orb : orbs) {
            orb.x += orb.vx * delta;
            orb.y += orb.vy * delta;

            if (orb.x < -orb.radius || orb.x > w + orb.radius) {
                orb.vx *= -1;
            }
            if (orb.y < -orb.radius || orb.y > h + orb.radius) {
                orb.vy *= -1;
            }
        }
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.setFill(Color.web("#05070f"));
        gc.fillRect(0, 0, w, h);

        for (Orb orb : orbs) {
            RadialGradient gradient = new RadialGradient(
                0, 0,
                orb.x, orb.y,
                orb.radius,
                false,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, orb.color.deriveColor(0, 1.0, 1.0, 0.35)),
                new Stop(1, Color.TRANSPARENT)
            );
            gc.setFill(gradient);
            gc.fillOval(orb.x - orb.radius, orb.y - orb.radius, orb.radius * 2, orb.radius * 2);
        }
    }

    private class Orb {
        double x = random.nextDouble() * 1200;
        double y = random.nextDouble() * 800;
        double radius = 120 + random.nextDouble() * 240;
        double vx = (random.nextDouble() - 0.5) * 30;
        double vy = (random.nextDouble() - 0.5) * 30;
        Color color = Color.hsb(random.nextDouble() * 360, 0.6, 0.9);
    }
}
