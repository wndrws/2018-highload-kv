package ru.kspt.highload.service;

import lombok.extern.slf4j.Slf4j;
import ru.kspt.highload.dto.ReplicaResponse;
import ru.kspt.highload.dto.ResponseStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TaskScheduler {
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final List<Future<ReplicaResponse>> tasks = new ArrayList<>();

    private final AtomicInteger acksGathered = new AtomicInteger(0);

    private ExecutorService executor;

    public void start() {
        executor = Executors.newCachedThreadPool();
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    void schedule(final Function<Replica, ReplicaResponse> request, final List<Replica> replicas) {
        for (Replica replica : replicas) {
            tasks.add(executor.submit(() -> {
                final ReplicaResponse response = request.apply(replica);
                if (response.responseStatus == ResponseStatus.ACK) {
                    acksGathered.incrementAndGet();
                }
                return response;
            }));
        }
    }

    List<ReplicaResponse> getNeededAckedResponses(final int acksNeeded) {
        awaitCompletion(acksNeeded);
        final List<ReplicaResponse> ackedResponses = tasks.stream()
                .filter(Future::isDone)
                .map(this::getResult)
                .filter(r -> r.responseStatus == ResponseStatus.ACK)
                .collect(Collectors.toList());
        clearTasks();
        return ackedResponses;
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

    private void awaitCompletion(final int acksNeeded) {
        while (!Thread.currentThread().isInterrupted()) {
            if (isEverythingCompleted() || acksGathered.get() >= acksNeeded) {
                break;
            }
        }
    }

    private boolean isEverythingCompleted() {
        boolean everythingCompleted = true;
        for (Future<ReplicaResponse> task : tasks) {
            everythingCompleted &= task.isDone();
        }
        return everythingCompleted;
    }

    private void clearTasks() {
        tasks.stream()
                .filter(f -> !f.isDone())
                .forEach(f -> f.cancel(true));
        tasks.clear();
        acksGathered.set(0);
    }
}
