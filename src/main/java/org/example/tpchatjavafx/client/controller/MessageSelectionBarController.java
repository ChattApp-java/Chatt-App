package org.example.tpchatjavafx.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MessageSelectionBarController {
    @FXML private Label selectedCountLabel;
    @FXML private Button copyButton;
    @FXML private Button forwardButton;
    @FXML private Button deleteButton;
    @FXML private Button selectAllButton;
    @FXML private Button closeButton;

    private Runnable copyAction = () -> {};
    private Runnable forwardAction = () -> {};
    private Runnable deleteAction = () -> {};
    private Runnable selectAllAction = () -> {};
    private Runnable closeAction = () -> {};

    public void setHandlers(Runnable copyAction, Runnable forwardAction, Runnable deleteAction,
                            Runnable selectAllAction, Runnable closeAction) {
        this.copyAction = copyAction == null ? () -> {} : copyAction;
        this.forwardAction = forwardAction == null ? () -> {} : forwardAction;
        this.deleteAction = deleteAction == null ? () -> {} : deleteAction;
        this.selectAllAction = selectAllAction == null ? () -> {} : selectAllAction;
        this.closeAction = closeAction == null ? () -> {} : closeAction;
    }

    public void setSelectedCount(int count) {
        if (selectedCountLabel != null) {
            selectedCountLabel.setText(String.valueOf(Math.max(count, 0)));
        }
    }

    @FXML private void onCopy() { copyAction.run(); }
    @FXML private void onForward() { forwardAction.run(); }
    @FXML private void onDelete() { deleteAction.run(); }
    @FXML private void onSelectAll() { selectAllAction.run(); }
    @FXML private void onClose() { closeAction.run(); }
}
