package ru.kspt.highload.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import ru.kspt.highload.dto.ReplicaResponse;
import ru.kspt.highload.dto.ResponseStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TaskScheduler {
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();

    private final Queue<Future<ReplicaResponse>> abandoned = new ConcurrentLinkedQueue<>();

    private ExecutorService executor;

    public void start() {
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat(this.getClass().getSimpleName() + "-pool-%d").build());
    }

    public void stop() {
        tasks.clear();
        abandoned.stream().filter(f -> !f.isDone()).forEach(f -> f.cancel(true));
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    void schedule(final Function<Replica, ReplicaResponse> request, final List<Replica> replicas) {
        final long currentThreadId = Thread.currentThread().getId();
        final Task task = new Task();
        for (Replica replica : replicas) {
            task.responses.add(executor.submit(() -> {
                final ReplicaResponse response = request.apply(replica);
                if (response.responseStatus == ResponseStatus.ACK) {
                    task.acksGathered.incrementAndGet();
                }
                return response;
            }));
        }
        if (tasks.containsKey(currentThreadId)) {
            abandoned.addAll(tasks.get(currentThreadId).responses);
        }
        tasks.put(currentThreadId, task);
    }

    List<ReplicaResponse> getNeededAckedResponses(final int acksNeeded) {
        final long currentThreadId = Thread.currentThread().getId();
        awaitCompletion(acksNeeded, currentThreadId);
        final List<ReplicaResponse> responses = tasks.get(currentThreadId).responses.stream()
                .filter(Future::isDone)
                .map(this::getResult)
                .filter(r -> r.responseStatus == ResponseStatus.ACK)
                .collect(Collectors.toList());
        return responses;
    }

    private ReplicaResponse getResult(final Future<ReplicaResponse> future) {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException __) {
            log.warn(Thread.currentThread().getName() + " was interrupted");
            log.error("The interrupted Future must have been already completed!");
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (TimeoutException e) {
            log.error("The was timeout on Future that must have been already completed!");
            return ReplicaResponse.fail();
        } catch (ExecutionException e) {
            log.error("Unexpected error in " + this.getClass().getSimpleName(), e);
            return ReplicaResponse.fail();
        }
    }

    private void awaitCompletion(final int acksNeeded, final long currentThreadId) {
        while (!Thread.currentThread().isInterrupted()) {
            if (isEverythingCompleted(currentThreadId) ||
                    tasks.get(currentThreadId).acksGathered.get() >= acksNeeded) {
                return;
            }
        }
        log.warn(Thread.currentThread().getName() + " was interrupted");
    }

    private boolean isEverythingCompleted(final long threadId) {
        boolean everythingCompleted = true;
        for (Future<ReplicaResponse> future : tasks.get(threadId).responses) {
            everythingCompleted &= future.isDone();
        }
        return everythingCompleted;
    }

    private static class Task {
        final List<Future<ReplicaResponse>> responses = new ArrayList<>();

        final AtomicInteger acksGathered = new AtomicInteger(0);
    }
}
