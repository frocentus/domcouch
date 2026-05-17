package com.domcouch.demo.kanban;

import com.domcouch.api.NotesException;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.*;

@Route("kanban")
public class KanbanView extends VerticalLayout {

    private final KanbanService service;
    private String currentBoardUnid;

    public KanbanView(KanbanService service) {
        this.service = service;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H1("DomCouch Kanban Board"));
        add(createToolbar());
        add(createBoardContainer());
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(true);
        toolbar.getStyle().set("background", "#f5f5f5");
        toolbar.getStyle().set("border-radius", "8px");

        Button newBoardBtn = new Button("+ New Board", e -> createNewBoard());
        newBoardBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button deleteBoardBtn = new Button("Delete Board", e -> deleteCurrentBoard());
        deleteBoardBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button loadBoardBtn = new Button("Load Board", e -> loadBoardDialog());
        loadBoardBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        toolbar.add(newBoardBtn, loadBoardBtn, deleteBoardBtn);
        return toolbar;
    }

    private Div createBoardContainer() {
        Div container = new Div();
        container.setId("board-container");
        container.setWidthFull();
        container.getStyle().set("display", "flex");
        container.getStyle().set("gap", "12px");
        container.getStyle().set("overflow-x", "auto");
        container.getStyle().set("min-height", "400px");
        container.getStyle().set("padding", "8px 0");
        return container;
    }

    private void createNewBoard() {
        TextField titleField = new TextField("Board Title");
        titleField.setValue("My Kanban Board");

        Dialog dialog = new Dialog(titleField);
        Button createBtn = new Button("Create", e -> {
            try {
                KanbanBoard board = service.createBoardWithLanes(titleField.getValue());
                currentBoardUnid = board.getUnid();
                refreshBoard();
                dialog.close();
                Notification.show("Board created: " + board.getTitle());
            } catch (NotesException ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(createBtn);
        dialog.open();
    }

    private void loadBoardDialog() {
        Dialog dialog = new Dialog();
        VerticalLayout content = new VerticalLayout();
        try {
            var boards = service.getBoards();
            if (boards.isEmpty()) {
                content.add(new Span("No boards found. Create one with '+ New Board'."));
            }
            for (KanbanBoard board : boards) {
                Button btn = new Button(board.getTitle(), e -> {
                    currentBoardUnid = board.getUnid();
                    refreshBoard();
                    dialog.close();
                });
                btn.setWidthFull();
                content.add(btn);
            }
        } catch (NotesException ex) {
            content.add(new Span("Error loading boards: " + ex.getMessage()));
        }
        dialog.add(content);
        dialog.open();
    }

    private void deleteCurrentBoard() {
        if (currentBoardUnid == null) {
            Notification.show("No board selected");
            return;
        }
        try {
            service.deleteBoard(currentBoardUnid);
            currentBoardUnid = null;
            refreshBoard();
            Notification.show("Board deleted");
        } catch (NotesException ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }

    private void refreshBoard() {
        System.out.println("refreshBoard: currentBoardUnid=" + currentBoardUnid);
        Div container = (Div) getChildren()
                .filter(c -> c.getId().map("board-container"::equals).orElse(false))
                .findFirst().orElse(null);
        if (container == null) return;
        container.removeAll();

        if (currentBoardUnid == null) {
            container.add(new Span("Select or create a board to begin."));
            return;
        }

        try {
            Map<String, Object> state = service.getBoardState(currentBoardUnid);
            System.out.println("refreshBoard: state keys=" + state.keySet() + " lanes=" + (state.get("lanes") != null ? ((java.util.List)state.get("lanes")).size() : "null"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lanes = (List<Map<String, Object>>) state.get("lanes");

            // Always show Add Task button when board is loaded
            Button newTaskBtn = new Button("+ Add Task", e -> showNewTaskDialog());
            newTaskBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            add(newTaskBtn);

            if (lanes == null || lanes.isEmpty()) {
                container.add(new Span("No tasks yet — use '+ Add Task' to create one."));
                return;
            }

            for (Map<String, Object> laneData : lanes) {
                container.add(createLaneColumn(laneData));
            }
        } catch (NotesException ex) {
            container.add(new Span("Error: " + ex.getMessage()));
        }
    }

    private Div createLaneColumn(Map<String, Object> laneData) {
        String laneUnid = (String) laneData.get("unid");
        String laneTitle = (String) laneData.get("title");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) laneData.get("tasks");

        Div column = new Div();
        column.addClassName(LumoUtility.BoxSizing.BORDER);
        column.getStyle().set("min-width", "280px");
        column.getStyle().set("max-width", "320px");
        column.getStyle().set("background", "#e8e8e8");
        column.getStyle().set("border-radius", "8px");
        column.getStyle().set("padding", "12px");
        column.getStyle().set("display", "flex");
        column.getStyle().set("flex-direction", "column");
        column.getStyle().set("gap", "8px");

        // Lane header
        H4 header = new H4(laneTitle + " (" + tasks.size() + ")");
        header.getStyle().set("margin", "0");
        header.getStyle().set("padding", "4px 0");
        column.add(header);

        // Task cards
        for (Map<String, Object> taskData : tasks) {
            column.add(createTaskCard(taskData, laneUnid));
        }

        return column;
    }

    private Div createTaskCard(Map<String, Object> taskData, String parentLaneUnid) {
        String taskUnid = (String) taskData.get("unid");
        String title = (String) taskData.get("title");
        String priority = (String) taskData.get("priority");
        String assignee = (String) taskData.get("assignee");

        Div card = new Div();
        card.getStyle().set("background", "#ffffff");
        card.getStyle().set("border-radius", "6px");
        card.getStyle().set("padding", "10px 12px");
        card.getStyle().set("box-shadow", "0 1px 3px rgba(0,0,0,0.12)");
        card.getStyle().set("cursor", "pointer");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-weight", "600");
        titleSpan.getStyle().set("font-size", "14px");

        String prioColor = switch (priority) {
            case "Critical" -> "#d32f2f";
            case "High" -> "#f57c00";
            case "Medium" -> "#1976d2";
            default -> "#388e3c";
        };
        Span prioBadge = new Span(priority);
        prioBadge.getStyle().set("font-size", "11px");
        prioBadge.getStyle().set("background", prioColor);
        prioBadge.getStyle().set("color", "white");
        prioBadge.getStyle().set("padding", "1px 6px");
        prioBadge.getStyle().set("border-radius", "3px");

        Span assigneeSpan = new Span(assignee);
        assigneeSpan.getStyle().set("font-size", "11px");
        assigneeSpan.getStyle().set("color", "#666");

        card.add(titleSpan, new Html("<br>"), prioBadge, new Html("&nbsp;"), assigneeSpan);

        // Context menu: move to another lane, delete
        card.addClickListener(e -> showTaskContextMenu(taskUnid, title, parentLaneUnid));

        return card;
    }

    private void showNewTaskDialog() {
        if (currentBoardUnid == null) return;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("New Task");

        TextField titleField = new TextField("Title");
        Select<String> laneSelect = new Select<>();
        laneSelect.setLabel("Lane");
        try {
            Map<String, Object> state = service.getBoardState(currentBoardUnid);
            @SuppressWarnings("unchecked")
            var lanes = (List<Map<String, Object>>) state.get("lanes");
            if (lanes != null && !lanes.isEmpty()) {
                laneSelect.setItems(lanes.stream().map(l -> (String) l.get("title")).toList());
                laneSelect.setValue((String) lanes.get(0).get("title"));
            }
        } catch (NotesException ignored) {}

        Select<String> prioSelect = new Select<>();
        prioSelect.setLabel("Priority");
        prioSelect.setItems("Low", "Medium", "High", "Critical");

        TextField assigneeField = new TextField("Assignee");

        dialog.add(titleField, laneSelect, prioSelect, assigneeField);

        Button createBtn = new Button("Create", e -> {
            try {
                Map<String, Object> state = service.getBoardState(currentBoardUnid);
                @SuppressWarnings("unchecked")
                var lanes = (List<Map<String, Object>>) state.get("lanes");
                String laneUnid = (lanes != null) ? lanes.stream()
                        .filter(l -> laneSelect.getValue().equals(l.get("title")))
                        .findFirst().map(l -> (String) l.get("unid")).orElse(null) : null;
                if (laneUnid != null) {
                    service.addTask(laneUnid, titleField.getValue(),
                            prioSelect.getValue(), assigneeField.getValue());
                    refreshBoard();
                }
                dialog.close();
            } catch (NotesException ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(createBtn);
        dialog.open();
    }

    private void showTaskContextMenu(String taskUnid, String taskTitle, String currentLaneUnid) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(taskTitle);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);

        // Move to another lane
        Select<String> moveSelect = new Select<>();
        moveSelect.setLabel("Move to lane");
        try {
            Map<String, Object> state = service.getBoardState(currentBoardUnid);
            @SuppressWarnings("unchecked")
            var lanes = (List<Map<String, Object>>) state.get("lanes");
            if (lanes != null) moveSelect.setItems(lanes.stream().map(l -> (String) l.get("title")).toList());
        } catch (NotesException ignored) {}

        Button moveBtn = new Button("Move", e -> {
            try {
                Map<String, Object> state = service.getBoardState(currentBoardUnid);
                @SuppressWarnings("unchecked")
                var lanes = (List<Map<String, Object>>) state.get("lanes");
                String targetLaneUnid = lanes.stream()
                        .filter(l -> moveSelect.getValue().equals(l.get("title")))
                        .findFirst().map(l -> (String) l.get("unid")).orElse(null);
                if (targetLaneUnid != null) {
                    service.moveTask(taskUnid, targetLaneUnid);
                    refreshBoard();
                    dialog.close();
                }
            } catch (NotesException ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        moveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button deleteBtn = new Button("Delete", e -> {
            try {
                service.deleteTask(taskUnid);
                refreshBoard();
                dialog.close();
            } catch (NotesException ex) {
                Notification.show("Error: " + ex.getMessage());
            }
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        content.add(moveSelect, new HorizontalLayout(moveBtn, deleteBtn));
        dialog.add(content);
        dialog.open();
    }
}
