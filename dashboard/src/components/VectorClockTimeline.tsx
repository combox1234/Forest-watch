import React, { useMemo } from 'react';
import { Clock, ArrowRight, Info, TrendingUp } from 'lucide-react';
import { ReadingSummary } from '../types';

interface VectorClockTimelineProps {
  summaries: ReadingSummary[];
}

/** Compare two vector clocks: returns 'before' | 'after' | 'concurrent' | 'equal' */
function compareClock(
  a: Record<string, number>,
  b: Record<string, number>
): 'before' | 'after' | 'concurrent' | 'equal' {
  const allKeys = new Set([...Object.keys(a), ...Object.keys(b)]);
  let aLessOrEq = true;
  let bLessOrEq = true;

  for (const k of allKeys) {
    const av = a[k] ?? 0;
    const bv = b[k] ?? 0;
    if (av > bv) bLessOrEq = false;
    if (bv > av) aLessOrEq = false;
  }

  if (aLessOrEq && bLessOrEq) return 'equal';
  if (aLessOrEq) return 'before';
  if (bLessOrEq) return 'after';
  return 'concurrent';
}

function parseVC(s: ReadingSummary): Record<string, number> {
  if (s.vectorClock) return s.vectorClock as Record<string, number>;
  if (s.vectorClockJson) {
    try { return JSON.parse(s.vectorClockJson); } catch { return {}; }
  }
  return {};
}

function maxVC(vcs: Record<string, number>[]): Record<string, number> {
  const out: Record<string, number> = {};
  for (const vc of vcs) {
    for (const [k, v] of Object.entries(vc)) {
      out[k] = Math.max(out[k] ?? 0, v);
    }
  }
  return out;
}

const EVENT_COLORS: Record<string, { bg: string; border: string; text: string; dot: string }> = {
  'cluster-1': { bg: 'rgba(99,102,241,0.08)', border: 'rgba(99,102,241,0.3)', text: '#3730a3', dot: '#6366f1' },
  'cluster-2': { bg: 'rgba(5,150,105,0.08)',  border: 'rgba(5,150,105,0.25)',  text: '#065f46',  dot: '#059669' },
  'cluster-3': { bg: 'rgba(234,88,12,0.08)',  border: 'rgba(234,88,12,0.25)',  text: '#7c2d12',  dot: '#ea580c' },
};

function clusterStyle(clusterId: string) {
  return EVENT_COLORS[clusterId] ?? {
    bg: 'rgba(100,116,139,0.08)', border: 'rgba(100,116,139,0.25)', text: '#1e293b', dot: '#64748b'
  };
}

const CausalBadge: React.FC<{ relation: ReturnType<typeof compareClock> }> = ({ relation }) => {
  const map = {
    before:     { icon: '→',  label: 'A happened-before B',  bg: '#dbeafe', color: '#1d4ed8', border: '#93c5fd' },
    after:      { icon: '←',  label: 'B happened-before A',  bg: '#ede9fe', color: '#6d28d9', border: '#c4b5fd' },
    concurrent: { icon: '‖',  label: 'Concurrent (no causal order)', bg: '#fef3c7', color: '#b45309', border: '#fcd34d' },
    equal:      { icon: '=',  label: 'Identical clocks',      bg: '#dcfce7', color: '#065f46', border: '#86efac' },
  };
  const m = map[relation];
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: m.bg, color: m.color, border: `1px solid ${m.border}`, padding: '2px 8px', borderRadius: '6px', fontSize: '0.72rem', fontWeight: 600 }}>
      <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{m.icon}</span>
      {m.label}
    </span>
  );
};

