import React from 'react';
import { Radio, Cpu, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  wsConnected: boolean;
  activeNodes: number;
  totalNodes: number;
  totalClusters: number;
}

export const Header: React.FC<HeaderProps> = ({
  wsConnected, activeNodes, totalNodes, totalClusters
}) => {
  return (
    <header className="glass-panel" style={{ padding: '1.25rem 1.75rem', marginBottom: '1.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>

        {/* Title & Branding */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '12px',
            background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-blue))',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 4px 16px var(--glow-indigo)'
          }}>
            <Radio size={26} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: '1.35rem', fontWeight: 800, letterSpacing: '-0.02em', color: 'var(--text-main)' }}>
                Distributed Environmental Sensor Network
              </h1>
              <span className="badge" style={{ background: 'rgba(99,102,241,0.1)', color: '#3730a3', border: '1px solid rgba(99,102,241,0.25)' }}>
                B.Tech DS Mini Project
              </span>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '2px' }}>
              Edge Aggregation &nbsp;·&nbsp; Bully Leader Election &nbsp;·&nbsp; Vector Clocks &nbsp;·&nbsp; Mutual Exclusion &nbsp;·&nbsp; Fault Tolerance
            </p>
          </div>
        </div>

        {/* Live Network Indicators */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>

          {/* WebSocket Status */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: '6px',
            background: wsConnected ? 'rgba(5,150,105,0.08)' : 'rgba(225,29,72,0.08)',
            padding: '6px 12px', borderRadius: '8px',
            border: `1px solid ${wsConnected ? 'rgba(5,150,105,0.25)' : 'rgba(225,29,72,0.25)'}`
          }}>
            <span style={{
              width: '8px', height: '8px', borderRadius: '50%', flexShrink: 0,
              backgroundColor: wsConnected ? '#059669' : '#e11d48',
              boxShadow: wsConnected ? '0 0 6px #059669' : '0 0 6px #e11d48'
            }} className="pulse-dot" />
            <span style={{ fontSize: '0.75rem', fontWeight: 600, color: wsConnected ? '#065f46' : '#9f1239' }}>
              {wsConnected ? 'LIVE STREAM' : 'RECONNECTING...'}
            </span>
          </div>

          {/* Node Count */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--bg-subtle)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border-card)' }}>
            <Cpu size={14} color="var(--accent-indigo)" />
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              <strong style={{ color: 'var(--text-main)' }}>{activeNodes}</strong>/{totalNodes} Nodes &nbsp; ({totalClusters} Clusters)
            </span>
          </div>

          {/* Aggregator */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'var(--bg-subtle)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border-card)' }}>
            <ShieldCheck size={14} color="var(--accent-emerald)" />
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Spring Boot + gRPC :9090</span>
          </div>
        </div>

      </div>
    </header>
  );
};
