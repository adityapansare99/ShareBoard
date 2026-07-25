package com.cb.service;

import com.cb.patterns.command.CommandInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class UndoRedoService {

    private final Map<String, CommandInvoker> invokers = new ConcurrentHashMap<>();

    public CommandInvoker getInvoker(String sessionId) {
        return invokers.computeIfAbsent(sessionId, k -> new CommandInvoker());
    }

    public boolean undo(String sessionId) {
        CommandInvoker invoker = invokers.get(sessionId);
        return invoker != null && invoker.undo();
    }

    public boolean redo(String sessionId) {
        CommandInvoker invoker = invokers.get(sessionId);
        return invoker != null && invoker.redo();
    }

    public Map<String, Object> getHistory(String sessionId) {
        CommandInvoker invoker = invokers.get(sessionId);
        Map<String, Object> result = new java.util.HashMap<>();
        if (invoker == null) {
            result.put("undoCount", 0);
            result.put("redoCount", 0);
            result.put("peekUndo", null);
            result.put("peekRedo", null);
        } else {
            result.put("undoCount", invoker.getUndoCount());
            result.put("redoCount", invoker.getRedoCount());
            result.put("peekUndo", invoker.peekUndo());
            result.put("peekRedo", invoker.peekRedo());
        }
        return result;
    }

    public void removeInvoker(String sessionId) {
        invokers.remove(sessionId);
    }
}
