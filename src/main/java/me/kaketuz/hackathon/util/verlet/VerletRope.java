package me.kaketuz.hackathon.util.verlet;

import com.projectkorra.projectkorra.GeneralMethods;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

        VerletPoint last       = points.getLast();
        VerletPoint secondLast = points.get(points.size() - 2);
        Vector forwardDir      = last.getPosition()
                .clone()
                .subtract(secondLast.getPosition())
                .normalize();

        Location lastLoc = new Location(world,
                last.getPosition().getX(),
                last.getPosition().getY(),
                last.getPosition().getZ());

        double radius = segmentLen * 1.5;
        int steps = 8;

        class Candidate {
            Location loc;
            double score;
            Candidate(Location loc, double score) {
                this.loc = loc; this.score = score;
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int xi = -steps; xi <= steps; xi++) {
            for (int yi = -steps; yi <= steps; yi++) {
                for (int zi = -steps; zi <= steps; zi++) {
                    double dx = xi * radius / steps;
                    double dy = yi * radius / steps;
                    double dz = zi * radius / steps;
                    Vector offset = new Vector(dx, dy, dz);
                    if (offset.lengthSquared() > radius * radius) continue;

                    Location candLoc = lastLoc.clone().add(offset);
                    if (GeneralMethods.isSolid(candLoc.getBlock())) continue;

                    Vector toCand = offset.clone().normalize();
                    double score = forwardDir.dot(toCand);
                    candidates.add(new Candidate(candLoc, score));
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(b.score, a.score));

        if (!candidates.isEmpty()) {
            Location chosen = candidates.getFirst().loc;
            VerletPoint newPoint = new VerletPoint(chosen, false);
            points.add(newPoint);
            sticks.add(new VerletStick(last, newPoint, segmentLen));
        }
    }

    public List<VerletPoint> getPoints() {
        return points;
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



    public boolean isStretched() {
        for (VerletStick stick : sticks) {
            if (stick.getCurrentLength() > segmentLen) return true;
        }
        return false;
    }

    public boolean isStretchedSegment(double threshold) {
        for (VerletStick stick : sticks) {
            if (stick.getCurrentLength() >= threshold) return true;
        }
        return false;
    }

    public void normalizeRope(boolean ignoreCollision) {
        boolean prevCollision = this.collision;
        if (ignoreCollision) setCollisionEnabled(false);

        for (VerletStick stick : sticks) {
            stick.setRestLength(segmentLen);
            stick.constrain();
        }
        if (ignoreCollision) setCollisionEnabled(prevCollision);
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
        if (!collision) return;

        final double EPS = 0.01;
        for (VerletPoint p : points) {
            if (!p.isCollisionEnabled() || p.isLocked()) continue;

            Vector curr = p.getPosition();
            Vector prev = p.getPrevious();
            Vector motion = curr.clone().subtract(prev);
            double dist = motion.length();
            if (dist == 0) continue;
            Vector dir = motion.clone().normalize();

            RayTraceResult result = world.rayTraceBlocks(
                    toLoc(prev),
                    dir,
                    dist,
                    FluidCollisionMode.NEVER,
                    true
            );
            if (result == null || result.getHitBlock() == null) continue;

            BlockFace face = result.getHitBlockFace();
            Vector hit = result.getHitPosition();

            Vector offset = face.getDirection().multiply(EPS);
            Vector corrected = hit.clone().add(offset);

            p.setPosition(corrected);
            p.setPrevious(corrected);
            p.dampenVelocity(0.3);
        }
    }

    private Location toLoc(Vector v) {
        return new Location(world, v.getX(), v.getY(), v.getZ());
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
            VerletPoint start = points.getFirst();
            start.setPosition(loc.toVector());
            start.setPrevious(loc.toVector());
        }
    }

    public void setEndPosition(Location loc) {
        if (points.size() > 1) {
            VerletPoint end = points.getLast();
            end.setPosition(loc.toVector());
            end.setPrevious(loc.toVector());
            end.lock(true);
            for (int i = 0; i < Math.max(2, constraintIterations); i++) {
                for (VerletStick s : sticks) s.constrain();
            }
        }
    }

}
