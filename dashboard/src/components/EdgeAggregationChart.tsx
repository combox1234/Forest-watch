import React from 'react';
import { BarChart3, Zap } from 'lucide-react';
import { ReadingSummary } from '../types';

interface EdgeAggregationChartProps {
  summaries: ReadingSummary[];
}

export const EdgeAggregationChart: React.FC<EdgeAggregationChartProps> = ({ summaries }) => {
  const totalSummaries = summaries.length;
  const totalRawSamples = summaries.reduce((acc, s) => acc + (s.sampleCount || 4), 0);
  const bandwidthSavedPct = totalRawSamples > 0
    ? Math.round(((totalRawSamples - totalSummaries) / totalRawSamples) * 100)
    : 75;

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '1.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BarChart3 size={20} color="var(--accent-emerald)" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-main)' }}>
              Edge Aggregation &amp; Upstream Traffic Savings
            </h3>
            <span className="badge" style={{ background: 'rgba(5,150,105,0.1)', color: '#065f46', border: '1px solid rgba(5,150,105,0.25)', fontSize: '0.65rem' }}>
              Algorithm 10.5
            </span>
          </div>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>
            Rather than flooding raw sensor streams upstream, elected edge coordinators calculate statistical summaries over sliding windows, dramatically reducing gRPC network utilization.
          </p>
        </div>

        {/* Savings Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'rgba(5,150,105,0.08)', border: '1px solid rgba(5,150,105,0.25)', padding: '6px 14px', borderRadius: '12px' }}>
          <Zap size={16} color="var(--accent-emerald)" />
          <span style={{ fontSize: '0.85rem', fontWeight: 800, color: '#065f46' }}>
            {bandwidthSavedPct}% Upstream Bandwidth Saved
          </span>
        </div>
      </div>

      {/* Visual Volume Comparison */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', alignItems: 'center' }}>

        {/* Raw Sensor Packets */}
        <div style={{ background: 'rgba(225,29,72,0.04)', padding: '1rem', borderRadius: '10px', border: '1px solid rgba(225,29,72,0.15)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>Raw Telemetry Generated at Edge</span>
            <span className="font-mono" style={{ fontSize: '1.1rem', fontWeight: 800, color: '#e11d48' }}>
              {totalRawSamples} packets
            </span>
          </div>
          <div style={{ width: '100%', height: '10px', background: '#fee2e2', borderRadius: '5px', overflow: 'hidden' }}>
            <div style={{ width: '100%', height: '100%', background: 'linear-gradient(to right, #f43f5e, #fb7185)' }} />
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '6px' }}>
            Direct transmission would cause network bottlenecks &amp; central saturation.
          </div>
        </div>

        {/* Compressed Summaries */}
        <div style={{ background: 'rgba(5,150,105,0.04)', padding: '1rem', borderRadius: '10px', border: '1px solid rgba(5,150,105,0.15)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>Forwarded Upstream via gRPC</span>
            <span className="font-mono" style={{ fontSize: '1.1rem', fontWeight: 800, color: '#059669' }}>
              {totalSummaries} summaries
            </span>
          </div>
          <div style={{ width: '100%', height: '10px', background: '#dcfce7', borderRadius: '5px', overflow: 'hidden' }}>
            <div style={{ width: `${Math.max(15, 100 - bandwidthSavedPct)}%`, height: '100%', background: 'linear-gradient(to right, #059669, #34d399)' }} />
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '6px' }}>
            Edge summaries include count, min, max, avg &amp; merged vector clocks.
          </div>
        </div>

      </div>
    </div>
  );
};
