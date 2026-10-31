import React, { useState } from 'react';
import { Sparkles, X, Send, Bot, BookOpen, Cpu } from 'lucide-react';
import { aiApi } from '../services/api';
import type { AiChatMessage } from '../types';

interface AiStudyDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  contextTitle?: string;
  contextText?: string;
}

export const AiStudyDrawer: React.FC<AiStudyDrawerProps> = ({
  isOpen,
  onClose,
  contextTitle,
  contextText,
}) => {
  const [messages, setMessages] = useState<AiChatMessage[]>([
    {
      sender: 'assistant',
      text: `Hello! I'm your AI Study Assistant. ${
        contextTitle ? `I see you are studying "${contextTitle}". ` : ''
      }How can I help you understand this topic better today?`,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [engineInfo, setEngineInfo] = useState<string>('Ready');

  if (!isOpen) return null;

  const handleSend = async (messageText?: string) => {
    const textToSend = messageText || input;
    if (!textToSend.trim() || loading) return;

    const userMsg: AiChatMessage = {
      sender: 'user',
      text: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await aiApi.askStudyAssistant(
        textToSend,
        contextText ? `Context: ${contextTitle || 'Topic'} - ${contextText}` : undefined
      );

      const assistantMsg: AiChatMessage = {
        sender: 'assistant',
        text: res.reply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, assistantMsg]);
      setEngineInfo(`${res.provider} (${res.model})`);
    } catch {
      const errorMsg: AiChatMessage = {
        sender: 'assistant',
        text: 'Sorry, I encountered a temporary connection issue. Please try again shortly.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const quickPrompts = [
    'Explain key concepts in simple terms',
    'Summarize this into 3 bullet points',
    'Generate 2 practice quiz questions',
    'What are the common exam pitfalls?',
  ];

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      right: 0,
      width: '440px',
      maxWidth: '100vw',
      height: '100vh',
      backgroundColor: '#ffffff',
      boxShadow: '-8px 0 25px rgba(0,0,0,0.15)',
      zIndex: 1050,
      display: 'flex',
      flexDirection: 'column',
      animation: 'slideIn 0.25s ease-out',
    }}>
      <style>{`
        @keyframes slideIn {
          from { transform: translateX(100%); }
          to { transform: translateX(0); }
        }
      `}</style>

      {/* Header */}
      <div style={{
        background: 'linear-gradient(135deg, #064e3b, #047857)',
        color: '#fff',
        padding: '1.25rem 1.5rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid rgba(110,231,183,0.2)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            background: 'linear-gradient(135deg, #059669, #10b981)',
            padding: '0.5rem',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Sparkles size={20} color="#fff" />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '1.05rem', letterSpacing: '-0.01em', color: '#ecfdf5' }}>
              AI Study Assistant
            </div>
            <div style={{ fontSize: '0.72rem', color: '#a7f3d0', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              <Cpu size={12} />
              <span>{engineInfo}</span>
            </div>
          </div>
        </div>
        <button
          onClick={onClose}
          style={{
            background: 'transparent',
            border: 'none',
            color: '#a7f3d0',
            cursor: 'pointer',
            padding: '0.25rem',
            display: 'flex',
            alignItems: 'center',
          }}
        >
          <X size={22} />
        </button>
      </div>

      {/* Context pill if studying a specific topic */}
      {contextTitle && (
        <div style={{
          backgroundColor: '#f0fdf4',
          padding: '0.6rem 1.25rem',
          borderBottom: '1px solid #d1fae5',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          fontSize: '0.8rem',
          color: '#065f46'
        }}>
          <BookOpen size={14} color="#059669" />
          <span>Active Topic: <strong>{contextTitle}</strong></span>
        </div>
      )}

      {/* Quick Prompts Bar */}
      <div style={{
        padding: '0.75rem 1rem',
        background: '#f9fafb',
        borderBottom: '1px solid #e5e7eb',
        display: 'flex',
        gap: '0.5rem',
        overflowX: 'auto',
      }}>
        {quickPrompts.map((prompt, idx) => (
          <button
            key={idx}
            onClick={() => handleSend(prompt)}
            disabled={loading}
            style={{
              whiteSpace: 'nowrap',
              fontSize: '0.75rem',
              padding: '0.35rem 0.65rem',
              borderRadius: '9999px',
              backgroundColor: '#ffffff',
              color: '#1f2937',
              border: '1px solid #d1fae5',
              fontWeight: 500,
              cursor: 'pointer',
              transition: 'all 0.15s'
            }}
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Chat Messages Body */}
      <div style={{
        flex: 1,
        padding: '1.25rem',
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
        backgroundColor: '#fafafa',
      }}>
        {messages.map((msg, i) => (
          <div
            key={i}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: msg.sender === 'user' ? 'flex-end' : 'flex-start',
            }}
          >
            <div style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: '0.5rem',
              maxWidth: '88%',
              flexDirection: msg.sender === 'user' ? 'row-reverse' : 'row',
            }}>
              <div style={{
                width: '28px',
                height: '28px',
                borderRadius: '50%',
                backgroundColor: msg.sender === 'user' ? '#059669' : '#064e3b',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                fontSize: '0.75rem',
                fontWeight: 700
              }}>
                {msg.sender === 'user' ? 'You' : <Bot size={16} />}
              </div>
              <div style={{
                backgroundColor: msg.sender === 'user' ? '#059669' : '#ffffff',
                color: msg.sender === 'user' ? '#ffffff' : '#1f2937',
                padding: '0.85rem 1rem',
                borderRadius: '12px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                fontSize: '0.9rem',
                lineHeight: 1.5,
                border: msg.sender === 'assistant' ? '1px solid #e5e7eb' : 'none',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }}>
                {msg.text}
              </div>
            </div>
            <span style={{ fontSize: '0.7rem', color: '#94a3b8', marginTop: '0.25rem', padding: '0 2.25rem' }}>
              {msg.timestamp}
            </span>
          </div>
        ))}
        {loading && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#059669', fontSize: '0.85rem' }}>
            <div style={{
              width: '16px',
              height: '16px',
              border: '2px solid #a7f3d0',
              borderTopColor: '#059669',
              borderRadius: '50%',
              animation: 'spin 0.6s linear infinite'
            }} />
            <span>AI Assistant is generating explanation...</span>
          </div>
        )}
      </div>

      {/* Input Box */}
      <div style={{
        padding: '1rem',
        borderTop: '1px solid #e5e7eb',
        backgroundColor: '#ffffff',
      }}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          style={{ display: 'flex', gap: '0.5rem' }}
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask a question or request concept help..."
            disabled={loading}
            style={{
              flex: 1,
              padding: '0.65rem 0.85rem',
              borderRadius: '8px',
              border: '1px solid #d1d5db',
              fontSize: '0.9rem',
              outline: 'none',
              color: '#1f2937',
            }}
          />
          <button
            type="submit"
            disabled={loading || !input.trim()}
            style={{
              backgroundColor: '#059669',
              color: '#ffffff',
              border: 'none',
              borderRadius: '8px',
              padding: '0.65rem 1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
              opacity: loading || !input.trim() ? 0.6 : 1,
            }}
          >
            <Send size={16} />
          </button>
        </form>
      </div>
    </div>
  );
};
