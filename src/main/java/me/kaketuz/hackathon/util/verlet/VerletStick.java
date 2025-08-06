package me.kaketuz.hackathon.util.verlet;

import org.bukkit.util.Vector;

public class VerletStick {
    private VerletPoint a, b;
    private double restLength;

    public VerletStick(VerletPoint a, VerletPoint b, double length) {
        this.a = a;
        this.b = b;
        this.restLength = length;
    }

    public VerletPoint getA() {
        return a;
    }

    public VerletPoint getB() {
        return b;
    }

    public double getCurrentLength() {
        return b.getPosition().distance(a.getPosition());
    }

    public void setRestLength(double restLength) {
        this.restLength = restLength;
    }

    public void constrain() {
        Vector delta = b.getPosition().clone().subtract(a.getPosition());
        double current = delta.length();
        if (current == 0) return;

        double diff = (current - restLength) / current;
        Vector offset = delta.multiply(0.5 * diff);

        if (!a.isLocked() && !b.isLocked()) {
            a.setPosition(a.getPosition().clone().add(offset));
            b.setPosition(b.getPosition().clone().subtract(offset));
        } else if (!a.isLocked()) {
            a.setPosition(a.getPosition().clone().add(offset.multiply(2)));
        } else if (!b.isLocked()) {
            b.setPosition(b.getPosition().clone().subtract(offset.multiply(2)));
        }
    }

}
