package net.kdt.pojavlaunch.progresskeeper;

public interface TaskCountListener {
    /**
     * @return whether to remove self after this callback.
     */
    boolean onUpdateTaskCount(int taskCount);
}
