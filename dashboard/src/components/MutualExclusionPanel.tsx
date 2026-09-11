import React from 'react';
import { ShieldCheck, CheckCircle, XCircle, Trash2, AlertTriangle, Lock } from 'lucide-react';
import { AlertEvent } from '../types';

interface MutualExclusionPanelProps {
  alerts: AlertEvent[];
  onClearAlerts: () => void;
}

const SeverityBadge: React.FC<{ severity: string }> = ({ severity }) => {
  const isCritical = severity === 'CRITICAL';
  return (
    <span className="badge" style={{
      background: isCritical ? 'rgba(225,29,72,0.1)' : 'rgba(217,119,6,0.1)',
      color: isCritical ? '#9f1239' : '#92400e',
      border: `1px solid ${isCritical ? 'rgba(225,29,72,0.25)' : 'rgba(217,119,6,0.25)'}`,
      fontSize: '0.62rem'
    }}>
      {isCritical ? <AlertTriangle size={10} /> : null}
      {severity}
    </span>
  );
};

export const MutualExclusionPanel: React.FC<MutualExclusionPanelProps> = ({ alerts, onClearAlerts }) => {
  const executedCount = alerts.filter(a => !a.isDeduplicated && a.status !== 'DEDUPLICATED_MUTEX').length;
  const dedupCount = alerts.filter(a => a.isDeduplicated || a.status === 'DEDUPLICATED_MUTEX').length;

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck size={20} color="var(--accent-indigo)" />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--text-main)' }}>
              Mutual Exclusion &amp; Alert Deduplication
            </h3>
            <span className="badge" style={{ background: 'rgba(99,102,241,0.1)', color: '#3730a3', border: '1px solid rgba(99,102,241,0.2)', fontSize: '0.65rem' }}>
              Algorithm 10.4
            </span>
          </div>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '3px', maxWidth: '520px' }}>
            When multiple sensors concurrently detect the same threshold, a distributed mutex ensures the dispatch action executes <strong>exactly once</strong>. Secondary attempts are suppressed (deduplicated).
          </p>
        </div>

        {/* Clear All Button */}
        {alerts.length > 0 && (
          <button
            onClick={onClearAlerts}
            className="btn-outline"
            style={{ flexShrink: 0, borderColor: '#e11d48', color: '#e11d48', gap: '5px' }}
            title="Remove all alerts from the list"
          >
            <Trash2 size={13} />
            Clear All ({alerts.length})
          </button>
        )}
      </div>

      {/* Mutex Explanation */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.65rem', marginBottom: '1rem' }}>
        <div style={{ background: 'rgba(5,150,105,0.07)', border: '1px solid rgba(5,150,105,0.2)', borderRadius: '10px', padding: '10px 12px', display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
          <Lock size={15} color="#059669" style={{ flexShrink: 0, marginTop: '1px' }} />
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#065f46', marginBottom: '2px' }}>Mutex Lock Granted</div>
            <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>First sensor to acquire the mutex gets to execute the action (e.g., dispatch alert).</div>
          </div>
        </div>
        <div style={{ background: 'rgba(100,116,139,0.07)', border: '1px solid rgba(100,116,139,0.2)', borderRadius: '10px', padding: '10px 12px', display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
          <XCircle size={15} color="#64748b" style={{ flexShrink: 0, marginTop: '1px' }} />
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#475569', marginBottom: '2px' }}>Mutex Suppressed</div>
            <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>Subsequent sensors detect the lock is held → their action is deduplicated (skipped).</div>
          </div>
        </div>
      </div>

      {/* Stats Bar */}
      {alerts.length > 0 && (
        <div style={{ display: 'flex', gap: '1rem', marginBottom: '0.85rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(5,150,105,0.08)', border: '1px solid rgba(5,150,105,0.2)', padding: '4px 12px', borderRadius: '8px', fontSize: '0.75rem', fontWeight: 600, color: '#065f46' }}>
            <CheckCircle size={13} color="#059669" />
            {executedCount} Executed
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(100,116,139,0.08)', border: '1px solid rgba(100,116,139,0.2)', padding: '4px 12px', borderRadius: '8px', fontSize: '0.75rem', fontWeight: 600, color: '#475569' }}>
            <XCircle size={13} color="#64748b" />
            {dedupCount} Deduplicated
          </div>
        </div>
      )}

      {/* Table */}
      <div style={{ overflowX: 'auto', borderRadius: '10px', border: '1px solid var(--border-card)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}>
          <thead>
            <tr style={{ background: 'var(--bg-subtle)', borderBottom: '1px solid var(--border-card)', textAlign: 'left' }}>
              {['Cluster', 'Source', 'Event Type', 'Severity', 'Mutex Outcome', 'Time'].map(h => (
                <th key={h} style={{ padding: '8px 12px', fontWeight: 600, fontSize: '0.68rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {alerts.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '1.75rem', color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                  No environmental alarms yet.<br />
                  <span style={{ fontSize: '0.72rem' }}>Click <strong>"Trigger Concurrent Fire Alert"</strong> above to test mutual exclusion!</span>
                </td>
              </tr>
            ) : (
              alerts.slice(0, 8).map((a, i) => {
                const isDedup = a.isDeduplicated || a.status === 'DEDUPLICATED_MUTEX';
                return (
                  <tr key={a.alertId} style={{ borderBottom: '1px solid var(--border-card)', background: i % 2 === 0 ? 'transparent' : 'var(--bg-subtle)' }}>
                    <td style={{ padding: '8px 12px' }}>
                      <span className="font-mono" style={{ color: 'var(--accent-indigo)', fontWeight: 600 }}>{a.clusterId}</span>
                    </td>
                    <td style={{ padding: '8px 12px' }}>
                      <span className="font-mono" style={{ fontWeight: 700, color: 'var(--text-main)', fontSize: '0.82rem' }}>{a.sourceSensor}</span>
                    </td>
                    <td style={{ padding: '8px 12px', color: 'var(--text-muted)', fontSize: '0.78rem' }}>{a.eventType}</td>
                    <td style={{ padding: '8px 12px' }}>
                      <SeverityBadge severity={a.severity} />
                    </td>
                    <td style={{ padding: '8px 12px' }}>
                      {isDedup ? (
                        <span className="badge" style={{ background: 'rgba(100,116,139,0.1)', color: '#475569', border: '1px solid rgba(100,116,139,0.25)', fontSize: '0.65rem' }}>
                          <XCircle size={11} /> Mutex Suppressed
                        </span>
                      ) : (
                        <span className="badge" style={{ background: 'rgba(5,150,105,0.1)', color: '#065f46', border: '1px solid rgba(5,150,105,0.25)', fontSize: '0.65rem' }}>
                          <CheckCircle size={11} /> Lock Granted ✓
                        </span>
                      )}
                    </td>
                    <td style={{ padding: '8px 12px', color: 'var(--text-dim)', fontFamily: 'JetBrains Mono, monospace', fontSize: '0.72rem' }}>
                      {new Date(a.eventTime).toLocaleTimeString()}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

    </div>
  );
};
