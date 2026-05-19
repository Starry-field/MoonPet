package io.github.starrymoon.moonpet.task;

public interface Task {
	void start();

	void tick();

	boolean isDone();

	void report();
}
