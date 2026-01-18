package skid.supreme.blon.animation;

public class Animation {
    private final AnimationType type;
    private final long duration;

    private long startTime;
    private boolean running = false;
    private boolean finished = false;
    private Direction direction;

    public Animation(AnimationType type, long durationMillis) {
        this.type = type;
        this.duration = durationMillis;
        this.direction = Direction.FORWARDS;
    }

    public void start() {
        start(Direction.FORWARDS);
    }

    public void start(Direction direction) {
        this.startTime = System.currentTimeMillis();
        this.running = true;
        this.finished = false;
        this.direction = direction;
    }

    public void finishedAt(Direction dir) {
        running = false;
        finished = true;
        direction = dir;
        startTime = System.currentTimeMillis() - (dir.isForwards() ? duration : 0);
    }

    public void reverse() {
        double currentProgress = getProgress();
        direction = direction.opposite();

        long newElapsed;
        if (direction.isForwards()) newElapsed = (long)(currentProgress * duration);
        else newElapsed = (long)((1.0 - currentProgress) * duration);

        startTime = System.currentTimeMillis() - newElapsed;
        running = true;
        finished = false;
    }

    public void reset() {
        startTime = System.currentTimeMillis();
        running = false;
        finished = false;
        direction = Direction.FORWARDS;
    }

    public double getProgress() {
        boolean forward = direction.isForwards();

        if (!running) {
            if (finished) return forward ? 1.0 : 0.0;
            else return forward ? 0.0 : 1.0;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= duration) {
            running = false;
            finished = true;
            return forward ? 1.0 : 0.0;
        }

        double t = (double) elapsed / duration;
        double easedProgress = applyEasing(t);

        return forward ? easedProgress : 1.0 - easedProgress;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public Direction getDirection() {
        return direction;
    }

    private double applyEasing(double t) {
        return switch (type) {
            case Linear -> t;
            case EaseIn -> t * t;
            case EaseOut -> 1.0 - Math.pow(1.0 - t, 2);
            case EaseInOut -> t < 0.5
                    ? 2.0 * t * t
                    : 1.0 - 2.0 * Math.pow(1.0 - t, 2);
            case Bounce -> applyBounce(t);
            case Elastic -> applyElastic(t);
            case Back -> applyBack(t);
            case Sine -> applySine(t);
        };
    }

    private double applyBounce(double t) {
        if (t < 1.0 / 2.75) {
            return 7.5625 * t * t;
        } else if (t < 2.0 / 2.75) {
            t -= 1.5 / 2.75;
            return 7.5625 * t * t + 0.75;
        } else if (t < 2.5 / 2.75) {
            t -= 2.25 / 2.75;
            return 7.5625 * t * t + 0.9375;
        } else {
            t -= 2.625 / 2.75;
            return 7.5625 * t * t + 0.984375;
        }
    }

    private double applyElastic(double t) {
        double c4 = (2 * Math.PI) / 3;
        return t == 0 ? 0 : t == 1 ? 1 : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * c4);
    }

    private double applyBack(double t) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return c3 * t * t * t - c1 * t * t;
    }

    private double applySine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }
}
