package ru.kspt.highload.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.kspt.highload.dto.ReplicaResponse;
import ru.kspt.highload.dto.ResponseStatus;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class TaskScheduler {
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    private final Queue<Future<?>> abandoned = new ConcurrentLinkedQueue<>();

    private final int maxReplicasCount;

    private ExecutorService executor;

    public void start() {
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat(this.getClass().getSimpleName() + "-pool-%d").build());
    }

    public void stop() {
        tasks.values().forEach(f -> f.drainFuturesAndClear(abandoned));
        tasks.clear();
        abandoned.stream().filter(f -> !f.isDone()).forEach(f -> f.cancel(true));
        abandoned.clear();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    void schedule(final Function<Replica, ReplicaResponse> request, final List<Replica> replicas) {
        final long currentThreadId = Thread.currentThread().getId();
        final Task task = renewTaskForThread(currentThreadId);
        for (Replica replica : replicas) {
            task.futureResponses.add(executor.submit(() -> {
                final ReplicaResponse response = request.apply(replica);
                task.responses.add(response);
            }));
        }
    }

    private Task renewTaskForThread(final long threadId) {
        if (tasks.containsKey(threadId)) {
            final Task task = tasks.get(threadId);
            task.drainFuturesAndClear(abandoned);
            return task;
        } else {
            final Task task = new Task(maxReplicasCount);
            tasks.put(threadId, task);
            return task;
        }
    }

    List<ReplicaResponse> getEnoughResponses(final ReplicationFactor replicationFactor) {
        final long currentThreadId = Thread.currentThread().getId();
        try {
            return awaitCompletion(currentThreadId, replicationFactor);
        } catch (InterruptedException e) {
            log.warn(Thread.currentThread().getName() + " was interrupted");
            return new ArrayList<>();
        }
    }

    private List<ReplicaResponse> awaitCompletion(final long currentThreadId,
            final ReplicationFactor rf) throws InterruptedException {
        final List<ReplicaResponse> ackedResponses = new ArrayList<>();
        int totalGathered = 0;
        while (!Thread.currentThread().isInterrupted()) {
            final ReplicaResponse response = tasks.get(currentThreadId).responses.take();
            totalGathered++;
            if (response.responseStatus == ResponseStatus.ACK) {
                ackedResponses.add(response);
                if (ackedResponses.size() == rf.ack) return ackedResponses;
            }
            if (totalGathered == rf.from) return ackedResponses;
        }
        log.warn(Thread.currentThread().getName() + " was interrupted on awaiting completion");
        return new ArrayList<>();
    }

    private static class Task {
        private final List<Future<?>> futureResponses = new ArrayList<>();

        private final BlockingQueue<ReplicaResponse> responses;

        Task(final int from) {
            responses = new ArrayBlockingQueue<>(from);
        }

        void drainFuturesAndClear(final Collection<Future<?>> sink) {
            sink.addAll(futureResponses);
            futureResponses.clear();
            responses.clear();
        }
    }
}
