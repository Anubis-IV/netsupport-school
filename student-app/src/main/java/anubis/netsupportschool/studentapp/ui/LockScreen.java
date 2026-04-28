package anubis.netsupportschool.studentapp.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Root container that fills the screen at all times.
 *
 * Two modes:
 *   showPlainLock() → big padlock message
 *   showContent(node) → replace centre content with given node (exam UI, name entry, etc.)
 */
public class LockScreen {

    private final BorderPane root;
    private final StackPane  centre;

    public LockScreen() {
        root   = new BorderPane();
        root.getStyleClass().add("lock-root");

        centre = new StackPane();
        root.setCenter(centre);

        showPlainLock();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public BorderPane getRoot() { return root; }

    /** Switch to the plain "computer is locked" splash. */
    public void showPlainLock() {
        centre.getChildren().setAll(buildPlainLockNode());
    }

    /** Replace the centre content with any arbitrary node (exam, name-entry, etc.). */
    public void showContent(Node node) {
        centre.getChildren().setAll(node);
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private Node buildPlainLockNode() {
        VBox box = new VBox(24);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("lock-splash");

        Text icon = new Text("🔒");
        icon.getStyleClass().add("lock-icon");

        Text title = new Text("This Computer is Locked");
        title.getStyleClass().add("lock-title");
        title.setTextAlignment(TextAlignment.CENTER);

        Text sub = new Text("Your session is being managed by your instructor.\nPlease wait for instructions.");
        sub.getStyleClass().add("lock-sub");
        sub.setTextAlignment(TextAlignment.CENTER);

        // Live clock
        Text clock = new Text(currentTime());
        clock.getStyleClass().add("lock-clock");

        // Update clock every second via a JavaFX timeline
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                e -> clock.setText(currentTime())
            )
        );
        tl.setCycleCount(javafx.animation.Animation.INDEFINITE);
        tl.play();

        box.getChildren().addAll(icon, title, sub, clock);
        return box;
    }

    private static String currentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
