package com.cb.patterns.command;

import java.util.ArrayDeque;
import java.util.Deque;


public class CommandInvoker {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 50;

    public synchronized void execute(Command command) {
        command.execute();
        if (undoStack.size() >= MAX_HISTORY) {
            undoStack.pollLast();
        }
        undoStack.push(command);
        redoStack.clear();
    }

    public synchronized boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    public synchronized boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }

    public synchronized int getUndoCount() { return undoStack.size(); }
    public synchronized int getRedoCount() { return redoStack.size(); }
    public synchronized String peekUndo() {
        return undoStack.isEmpty() ? null : undoStack.peek().getDescription();
    }
    public synchronized String peekRedo() {
        return redoStack.isEmpty() ? null : redoStack.peek().getDescription();
    }
}
