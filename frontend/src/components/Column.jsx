import React from 'react';
import Card from './Card';

const COLUMN_THEMES = {
  'To Do': { border: 'border-slate-300', accent: 'bg-slate-500', badge: 'bg-slate-100 text-slate-700 border-slate-200' },
  'In Progress': { border: 'border-indigo-200', accent: 'bg-indigo-600', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  'Review': { border: 'border-amber-200', accent: 'bg-amber-500', badge: 'bg-amber-50 text-amber-800 border-amber-200' },
  'Done': { border: 'border-emerald-200', accent: 'bg-emerald-600', badge: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
};

export default function Column({ column, view, onDragStart, onDrop, onCardClick,
                                 newCardTitle, onNewCardTitleChange, onCreateCard }) {
  const theme = COLUMN_THEMES[column.name] || { border: 'border-slate-200', accent: 'bg-indigo-600', badge: 'bg-indigo-50 text-indigo-700 border-indigo-200' };
  const isList = view === 'list';

  const handleDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add('column-drop-target');
  };

  const handleDragLeave = (e) => {
    e.currentTarget.classList.remove('column-drop-target');
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.currentTarget.classList.remove('column-drop-target');
    if (column.id) {
      onDrop(column.id);
    }
  };

  return (
    <div
      className={`flex flex-col bg-white rounded-2xl border ${theme.border} shadow-sm ${
        isList ? 'min-w-[380px] flex-1 max-w-4xl mx-auto' : 'min-w-[300px] max-w-[340px]'
      }`}
    >
      {/* Column Header */}
      <div className="px-4 py-3.5 flex items-center justify-between border-b border-slate-100">
        <div className="flex items-center gap-2.5">
          <span className={`w-2.5 h-2.5 rounded-full ${theme.accent}`} />
          <h3 className="font-heading font-bold text-slate-900 text-sm tracking-tight">
            {column.name}
          </h3>
        </div>
        <span className={`text-xs font-mono font-semibold px-2.5 py-0.5 rounded-full border ${theme.badge}`}>
          {column.cards?.length || 0}
        </span>
      </div>

      {/* Card Container */}
      <div
        className="flex-1 px-3 py-3 overflow-y-auto"
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <div className="space-y-3 min-h-[80px]">
          {column.cards && column.cards.map(card => (
            <Card
              key={card.id}
              card={card}
              onDragStart={onDragStart}
              onClick={() => onCardClick(card)}
            />
          ))}
        </div>
      </div>

      {/* Inline Card Creation */}
      {column.id && (
        <div className="p-3 border-t border-slate-100 bg-slate-50/50 rounded-b-2xl">
          <form
            onSubmit={e => {
              e.preventDefault();
              onCreateCard();
            }}
            className="flex items-center gap-2"
          >
            <input
              type="text"
              placeholder="+ Add task..."
              value={newCardTitle}
              onChange={e => onNewCardTitleChange(e.target.value)}
              className="flex-1 bg-white border border-slate-200 rounded-xl px-3 py-1.5 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-600 focus:ring-1 focus:ring-indigo-500/20 font-medium"
            />
            <button
              type="submit"
              disabled={!newCardTitle.trim()}
              className="p-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 text-white transition-all shadow-xs"
              title="Add Card"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M12 4v16m8-8H4" />
              </svg>
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
