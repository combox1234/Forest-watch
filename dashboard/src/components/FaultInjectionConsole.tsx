import React, { useState } from 'react';
import { Skull, Flame, Hourglass, RotateCcw, Zap, CheckCircle } from 'lucide-react';
import { ClusterGroup } from '../types';

interface FaultInjectionConsoleProps {
  clusters: ClusterGroup[];
  onFailNode: (nodeId: string) => void;
  onTriggerConcurrentAlert: (clusterId: string) => void;
  onInjectDelay: (nodeId: string, delayMs: number) => void;
  onRecoverAll: () => void;
}

export const FaultInjectionConsole: React.FC<FaultInjectionConsoleProps> = ({
  clusters, onFailNode, onTriggerConcurrentAlert, onInjectDelay, onRecoverAll
}) => {
  const [feedback, setFeedback] = useState<string | null>(null);

  const showNotification = (msg: string) => {
    setFeedback(msg);
    setTimeout(() => setFeedback(null), 4000);
  };

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', marginBottom: '1.5rem', border: '1px solid rgba(225,29,72,0.25)', background: 'linear-gradient(135deg, rgba(225,29,72,0.03), rgba(234,88,12,0.02))' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Zap size={20} color="var(--accent-rose)" />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--text-main)' }}>
            Fault Injection &amp; Viva Demonstration Console
          </h3>
          <span className="badge" style={{ background: 'rgba(225,29,72,0.08)', color: '#9f1239', border: '1px solid rgba(225,29,72,0.2)', fontSize: '0.65rem' }}>
            Section 14 &amp; 16 Test Suite
          </span>
        </div>

        <button
          onClick={() => {
            onRecoverAll();
            showNotification('All sensor nodes recovered to HEALTHY state.');
          }}
          className="btn-outline"
          style={{ borderColor: '#059669', color: '#059669' }}
        >
          <RotateCcw size={13} /> Recover All Nodes
        </button>
      </div>

      <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
        Inject real-time failure scenarios to demonstrate automatic failure recovery, the Bully algorithm, mutual exclusion deduplication, and vector clock ordering during viva evaluation.
      </p>

      {/* Action Buttons Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '0.75rem' }}>

        {/* Kill Leader per Cluster */}
        {clusters.map((c) => (
          <button
            key={c.clusterId}
            onClick={() => {
              if (c.leaderId) {
                onFailNode(c.leaderId);
                showNotification(`💥 Killed coordinator ${c.leaderId} on ${c.clusterName}! Heartbeat timeout will trigger Bully election.`);
              }
            }}
            className="btn-danger"
            style={{ padding: '10px 14px', justifyContent: 'center' }}
            disabled={!c.leaderId}
          >
            <Skull size={15} /> Kill {c.clusterName.split(' - ')[0]} Leader ({c.leaderId || 'None'})
          </button>
        ))}

        {/* Concurrent Wildfire Alert */}
        <button
          onClick={() => {
            onTriggerConcurrentAlert('cluster-1');
            showNotification('🔥 Concurrent wildfire alarm from S1 & S2! Testing mutual exclusion deduplication.');
          }}
          className="btn-amber"
          style={{ padding: '10px 14px', justifyContent: 'center' }}
        >
          <Flame size={15} /> Concurrent Fire Alert (Mutex Demo)
        </button>

        {/* Network Latency Injection */}
        <button
          onClick={() => {
            onInjectDelay('S1', 3500);
            showNotification('⏱️ Injected 3.5s latency on S1. Vector clocks will correctly reconstruct causal order upon delivery.');
          }}
          className="btn-primary"
          style={{ background: 'linear-gradient(135deg, var(--accent-purple), var(--accent-blue))', padding: '10px 14px', justifyContent: 'center' }}
        >
          <Hourglass size={15} /> Inject Network Latency (Causal Demo)
        </button>

      </div>

      {/* Notification Toast */}
      {feedback && (
        <div style={{
          marginTop: '1rem', padding: '8px 14px',
          background: 'rgba(5,150,105,0.08)', borderRadius: '8px',
          border: '1px solid rgba(5,150,105,0.25)',
          fontSize: '0.8rem', color: '#065f46',
          display: 'flex', alignItems: 'center', gap: '8px'
        }}>
          <CheckCircle size={14} color="#059669" />
          <span>{feedback}</span>
        </div>
      )}
    </div>
  );
};
