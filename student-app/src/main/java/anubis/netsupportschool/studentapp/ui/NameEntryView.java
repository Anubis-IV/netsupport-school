package anubis.netsupportschool.studentapp.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * Simple name-entry screen displayed when requireNameEntry = true.
 * On confirm, calls the provided callback with the entered name.
 */
public class NameEntryView {

    private final VBox root;

    public NameEntryView(Consumer<String> onNameEntered) {
        root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(420);
        root.getStyleClass().add("name-entry-box");

        Text title = new Text("Enter Your Name");
        title.getStyleClass().add("ne-title");

        Label hint = new Label("Please enter your full name before the exam begins.");
        hint.getStyleClass().add("ne-hint");
        hint.setWrapText(true);

        TextField nameField = new TextField();
        nameField.setPromptText("Full name…");
        nameField.getStyleClass().add("ne-field");
        nameField.setMaxWidth(320);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("ne-error");
        errorLabel.setVisible(false);

        Button confirmBtn = new Button("Start Exam →");
        confirmBtn.getStyleClass().add("btn-primary");

        Runnable confirm = () -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                errorLabel.setText("Name cannot be empty.");
                errorLabel.setVisible(true);
                return;
            }
            onNameEntered.accept(name);
        };

        confirmBtn.setOnAction(e -> confirm.run());
        nameField.setOnAction(e -> confirm.run());

        root.getChildren().addAll(title, hint, nameField, errorLabel, confirmBtn);
    }

    public Node getRoot() { return root; }
}
