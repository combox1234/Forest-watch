import React from 'react';
import { Crown, CheckCircle2, Clock, ArrowRight, Zap, Radio, AlertTriangle } from 'lucide-react';
import { ElectionRecord } from '../types';

interface BullyElectionPanelProps {
  elections: ElectionRecord[];
}

const BULLY_STEPS = [
  {
    step: 1,
    icon: <AlertTriangle size={14} />,
    title: 'Failure Detected',
    detail: 'A node notices the current leader stopped sending heartbeats for 3 consecutive intervals (≈ 3 × 1 s). It marks the leader as SUSPECTED.',
    color: '#e11d48',
    bg: 'rgba(225,29,72,0.08)',
    border: 'rgba(225,29,72,0.2)',
  },
  {
    step: 2,
    icon: <Radio size={14} />,
    title: 'ELECTION Broadcast',
    detail: 'The detecting node sends an ELECTION message to every peer with a higher sensor ID in the same cluster.',
    color: '#d97706',
    bg: 'rgba(217,119,6,0.08)',
    border: 'rgba(217,119,6,0.2)',
  },
  {
    step: 3,
    icon: <CheckCircle2 size={14} />,
    title: 'OK / Timeout',
    detail: 'Any higher-priority peer that is alive replies OK and takes over the election. If no OK arrives within 1 s, the initiator declares itself winner.',
    color: '#0891b2',
    bg: 'rgba(8,145,178,0.08)',
    border: 'rgba(8,145,178,0.2)',
  },
  {
    step: 4,
    icon: <Crown size={14} />,
    title: 'COORDINATOR Elected',
    detail: 'The winner — always the highest-ID surviving node — broadcasts a COORDINATOR message to all cluster members and updates the gRPC Aggregator.',
    color: '#059669',
    bg: 'rgba(5,150,105,0.08)',
    border: 'rgba(5,150,105,0.2)',
  },
];

const ReasonBadge: React.FC<{ reason: string }> = ({ reason }) => {
  const lower = (reason || '').toLowerCase();
  let color = '#475569';
  let bg = '#f1f5f9';
  let border = '#e2e8f0';
  if (lower.includes('fail') || lower.includes('crash') || lower.includes('timeout')) {
    color = '#9f1239'; bg = '#fff1f2'; border = '#fecdd3';
  } else if (lower.includes('startup') || lower.includes('init')) {
    color = '#1d4ed8'; bg = '#eff6ff'; border = '#bfdbfe';
  } else if (lower.includes('recover') || lower.includes('rejoin')) {
    color = '#065f46'; bg = '#f0fdf4'; border = '#bbf7d0';
  }
  return (
    <span style={{ background: bg, border: `1px solid ${border}`, color, borderRadius: '6px', padding: '2px 8px', fontSize: '0.7rem', fontWeight: 500 }}>
      {reason || '—'}
    </span>
  );
};

