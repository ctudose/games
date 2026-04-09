package games.test;

import javafx.application.Platform;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test helper to initialize the JavaFX toolkit once.
 *
 * Some net/server client code uses {@code Platform.runLater(...)} even in headless unit tests,
 * so we must ensure the JavaFX runtime is started for integration tests.
 */
public final class JfxTestUtil {

    private static final AtomicBoolean started = new AtomicBoolean(false);

    private JfxTestUtil() {
    }

    public static void initJfx() {
        if (started.get()) {
            return;
        }

        synchronized (JfxTestUtil.class) {
            if (started.get()) {
                return;
            }

            try {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.startup(latch::countDown);
                started.set(true);
                try {
                    latch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (IllegalStateException alreadyStarted) {
                // Toolkit already started elsewhere.
                started.set(true);
            }
        }
    }

    public static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get free port", e);
        }
    }
}

