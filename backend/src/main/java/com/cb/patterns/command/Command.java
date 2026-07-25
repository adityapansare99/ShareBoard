package com.cb.patterns.command;

public interface Command {
    void execute();
    void undo();
    String getDescription();
    Long getTargetCardId();
}