export const VectorClockTimeline: React.FC<VectorClockTimelineProps> = ({ summaries }) => {
  const recentSummaries = summaries.slice(0, 12);

  /* Determine all node/cluster IDs that appeared in vector clocks */
  const allNodes = useMemo(() => {
    const keys = new Set<string>();
    recentSummaries.forEach(s => {
      Object.keys(parseVC(s)).forEach(k => keys.add(k));
    });
    return Array.from(keys).sort();
  }, [recentSummaries]);

  /* For the "global state" row: take max across all seen VCs */
  const globalVC = useMemo(() => maxVC(recentSummaries.map(parseVC)), [recentSummaries]);

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={20} color="var(--accent-indigo)" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-main)' }}>
              Vector Clock &amp; Causal Timeline
            </h3>
            <span className="badge" style={{ background: 'rgba(99,102,241,0.1)', color: '#3730a3', border: '1px solid rgba(99,102,241,0.25)', fontSize: '0.65rem' }}>
              Algorithm 10.2
            </span>
          </div>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '3px', maxWidth: '640px' }}>
            Each cluster head maintains a vector clock <code style={{ background: '#f1f5f9', padding: '1px 4px', borderRadius: '4px', fontSize: '0.75rem' }}>V[i]</code>.
            On send: <code style={{ background: '#f1f5f9', padding: '1px 4px', borderRadius: '4px', fontSize: '0.75rem' }}>V[i]++</code>.
            On receive: <code style={{ background: '#f1f5f9', padding: '1px 4px', borderRadius: '4px', fontSize: '0.75rem' }}>V[k] = max(V[k], M[k])</code> for each k.
            This allows the Aggregator to detect <strong>happens-before (→)</strong> vs <strong>concurrent (‖)</strong> events.
          </p>
        </div>
      </div>

      {/* ── Rule Explainer Cards ── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '0.75rem', marginBottom: '1.1rem' }}>
        {[
          {
            icon: '🔢', color: '#6366f1', bg: 'rgba(99,102,241,0.07)', border: 'rgba(99,102,241,0.2)',
            title: 'Local Event',
            rule: 'V[i] = V[i] + 1',
            desc: 'Every internal event or send ticks up the local component.'
          },
          {
            icon: '📨', color: '#0891b2', bg: 'rgba(8,145,178,0.07)', border: 'rgba(8,145,178,0.2)',
            title: 'Message Receive',
            rule: 'V[k] = max(V[k], M[k])',
            desc: 'Merge the received clock M with own clock V element-wise.'
          },
          {
            icon: '⚖️', color: '#d97706', bg: 'rgba(217,119,6,0.07)', border: 'rgba(217,119,6,0.2)',
            title: 'Causal Order (A → B)',
            rule: '∀k: V_A[k] ≤ V_B[k]',
            desc: 'A happened-before B when every component of V_A ≤ V_B.'
          },
          {
            icon: '🔀', color: '#7c3aed', bg: 'rgba(124,58,237,0.07)', border: 'rgba(124,58,237,0.2)',
            title: 'Concurrent (A ‖ B)',
            rule: 'Neither A→B nor B→A',
            desc: 'At least one component of V_A > V_B and one < V_B.'
          }
        ].map(card => (
          <div key={card.title} style={{ background: card.bg, border: `1px solid ${card.border}`, borderRadius: '10px', padding: '0.85rem' }}>
            <div style={{ fontSize: '1.1rem', marginBottom: '4px' }}>{card.icon}</div>
            <div style={{ fontSize: '0.78rem', fontWeight: 700, color: card.color, marginBottom: '2px' }}>{card.title}</div>
            <code style={{ fontSize: '0.7rem', fontFamily: 'JetBrains Mono, monospace', color: '#1e293b', display: 'block', marginBottom: '4px' }}>{card.rule}</code>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{card.desc}</div>
          </div>
        ))}
      </div>

      {/* ── Global Clock Summary ── */}
      {allNodes.length > 0 && (
        <div style={{ background: 'linear-gradient(135deg,rgba(99,102,241,0.06),rgba(8,145,178,0.06))', border: '1px solid rgba(99,102,241,0.18)', borderRadius: '10px', padding: '10px 14px', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <TrendingUp size={14} color="#6366f1" />
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#3730a3', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Aggregated Global Clock</span>
          </div>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            {Object.entries(globalVC).map(([node, tick]) => (
              <span key={node} className="vc-pill">
                <span style={{ color: 'var(--text-dim)' }}>{node}:</span>
                <strong style={{ color: '#1e40af' }}>{tick}</strong>
              </span>
            ))}
          </div>
        </div>
      )}

      {/* ── Timeline Stream ── */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem', maxHeight: '420px', overflowY: 'auto', paddingRight: '4px' }}>
        {recentSummaries.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-dim)', fontSize: '0.82rem', background: 'var(--bg-subtle)', borderRadius: '10px' }}>
            <Clock size={28} color="var(--text-dim)" style={{ margin: '0 auto 8px', display: 'block' }} />
            Awaiting edge telemetry stream…<br />
            <span style={{ fontSize: '0.72rem' }}>Vector clocks will appear here as cluster heads submit summaries.</span>
          </div>
        ) : (
          recentSummaries.map((s, idx) => {
            const vc = parseVC(s);
            const vcEntries = Object.entries(vc);
            const style = clusterStyle(s.clusterId);

            /* Compare against previous event */
            const prevVC = idx < recentSummaries.length - 1 ? parseVC(recentSummaries[idx + 1]) : null;
            const relation = prevVC && vcEntries.length > 0 ? compareClock(vc, prevVC) : null;

            return (
              <div key={s.readingId || idx} className="float-in" style={{
                background: style.bg,
                border: `1px solid ${style.border}`,
                borderRadius: '12px',
                padding: '10px 14px',
              }}>
                {/* Row 1: origin + timestamp */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px', flexWrap: 'wrap', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    {/* Colored dot */}
                    <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: style.dot, display: 'inline-block', flexShrink: 0 }} />
                    <div>
                      <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '0.88rem', fontWeight: 700, color: style.text }}>
                        {s.clusterId}
                      </span>
                      <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginLeft: '6px' }}>
                        via Leader <strong style={{ color: style.text }}>{s.leaderId}</strong>
                      </span>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)', fontFamily: 'JetBrains Mono, monospace' }}>
                    {new Date(s.timestamp).toLocaleTimeString()}
                  </span>
                </div>

                {/* Row 2: Data summary */}
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: '8px' }}>
                  {s.sampleCount} samples aggregated &nbsp;·&nbsp; Avg {s.avgTemperature}°C &nbsp;·&nbsp; {s.avgHumidity}% RH
                </div>

                {/* Row 3: Vector Clock display */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap', marginBottom: relation ? '8px' : 0 }}>
                  <span style={{ fontSize: '0.68rem', fontWeight: 600, color: 'var(--text-dim)', marginRight: '2px' }}>
                    V:
                  </span>
                  {vcEntries.length > 0 ? (
                    vcEntries.map(([node, tick], ki) => (
                      <React.Fragment key={node}>
                        <span className="vc-pill" style={{ background: style.bg, border: `1px solid ${style.border}`, color: style.text }}>
                          <span style={{ opacity: 0.7 }}>{node}</span>
                          <strong>{tick}</strong>
                        </span>
                        {ki < vcEntries.length - 1 && (
                          <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>,</span>
                        )}
                      </React.Fragment>
                    ))
                  ) : (
                    <span className="font-mono" style={{ fontSize: '0.7rem', color: 'var(--text-dim)' }}>[initial — no prior events]</span>
                  )}
                </div>

                {/* Row 4: Causal relation with previous event */}
                {relation && vcEntries.length > 0 && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '6px', paddingTop: '6px', borderTop: `1px dashed ${style.border}` }}>
                    <Info size={11} color="var(--text-dim)" />
                    <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>vs previous event:</span>
                    <CausalBadge relation={relation} />
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {recentSummaries.length > 0 && (
        <div style={{ marginTop: '10px', fontSize: '0.7rem', color: 'var(--text-dim)', textAlign: 'center' }}>
          Showing latest {recentSummaries.length} events · Causal relation shown relative to previous event in stream
        </div>
      )}
    </div>
  );
};
