package me.kaketuz.hackathon.util.verlet;

import com.projectkorra.projectkorra.GeneralMethods;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class VerletRope {
    private List<VerletPoint> points = new ArrayList<>();
    private List<VerletStick> sticks = new ArrayList<>();
    private Vector gravity = new Vector(0, -9.8, 0);
    private int constraintIterations = 5;
    private World world;
    private boolean collision = true;
    private double segmentLen;

    public VerletRope(Location start, Location end, int segments, double segmentLen) {
        this.world = start.getWorld();
        Vector dir = end.toVector().subtract(start.toVector());
       this.segmentLen = segmentLen;
        dir.normalize().multiply(segmentLen);
        for (int i = 0; i <= segments; i++) {
            Location loc = start.clone().add(dir.clone().multiply(i));
            points.add(new VerletPoint(loc, i == 0));
        }
        for (int i = 1; i < points.size(); i++) {
            sticks.add(new VerletStick(points.get(i - 1), points.get(i), segmentLen));
        }
    }

    public double getSegmentLen() {
        return segmentLen;
    }

    public void addSegment() {
        if (points.size() < 2) return;

        VerletPoint last = points.getLast();
        VerletPoint secondLast = points.get(points.size() - 2);

        Vector dir = last.getPosition().clone().subtract(secondLast.getPosition()).normalize();
        Vector newPos = last.getPosition().clone().add(dir.multiply(1));

        VerletPoint newPoint = new VerletPoint(new Location(world, newPos.getX(), newPos.getY(), newPos.getZ()), false);
        points.add(newPoint);

        sticks.add(new VerletStick(last, newPoint, 1));
    }

    public void removeSegment() {
        if (points.size() <= 2) return;

        points.removeLast();
        sticks.removeLast();
    }

    public void removeSegmentFirst() {
        if (points.size() <= 2) return;

        points.removeFirst();
        sticks.removeFirst();
    }






    public void simulate(double delta) {
        for (VerletPoint p : points) {
            p.setGravityEnabled(p.isCollisionEnabled() || p.isGravityEnabled());
            p.update(delta, gravity);
        }

        for (int i = 0; i < constraintIterations; i++) {
            for (VerletStick s : sticks) s.constrain();
            if (collision) handleCollision();
        }
    }

    private void handleCollision() {
        for (VerletPoint p : points) {
            if (!p.isCollisionEnabled()) continue;
            Vector pos = p.getPosition();
            Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
            if (GeneralMethods.isSolid(loc.getBlock())) {
                loc.setY(loc.getBlockY() + 1);
                p.getPosition().setX(loc.getX());
                p.getPosition().setY(loc.getY());
                p.getPosition().setZ(loc.getZ());
                p.dampenVelocity(0.3);
            }
        }
    }









    private double getComponent(Vector vector, int index) {
        return switch (index) {
            case 0 -> vector.getX();
            case 1 -> vector.getY();
            case 2 -> vector.getZ();
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }

    private void setComponent(Vector vector, int index, double value) {
        switch (index) {
            case 0 -> vector.setX(value);
            case 1 -> vector.setY(value);
            case 2 -> vector.setZ(value);
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        }
    }




    public List<Location> getRenderPoints() {
        List<Location> list = new ArrayList<>();
        for (VerletPoint p : points) {
            Vector v = p.getPosition();
            list.add(new Location(world, v.getX(), v.getY(), v.getZ()));
        }
        return list;
    }

    public void setGravity(Vector gravity) {
        this.gravity = gravity;
    }

    public void setConstraintIterations(int iters) {
        this.constraintIterations = iters;
    }

    public void setCollisionEnabled(boolean enabled) {
        this.collision = enabled;
        for (VerletPoint p : points) p.setCollisionEnabled(enabled);
    }

    public void lockPoint(int index, boolean locked) {
        if (index >= 0 && index < points.size()) points.get(index).lock(locked);
    }

    public void applyVelocity(Vector vel) {
        for (VerletPoint p : points) p.applyVelocity(vel);
    }

    public void setRestLength(double length) {
        for (VerletStick s : sticks) s = new VerletStick(s.getA(), s.getB(), length);
    }

    public void setStartPosition(Location loc) {
        if (!points.isEmpty()) {
            VerletPoint start = points.get(0);
            start.setPosition(loc.toVector());
            start.lock(true);
        }
    }

    public void setEndPosition(Location loc) {
        if (points.size() > 1) {
            VerletPoint end = points.get(points.size() - 1);
            end.setPosition(loc.toVector());
            end.lock(true);
        }
    }
}
