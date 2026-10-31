import React from 'react';
import { Flag } from 'lucide-react';
import type { AttemptQuestionView } from '../types';

interface QuestionPaletteProps {
  questions: AttemptQuestionView[];
  currentIndex: number;
  onSelectQuestion: (index: number) => void;
  onToggleFlag: (index: number) => void;
}

export const QuestionPalette: React.FC<QuestionPaletteProps> = ({
  questions,
  currentIndex,
  onSelectQuestion,
  onToggleFlag,
}) => {
  const answeredCount = questions.filter((q) => q.selectedOption != null).length;
  const flaggedCount = questions.filter((q) => q.isFlagged).length;
  const unansweredCount = questions.length - answeredCount;

  return (
    <div className="card" style={{ padding: '1.25rem', height: 'fit-content', backgroundColor: '#ffffff', border: '1px solid #d1fae5' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#1f2937' }}>
          Question Navigation
        </h4>
        <button
          type="button"
          onClick={() => onToggleFlag(currentIndex)}
          style={{
            background: questions[currentIndex]?.isFlagged ? '#fef3c7' : '#f9fafb',
            border: `1px solid ${questions[currentIndex]?.isFlagged ? '#f59e0b' : '#d1d5db'}`,
            color: questions[currentIndex]?.isFlagged ? '#b45309' : '#64748b',
            borderRadius: '6px',
            padding: '0.35rem 0.65rem',
            fontSize: '0.78rem',
            fontWeight: 600,
            display: 'flex',
            alignItems: 'center',
            gap: '0.35rem',
            cursor: 'pointer',
          }}
        >
          <Flag size={13} fill={questions[currentIndex]?.isFlagged ? '#f59e0b' : 'none'} color={questions[currentIndex]?.isFlagged ? '#b45309' : '#64748b'} />
          <span>{questions[currentIndex]?.isFlagged ? 'Flagged' : 'Flag for Review'}</span>
        </button>
      </div>

      {/* Progress status pills */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gap: '0.5rem',
        marginBottom: '1.25rem',
        fontSize: '0.75rem',
        textAlign: 'center',
      }}>
        <div style={{ background: '#ecfdf5', color: '#047857', padding: '0.4rem', borderRadius: '6px', border: '1px solid #a7f3d0' }}>
          <div style={{ fontWeight: 800, fontSize: '0.95rem' }}>{answeredCount}</div>
          <div>Answered</div>
        </div>
        <div style={{ background: '#fef3c7', color: '#b45309', padding: '0.4rem', borderRadius: '6px', border: '1px solid #fde68a' }}>
          <div style={{ fontWeight: 800, fontSize: '0.95rem' }}>{flaggedCount}</div>
          <div>Flagged</div>
        </div>
        <div style={{ background: '#f9fafb', color: '#475569', padding: '0.4rem', borderRadius: '6px', border: '1px solid #e5e7eb' }}>
          <div style={{ fontWeight: 800, fontSize: '0.95rem' }}>{unansweredCount}</div>
          <div>Unanswered</div>
        </div>
      </div>

      {/* Question palette grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(5, 1fr)',
        gap: '0.5rem',
      }}>
        {questions.map((q, idx) => {
          const isCurrent = idx === currentIndex;
          const isAnswered = q.selectedOption != null;
          const isFlagged = q.isFlagged;

          let btnBg = '#f9fafb';
          let textColor = '#374151';
          let borderColor = '#d1d5db';

          if (isFlagged) {
            btnBg = '#f59e0b';
            textColor = '#ffffff';
            borderColor = '#d97706';
          } else if (isAnswered) {
            btnBg = '#059669';
            textColor = '#ffffff';
            borderColor = '#047857';
          }

          return (
            <button
              key={q.questionId}
              type="button"
              onClick={() => onSelectQuestion(idx)}
              style={{
                height: '42px',
                borderRadius: '8px',
                backgroundColor: btnBg,
                color: textColor,
                border: isCurrent ? '2px solid #059669' : `1px solid ${borderColor}`,
                boxShadow: isCurrent ? '0 0 0 2px rgba(5, 150, 105, 0.3)' : 'none',
                fontWeight: 700,
                fontSize: '0.9rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                transition: 'all 0.15s ease',
              }}
            >
              {idx + 1}
              {isFlagged && (
                <div style={{
                  position: 'absolute',
                  top: '-4px',
                  right: '-4px',
                  width: '12px',
                  height: '12px',
                  borderRadius: '50%',
                  background: '#f59e0b',
                  border: '2px solid #fff',
                }} />
              )}
            </button>
          );
        })}
      </div>

      {/* Palette Legend */}
      <div style={{
        marginTop: '1.25rem',
        paddingTop: '0.85rem',
        borderTop: '1px solid #f1f5f9',
        fontSize: '0.72rem',
        color: '#64748b',
        display: 'flex',
        flexDirection: 'column',
        gap: '0.35rem',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: '#059669' }} />
          <span>Answered</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: '#f59e0b' }} />
          <span>Flagged for Review</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: '#f9fafb', border: '1px solid #d1d5db' }} />
          <span>Not Answered</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '12px', height: '12px', borderRadius: '3px', border: '2px solid #059669' }} />
          <span>Current Active Question</span>
        </div>
      </div>
    </div>
  );
};
