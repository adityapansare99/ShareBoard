import React from 'react';

const PRIORITY_BADGES = {
  HIGH: 'bg-rose-50 text-rose-700 border-rose-200',
  MEDIUM: 'bg-amber-50 text-amber-800 border-amber-200',
  LOW: 'bg-emerald-50 text-emerald-700 border-emerald-200',
};

const STATE_BADGES = {
  IN_PROGRESS: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  REVIEW: 'bg-amber-50 text-amber-800 border-amber-200',
  DONE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
};

export default function Card({ card, onDragStart, onClick }) {
  const priorityClass = PRIORITY_BADGES[card.priority] || null;
  const stateClass = STATE_BADGES[card.state] || null;

  const handleDragStart = (e) => {
    e.dataTransfer.setData('text/plain', card.id.toString());
    e.currentTarget.classList.add('opacity-50', 'scale-95');
    onDragStart(card);
  };

  const handleDragEnd = (e) => {
    e.currentTarget.classList.remove('opacity-50', 'scale-95');
  };

  const isOverdue = card.dueDate && new Date(card.dueDate) < new Date() && card.state !== 'DONE';

  return (
    <div
      draggable
      onClick={onClick}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      className="group relative bg-white border border-slate-200 hover:border-indigo-300 rounded-xl p-3.5 cursor-pointer shadow-xs hover:shadow-md transition-all duration-200 hover:-translate-y-0.5 active:scale-[0.99] active:cursor-grabbing"
    >
      {/* Top Badge Row */}
      <div className="flex items-center justify-between gap-2 mb-2">
        {card.priority ? (
          <span className={`text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 rounded-md border ${priorityClass}`}>
            {card.priority}
          </span>
        ) : <div />}

        {card.state && card.state !== 'TODO' && (
          <span className={`text-[10px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded-md border ${stateClass}`}>
            {card.state.replace('_', ' ')}
          </span>
        )}
      </div>

      {/* Card Title */}
      <h4 className="text-xs font-bold text-slate-900 group-hover:text-indigo-600 transition-colors leading-snug">
        {card.title}
      </h4>

      {/* Description Preview */}
      {card.description && (
        <p className="text-[11px] text-slate-500 line-clamp-2 mt-1 font-normal leading-relaxed">
          {card.description}
        </p>
      )}

      {/* Footer Info: Assignee & Due Date */}
      {(card.assignee || card.dueDate) && (
        <div className="flex items-center justify-between gap-2 mt-3 pt-2.5 border-t border-slate-100 text-[10px] text-slate-500">
          {card.assignee ? (
            <div className="flex items-center gap-1">
              <span className="w-4 h-4 rounded-full bg-indigo-100 border border-indigo-200 text-indigo-700 flex items-center justify-center text-[9px] font-bold uppercase shrink-0">
                {card.assignee.charAt(0)}
              </span>
              <span className="truncate max-w-[90px] font-medium">{card.assignee}</span>
            </div>
          ) : <div />}

          {card.dueDate && (
            <div className={`flex items-center gap-1 px-1.5 py-0.5 rounded font-mono font-medium ${
              isOverdue ? 'bg-rose-50 text-rose-700 border border-rose-200' : 'bg-slate-50 text-slate-600 border border-slate-200'
            }`}>
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span>{new Date(card.dueDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
