package me.kaketuz.hackathon.util.verlet;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class VerletPoint {
    private Vector position;
    private Vector previous;
    private boolean locked;
    private boolean gravityEnabled;
    private boolean collisionEnabled;

    public VerletPoint(Location loc, boolean locked) {
        this.position = loc.toVector();
        this.previous = loc.toVector();
        this.locked = locked;
        this.gravityEnabled = true;
        this.collisionEnabled = true;
    }

    public void update(double delta, Vector gravity) {
        if (locked) return;

        delta = Math.min(delta, 0.05);

        Vector velocity = position.clone().subtract(previous);
        if (gravityEnabled) velocity.add(gravity.clone().multiply(delta));

        Vector next = position.clone().add(velocity);
        previous = position.clone();
        position = next;
    }


    public void applyVelocity(Vector vel) {
        if (locked) return;
        previous = previous.subtract(vel);
    }

    public void setGravityEnabled(boolean enabled) {
        this.gravityEnabled = enabled;
    }

    public void setCollisionEnabled(boolean enabled) {
        this.collisionEnabled = enabled;
    }

    public boolean isCollisionEnabled() {
        return collisionEnabled;
    }


    public boolean isGravityEnabled() {
        return gravityEnabled;
    }

    public void dampenVelocity(double factor) {
        Vector velocity = position.clone().subtract(previous);
        velocity.multiply(factor);
        previous = position.clone().subtract(velocity);
    }


    public Vector getPosition() {
        return position;
    }

    public void setPosition(Vector position) {
        this.position = position;
    }

    public void lock(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setPrevious(Vector prev) {
        this.previous = prev.clone();
    }

    public Vector getPrevious() {
        return previous;
    }
}