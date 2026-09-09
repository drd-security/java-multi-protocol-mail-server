
/**
 * ServerThreadPool manages a pool of threads for handling server tasks.
 * It uses a fixed thread pool to limit the number of concurrent threads.
 * author
 * -dave ronic donkeng
 * -leslie lucynda tingue
 * version 1.0
 */
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerThreadPool {

    private final ExecutorService executor;

    public ServerThreadPool(int maxThreads) {
        this.executor = Executors.newFixedThreadPool(maxThreads);
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public void shutdown() {
        executor.shutdownNow(); // Immediate shutdown
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }
}
