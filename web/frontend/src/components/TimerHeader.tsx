import React, { useEffect, useState, useRef } from 'react';
import { Clock, AlertTriangle } from 'lucide-react';

interface TimerHeaderProps {
  initialRemainingSeconds: number;
  deadlineIso?: string;
  onTimeUp: () => void;
  examTitle: string;
  subjectName?: string;
}

export const TimerHeader: React.FC<TimerHeaderProps> = ({
  initialRemainingSeconds,
  deadlineIso: _deadlineIso,
  onTimeUp,
  examTitle,
  subjectName,
}) => {
  const [secondsLeft, setSecondsLeft] = useState<number>(initialRemainingSeconds);
  const timeUpFired = useRef<boolean>(false);
  const startPerfMsRef = useRef<number>(performance.now());
  const initialSecondsRef = useRef<number>(initialRemainingSeconds);

  useEffect(() => {
    // Synchronize baseline with server-provided remaining seconds
    startPerfMsRef.current = performance.now();
    initialSecondsRef.current = initialRemainingSeconds;
    timeUpFired.current = false;
    setSecondsLeft(initialRemainingSeconds);

    const interval = setInterval(() => {
      // Monotonic high-resolution timing: completely immune to client browser/system clock changes
      const elapsedSeconds = Math.floor((performance.now() - startPerfMsRef.current) / 1000);
      const remaining = Math.max(0, initialSecondsRef.current - elapsedSeconds);

      setSecondsLeft(remaining);

      if (remaining <= 0 && !timeUpFired.current) {
        timeUpFired.current = true;
        clearInterval(interval);
        onTimeUp();
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [initialRemainingSeconds, onTimeUp]);

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

  const isCritical = secondsLeft <= 60;
  const isWarning = secondsLeft <= 300 && !isCritical;

  let timerBg = 'linear-gradient(135deg, #064e3b, #065f46)';
  let timerBorder = '1px solid #059669';
  let timerColor = '#6ee7b7';

  if (isCritical) {
    timerBg = 'linear-gradient(135deg, #7f1d1d, #450a0a)';
    timerBorder = '2px solid #ef4444';
    timerColor = '#fca5a5';
  } else if (isWarning) {
    timerBg = 'linear-gradient(135deg, #78350f, #451a03)';
    timerBorder = '1px solid #f59e0b';
    timerColor = '#fcd34d';
  }

  return (
    <div style={{
      position: 'sticky',
      top: '68px',
      zIndex: 90,
      background: '#ffffff',
      borderBottom: '1px solid #d1fae5',
      boxShadow: '0 2px 4px rgba(0,0,0,0.08)',
      padding: '0.85rem 1.5rem',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
    }}>
      <div>
        <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#1f2937', lineHeight: 1.2 }}>
          {examTitle}
        </div>
        {subjectName && (
          <div style={{ fontSize: '0.78rem', color: '#64748b', fontWeight: 500 }}>
            Subject: {subjectName}
          </div>
        )}
      </div>

      {/* Countdown Timer Display */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
        padding: '0.5rem 1.25rem',
        borderRadius: '12px',
        background: timerBg,
        border: timerBorder,
        color: timerColor,
        boxShadow: isCritical ? '0 0 15px rgba(239, 68, 68, 0.4)' : '0 2px 5px rgba(0,0,0,0.1)',
        transition: 'all 0.3s ease',
      }}>
        {isCritical ? (
          <AlertTriangle size={22} color="#fca5a5" className="pulse-warning" />
        ) : (
          <Clock size={20} color={timerColor} />
        )}
        <div style={{ textAlign: 'right' }}>
          <div style={{
            fontSize: '1.45rem',
            fontFamily: 'monospace',
            fontWeight: 800,
            letterSpacing: '0.05em',
            lineHeight: 1,
          }}>
            {formattedTime}
          </div>
          <div style={{ fontSize: '0.65rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: timerColor, opacity: 0.85 }}>
            {isCritical ? 'Time Almost Up!' : isWarning ? 'Time Running Out' : 'Remaining Time'}
          </div>
        </div>
      </div>
    </div>
  );
};
