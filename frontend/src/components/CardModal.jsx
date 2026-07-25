import React, { useState } from 'react';
import { editCard, deleteCard, changeCardState } from '../services/api';

const STATES = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
const PRIORITIES = ['', 'HIGH', 'MEDIUM', 'LOW'];

export default function CardModal({ card, boardCode, sessionId, onClose, onSaved, onDeleted }) {
  const [title, setTitle] = useState(card.title || '');
  const [description, setDescription] = useState(card.description || '');
  const [assignee, setAssignee] = useState(card.assignee || '');
  const [priority, setPriority] = useState(card.priority || '');
  const [dueDate, setDueDate] = useState(card.dueDate ? card.dueDate.substring(0, 10) : '');
  const [currentState, setCurrentState] = useState(card.state || 'TODO');
  const [saving, setSaving] = useState(false);

  function handleSave() {
    if (!title.trim()) return;
    setSaving(true);
    const body = { title, description, assignee, priority, dueDate, sessionId };
    editCard(boardCode, card.id, body)
      .then(() => {
        onSaved();
      })
      .catch(() => {
        alert('Failed to save card changes.');
      })
      .finally(() => setSaving(false));
  }

  function handleDelete() {
    if (!window.confirm('Delete this card?')) return;
    deleteCard(boardCode, card.id, sessionId)
      .then(onDeleted)
      .catch(() => {});
  }

  function handleStateChange(newState) {
    if (newState === currentState) return;
    changeCardState(boardCode, card.id, newState, sessionId)
      .then(() => {
        setCurrentState(newState);
        onSaved();
      })
      .catch(err => {
        alert(err?.response?.data?.message || `Cannot transition state to ${newState}`);
      });
  }

  return (
    <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 animate-fade-in" onClick={onClose}>
      <div
        className="bg-white rounded-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto border border-slate-200 shadow-2xl animate-scale-up text-slate-900"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="p-6 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-indigo-50 border border-indigo-200 text-indigo-700 flex items-center justify-center font-bold text-xs font-mono">
              #{card.id}
            </div>
            <h3 className="font-heading font-extrabold text-slate-900 text-base">
              Card Details
            </h3>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 transition-colors flex items-center justify-center"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-6 space-y-5">
          {/* Status State Pipeline */}
          <div>
            <label className="block text-[11px] font-bold text-slate-500 mb-2 uppercase tracking-wider">
              Status Pipeline
            </label>
            <div className="flex bg-slate-100 p-1 rounded-xl border border-slate-200">
              {STATES.map(st => (
                <button
                  key={st}
                  type="button"
                  onClick={() => handleStateChange(st)}
                  className={`flex-1 py-1.5 text-[11px] font-bold rounded-lg transition-all ${
                    currentState === st
                      ? 'bg-white text-indigo-700 shadow-xs border border-slate-200'
                      : 'text-slate-500 hover:text-slate-800'
                  }`}
                >
                  {st.replace('_', ' ')}
                </button>
              ))}
            </div>
          </div>

          {/* Title */}
          <div>
            <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Title</label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-900 font-semibold focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10"
              required
            />
          </div>

          {/* Description */}
          <div>
            <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Description</label>
            <textarea
              value={description}
              onChange={e => setDescription(e.target.value)}
              rows={3}
              placeholder="Task details and sub-notes..."
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10 resize-none font-medium"
            />
          </div>

          {/* Grid: Assignee, Priority, Due Date */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Assignee</label>
              <input
                type="text"
                placeholder="User email"
                value={assignee}
                onChange={e => setAssignee(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs text-slate-900 focus:outline-none focus:bg-white focus:border-indigo-600 font-medium"
              />
            </div>

            <div>
              <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Priority</label>
              <select
                value={priority}
                onChange={e => setPriority(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs text-slate-900 focus:outline-none focus:bg-white focus:border-indigo-600 font-medium"
              >
                {PRIORITIES.map(p => (
                  <option key={p} value={p}>{p || 'NONE'}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Due Date</label>
              <input
                type="date"
                value={dueDate}
                onChange={e => setDueDate(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs text-slate-900 focus:outline-none focus:bg-white focus:border-indigo-600 font-medium"
              />
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-6 border-t border-slate-100 flex items-center justify-between bg-slate-50/50">
          <button
            type="button"
            onClick={handleDelete}
            className="px-3.5 py-2 rounded-xl bg-rose-50 hover:bg-rose-100 border border-rose-200 text-rose-700 font-semibold text-xs transition-colors flex items-center gap-1.5"
          >
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            Delete Task
          </button>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              disabled={saving}
              className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-all shadow-xs disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