export const BullyElectionPanel: React.FC<BullyElectionPanelProps> = ({ elections }) => {
  const latestByCluster: Record<string, ElectionRecord> = {};
  elections.forEach(el => {
    if (!latestByCluster[el.clusterId]) latestByCluster[el.clusterId] = el;
  });

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
        <Crown size={20} color="#b45309" />
        <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-main)' }}>
          Bully Leader Election Monitor
        </h3>
        <span className="badge badge-leader" style={{ fontSize: '0.65rem' }}>Algorithm 10.1</span>
      </div>
      <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '1.1rem' }}>
        When a coordinator fails, surviving nodes use their <strong>sensor ID as priority</strong>. The highest-ID alive node always wins —  it "bullies" every lower-priority peer out of contention.
      </p>

      {/* ─── How a New Leader Is Selected ─── */}
      <div style={{ background: 'linear-gradient(135deg,rgba(99,102,241,0.05),rgba(5,150,105,0.04))', border: '1px solid rgba(99,102,241,0.15)', borderRadius: '12px', padding: '1rem', marginBottom: '1.1rem' }}>
        <div style={{ fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.07em', textTransform: 'uppercase', color: 'var(--accent-indigo)', marginBottom: '0.85rem' }}>
          How a New Leader Is Selected
        </div>

        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', flexWrap: 'wrap' }}>
          {BULLY_STEPS.map((s, i) => (
            <React.Fragment key={s.step}>
              <div style={{ flex: '1 1 160px', background: s.bg, border: `1px solid ${s.border}`, borderRadius: '10px', padding: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '5px' }}>
                  <span style={{
                    width: '22px', height: '22px', borderRadius: '50%', background: s.color,
                    color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '0.72rem', fontWeight: 800, flexShrink: 0
                  }}>{s.step}</span>
                  <span style={{ color: s.color, display: 'flex' }}>{s.icon}</span>
                  <strong style={{ fontSize: '0.78rem', color: 'var(--text-main)' }}>{s.title}</strong>
                </div>
                <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>{s.detail}</p>
              </div>
              {i < BULLY_STEPS.length - 1 && (
                <div style={{ display: 'flex', alignItems: 'center', paddingTop: '1.5rem', color: 'var(--text-dim)' }}>
                  <ArrowRight size={16} />
                </div>
              )}
            </React.Fragment>
          ))}
        </div>

        {/* Priority Rule */}
        <div style={{ marginTop: '0.85rem', padding: '8px 12px', background: 'rgba(217,119,6,0.08)', border: '1px solid rgba(217,119,6,0.2)', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Zap size={14} color="#d97706" />
          <span style={{ fontSize: '0.73rem', color: '#78350f' }}>
            <strong>Priority Rule:</strong> New leader = <code style={{ background: 'rgba(217,119,6,0.1)', padding: '1px 5px', borderRadius: '4px', fontFamily: 'JetBrains Mono, monospace' }}>max(sensorId)</code> among all <em>alive</em> nodes in the cluster.
            If leader S8 crashes → S7 wins (if alive). If S7 also crashes → S6 wins. Etc.
          </span>
        </div>
      </div>

      {/* ─── Current Leaders per Cluster ─── */}
      {Object.keys(latestByCluster).length > 0 && (
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
          {Object.entries(latestByCluster).map(([clusterId, el]) => (
            <div key={clusterId} style={{ flex: '1 1 160px', background: 'rgba(217,119,6,0.07)', border: '1px solid rgba(217,119,6,0.22)', borderRadius: '10px', padding: '10px 14px' }}>
              <div style={{ fontSize: '0.65rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '3px' }}>{clusterId}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Crown size={14} color="#b45309" />
                <span className="font-mono" style={{ fontWeight: 800, fontSize: '1rem', color: '#78350f' }}>{el.electedLeader}</span>
              </div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: '2px' }}>elected {new Date(el.startedAt).toLocaleTimeString()}</div>
            </div>
          ))}
        </div>
      )}

      {/* ─── Elections History Table ─── */}
      <div style={{ fontSize: '0.72rem', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: 'var(--text-dim)', marginBottom: '0.5rem' }}>
        Election History
      </div>
      <div style={{ overflowX: 'auto', borderRadius: '10px', border: '1px solid var(--border-card)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}>
          <thead>
            <tr style={{ background: 'var(--bg-subtle)', borderBottom: '1px solid var(--border-card)', textAlign: 'left' }}>
              {['Cluster', 'Initiator', 'New Leader', 'Trigger Reason', 'Duration', 'Time'].map(h => (
                <th key={h} style={{ padding: '8px 12px', fontWeight: 600, fontSize: '0.72rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {elections.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '1.75rem', color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                  No elections yet — initial leaders are active.<br />
                  <span style={{ fontSize: '0.72rem' }}>Use <strong>"Crash"</strong> on a leader node above to trigger a Bully re-election!</span>
                </td>
              </tr>
            ) : (
              elections.slice(0, 8).map((el, i) => (
                <tr key={el.electionId} style={{ borderBottom: '1px solid var(--border-card)', background: i % 2 === 0 ? 'transparent' : 'var(--bg-subtle)' }}>
                  <td style={{ padding: '8px 12px' }}>
                    <span className="font-mono" style={{ color: 'var(--accent-indigo)', fontWeight: 600 }}>{el.clusterId}</span>
                  </td>
                  <td style={{ padding: '8px 12px' }}>
                    <span className="font-mono" style={{ color: 'var(--text-muted)' }}>{el.initiator}</span>
                  </td>
                  <td style={{ padding: '8px 12px' }}>
                    <span className="badge badge-leader"><Crown size={11} />{el.electedLeader}</span>
                  </td>
                  <td style={{ padding: '8px 12px' }}>
                    <ReasonBadge reason={el.reason} />
                  </td>
                  <td style={{ padding: '8px 12px' }}>
                    <span className="font-mono" style={{ color: '#059669', fontWeight: 600 }}>{el.durationMs || 250}ms</span>
                  </td>
                  <td style={{ padding: '8px 12px', color: 'var(--text-dim)', fontFamily: 'JetBrains Mono, monospace', fontSize: '0.72rem' }}>
                    {new Date(el.startedAt).toLocaleTimeString()}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
