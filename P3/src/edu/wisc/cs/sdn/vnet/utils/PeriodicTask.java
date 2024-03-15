package edu.wisc.cs.sdn.vnet.utils;

/**
 * A class representing a task that runs periodically at a specified interval
 */
public class PeriodicTask implements Runnable {
    private Thread thread;
    private Runnable task;
    private long duration;
    private boolean isDaemon;
    private volatile boolean active;

    /**
     * Constructs a new periodic task with the given task and duration,
     * and sets it as a daemon task
     *
     * @param task     the task to run periodically
     * @param duration the duration between each run of the task in milliseconds
     */
    public PeriodicTask(Runnable task, long duration) {
        this(task, duration, true);
    }

    /**
     * Constructs a new periodic task with the given task, duration,
     * and daemon status
     *
     * @param task     the task to run periodically
     * @param duration the duration between each run of the task in milliseconds
     * @param isDaemon whether the task should be a daemon task or not
     */
    public PeriodicTask(Runnable task, long duration, boolean isDaemon) {
        this.task = task;
        this.active = true;
        this.isDaemon = isDaemon;
        setDuration(duration);
    }

    /**
     * Get the periodically run task
     *
     * @return the periodically run task
     */
    public Runnable getTask() { return task; }

    /**
     * Sets the task to run periodically
     * Typical usage should not need to use this method, but it exists to
     * allow flexibility
     *
     * @param task the task to run periodically
     */
    public void setTask(Runnable task) { this.task = task; }

    /**
     * Get the interval duration of the periodic task
     *
     * @return the interval duration of the periodic task
     */
    public long getDuration() { return duration; }

    /**
     * Sets the duration between each run of the task
     * Typical usage should not need to use this method, but it exists to
     * allow flexibility
     *
     * @param duration the duration between each run of the task in milliseconds
     */
    public void setDuration(long duration) { this.duration = duration; }

    /**
     * Returns the daemon status of the period task thread
     *
     * @return true if the task is running on a daemon thread, false otherwise
     */
    public boolean isDaemon() { return isDaemon; }

    /**
     * Sets whether the task should be a daemon task or not
     *
     * @param isDaemon true if the task should be a daemon task, false otherwise
     */
    public void setDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
        this.thread.setDaemon(isDaemon);
    }

    /**
     * Runs the task periodically at the specified interval until the
     * task is stopped
     */
    @Override
    public void run() {
        while (active) {
            task.run();

            try { Thread.sleep(duration); }
            catch (InterruptedException e) { e.printStackTrace(System.err); }
        }
    }

    /**
     * Starts the periodic task
     */
    public void start() {
        thread = new Thread(this);
        thread.setDaemon(isDaemon);
        thread.start();
    }

    /**
     * Stops the periodic task
     */
    public void stop() {
        active = false;

        if (thread == null) return;

        try {
            thread.join();
            thread = null;
        }
        catch (InterruptedException e) { e.printStackTrace(System.err); }
    }
}
