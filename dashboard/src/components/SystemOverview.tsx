import React from 'react';
import { Cpu, Crown, BarChart3, AlertTriangle, RefreshCw, ShieldCheck } from 'lucide-react';
import { SystemStats } from '../types';

interface SystemOverviewProps {
  stats: SystemStats;
  totalClusters: number;
  activeLeaders: number;
}

export const SystemOverview: React.FC<SystemOverviewProps> = ({ stats, totalClusters, activeLeaders }) => {
  const cards = [
    {
      title: 'Sensor Health',
      value: `${stats.healthyNodes} / ${stats.totalNodes}`,
      sub: `${stats.failedNodes} failed · ${stats.suspectedNodes} suspected`,
      icon: Cpu,
      color: 'var(--accent-blue)',
      accent: 'rgba(59,130,246,0.08)',
      border: 'rgba(59,130,246,0.2)',
      leftBar: '#3b82f6'
    },
    {
      title: 'Cluster Leaders',
      value: `${activeLeaders} / ${totalClusters}`,
      sub: 'Elected via Bully Algorithm',
      icon: Crown,
      color: 'var(--accent-amber)',
      accent: 'rgba(217,119,6,0.08)',
      border: 'rgba(217,119,6,0.2)',
      leftBar: '#d97706'
    },
    {
      title: 'Edge Summaries',
      value: stats.totalSummaries,
      sub: 'gRPC ingestion to Aggregator',
      icon: BarChart3,
      color: 'var(--accent-emerald)',
      accent: 'rgba(5,150,105,0.08)',
      border: 'rgba(5,150,105,0.2)',
      leftBar: '#059669'
    },
    {
      title: 'Env. Alerts',
      value: stats.totalAlerts,
      sub: 'Wildfire, Flood & Intrusion',
      icon: AlertTriangle,
      color: stats.totalAlerts > 0 ? 'var(--accent-rose)' : 'var(--text-dim)',
      accent: stats.totalAlerts > 0 ? 'rgba(225,29,72,0.08)' : 'rgba(100,116,139,0.06)',
      border: stats.totalAlerts > 0 ? 'rgba(225,29,72,0.2)' : 'var(--border-card)',
      leftBar: stats.totalAlerts > 0 ? '#e11d48' : '#94a3b8'
    },
    {
      title: 'Elections',
      value: stats.totalElections,
      sub: 'Automatic fault recovery',
      icon: RefreshCw,
      color: 'var(--accent-purple)',
      accent: 'rgba(124,58,237,0.08)',
      border: 'rgba(124,58,237,0.2)',
      leftBar: '#7c3aed'
    },
    {
      title: 'Mutex State',
      value: 'Active',
      sub: 'Duplicate alerts deduplicated',
      icon: ShieldCheck,
      color: 'var(--accent-teal)',
      accent: 'rgba(13,148,136,0.08)',
      border: 'rgba(13,148,136,0.2)',
      leftBar: '#0d9488'
    }
  ];

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(195px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
      {cards.map((c, i) => {
        const Icon = c.icon;
        return (
          <div key={i} className="stat-card" style={{ borderLeft: `4px solid ${c.leftBar}`, background: `radial-gradient(circle at top right, ${c.accent}, transparent 70%), var(--bg-card)` }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.6rem' }}>
              <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                {c.title}
              </span>
              <div style={{ padding: '6px', borderRadius: '8px', background: c.accent, border: `1px solid ${c.border}` }}>
                <Icon size={16} color={c.color} />
              </div>
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', lineHeight: 1.1 }}>
              {c.value}
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
              {c.sub}
            </div>
          </div>
        );
      })}
    </div>
  );
};
