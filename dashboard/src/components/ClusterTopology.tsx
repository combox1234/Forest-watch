import React, { useMemo } from 'react';
import { Crown, Battery, Skull, RotateCcw, Wifi, Zap } from 'lucide-react';
import { SensorNode, ClusterGroup, ReadingSummary } from '../types';

interface ClusterTopologyProps {
  clusters: ClusterGroup[];
  nodes: SensorNode[];
  latestSummaries: Record<string, ReadingSummary>;
  onFailNode: (nodeId: string) => void;
  onRecoverNode: (nodeId: string) => void;
}

/** Returns x,y for each node arranged in a small circle around cluster center */
function computeNodePositions(
  nodeIds: string[],
  cx: number,
  cy: number,
  radius: number
): Record<string, { x: number; y: number }> {
  const positions: Record<string, { x: number; y: number }> = {};
  const count = nodeIds.length;
  nodeIds.forEach((id, i) => {
    const angle = (2 * Math.PI * i) / count - Math.PI / 2;
    positions[id] = {
      x: cx + radius * Math.cos(angle),
      y: cy + radius * Math.sin(angle),
    };
  });
  return positions;
}

function nodeColor(status: string, isLeader: boolean): { fill: string; stroke: string; glow: string } {
  if (status === 'FAILED')    return { fill: '#fee2e2', stroke: '#e11d48', glow: 'rgba(225,29,72,0.35)' };
  if (status === 'SUSPECTED') return { fill: '#ffedd5', stroke: '#ea580c', glow: 'rgba(234,88,12,0.4)' };
  if (isLeader)               return { fill: '#fef08a', stroke: '#ca8a04', glow: 'rgba(234,179,8,0.6)' };
  return { fill: '#dcfce7', stroke: '#059669', glow: 'rgba(5,150,105,0.3)' };
}

function sensorIcon(type: string): string {
  const icons: Record<string, string> = {
    TEMPERATURE: '🌡',
    HUMIDITY: '💧',
    MOTION: '📡',
    MULTISENSOR: '⚡',
    WATER_QUALITY: '🌊',
  };
  return icons[type] || '📟';
}

/* Individual cluster SVG graph */
const ClusterGraph: React.FC<{
  cluster: ClusterGroup;
  clusterNodes: SensorNode[];
}> = ({ cluster, clusterNodes }) => {
  const W = 280;
  const H = 245;
  const CX = W / 2;
  const CY = 120;
  const radius = 64;

  const nodePositions = useMemo(
    () => computeNodePositions(clusterNodes.map(n => n.sensorId), CX, CY, radius),
    [clusterNodes, CX, CY, radius]
  );

  const leaderId = cluster.leaderId;

  return (
    <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ display: 'block', margin: '0 auto', overflow: 'visible' }}>
      {/* Cluster background circle */}
      <circle
        cx={CX} cy={CY} r={82}
        fill="rgba(99,102,241,0.04)"
        stroke="rgba(99,102,241,0.14)"
        strokeWidth={1.5}
        strokeDasharray="6 3"
      />

      {/* Center cluster badge */}
      <circle cx={CX} cy={CY} r={18} fill="#ffffff" stroke="rgba(99,102,241,0.2)" strokeWidth={1} />
      <text x={CX} y={CY + 1} textAnchor="middle" dominantBaseline="middle"
        fontSize="8" fontWeight="800" fontFamily="Inter, sans-serif" fill="#4f46e5" letterSpacing="0.04em">
        {cluster.clusterName.split(' - ')[0].toUpperCase()}
      </text>

      {/* Edges: each node → leader (hub-spoke) */}
      {clusterNodes.map(node => {
        if (!leaderId || node.sensorId === leaderId) return null;
        const from = nodePositions[node.sensorId];
        const to = nodePositions[leaderId];
        if (!from || !to) return null;
        const isFailedNode = node.status === 'FAILED';
        return (
          <line
            key={`edge-${node.sensorId}`}
            x1={from.x} y1={from.y}
            x2={to.x} y2={to.y}
            className={isFailedNode ? '' : 'cluster-graph-line'}
            stroke={isFailedNode ? '#fca5a5' : undefined}
            strokeDasharray={isFailedNode ? '3 4' : undefined}
            strokeWidth={isFailedNode ? 1 : undefined}
            opacity={isFailedNode ? 0.35 : undefined}
          />
        );
      })}

      {/* Nodes */}
      {clusterNodes.map(node => {
        const isLeader = node.sensorId === leaderId;
        const pos = nodePositions[node.sensorId];
        if (!pos) return null;
        const { fill, stroke, glow } = nodeColor(node.status, isLeader);
        const isFailed = node.status === 'FAILED';

        return (
          <g key={node.sensorId}>
            {/* Glow ring for leader */}
            {isLeader && (
              <circle
                cx={pos.x} cy={pos.y} r={22}
                fill="none"
                stroke={stroke}
                strokeWidth={2}
                opacity={0.35}
                style={{ animation: 'pulse-ring 2.4s infinite ease-in-out' }}
              />
            )}
            {/* Node circle */}
            <circle
              cx={pos.x} cy={pos.y} r={17}
              fill={fill}
              stroke={stroke}
              strokeWidth={isLeader ? 2.5 : 1.8}
              style={{
                filter: isFailed ? 'none' : `drop-shadow(0 0 5px ${glow})`,
                opacity: isFailed ? 0.55 : 1
              }}
            />
            {/* Sensor icon */}
            <text x={pos.x} y={pos.y + 1} textAnchor="middle" dominantBaseline="middle"
              fontSize="11" style={{ userSelect: 'none' }}>
              {sensorIcon(node.type)}
            </text>
            {/* Crown for leader */}
            {isLeader && (
              <text x={pos.x} y={pos.y - 22} textAnchor="middle" fontSize="11"
                style={{ userSelect: 'none' }}>
                👑
              </text>
            )}
            {/* FAILED badge */}
            {isFailed && (
              <text x={pos.x} y={pos.y - 23} textAnchor="middle" fontSize="8"
                fill="#e11d48" fontWeight="700" fontFamily="Inter, sans-serif">
                ✕ CRASHED
              </text>
            )}
            {/* Node ID label with clear high-contrast pill */}
            <g transform={`translate(${pos.x}, ${pos.y + 26})`}>
              <rect
                x="-16" y="-8" width="32" height="16" rx="4"
                fill={isFailed ? '#fee2e2' : isLeader ? '#fef3c7' : '#ffffff'}
                stroke={isFailed ? '#e11d48' : isLeader ? '#d97706' : '#cbd5e1'}
                strokeWidth="1.2"
                style={{ filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.08))' }}
              />
              <text
                x="0" y="3.5"
                textAnchor="middle"
                fontSize="9.5"
                fontFamily="'JetBrains Mono', monospace"
                fontWeight="700"
                fill={isFailed ? '#e11d48' : isLeader ? '#92400e' : '#0f172a'}
              >
                {node.sensorId}
              </text>
            </g>
          </g>
        );
      })}
    </svg>
  );
};

export const ClusterTopology: React.FC<ClusterTopologyProps> = ({
  clusters, nodes, latestSummaries, onFailNode, onRecoverNode
}) => {
  return (
    <div style={{ marginBottom: '1.5rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
            Cluster &amp; Edge Node Topology
          </h2>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '2px' }}>
            Hub-spoke graph — leader nodes are highlighted in <span style={{ color: '#b45309', fontWeight: 600 }}>gold</span>,
            crashed nodes in <span style={{ color: '#e11d48', fontWeight: 600 }}>red</span>,
            healthy nodes in <span style={{ color: '#059669', fontWeight: 600 }}>green</span>
          </p>
        </div>
        {/* Legend */}
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
          {[
            { color: '#059669', bg: '#dcfce7', label: 'Healthy (Green)' },
            { color: '#ca8a04', bg: '#fef08a', label: 'Leader (Gold 👑)' },
            { color: '#ea580c', bg: '#ffedd5', label: 'Suspected (Orange)' },
            { color: '#e11d48', bg: '#fee2e2', label: 'Failed (Red ✕)' },
          ].map(({ color, bg, label }) => (
            <div key={label} style={{ display: 'flex', alignItems: 'center', gap: '5px', fontSize: '0.72rem', color: 'var(--text-muted)' }}>
              <span style={{ width: '12px', height: '12px', borderRadius: '50%', background: bg, border: `2px solid ${color}`, display: 'inline-block' }} />
              {label}
            </div>
          ))}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '1.25rem' }}>
        {clusters.map((cluster) => {
          const clusterNodes = nodes.filter(n => n.clusterId === cluster.clusterId);
          const summary = latestSummaries[cluster.clusterId];
          const failedCount = clusterNodes.filter(n => n.status === 'FAILED').length;
          const healthyCount = clusterNodes.filter(n => n.status === 'HEALTHY').length;

          return (
            <div key={cluster.clusterId} className="glass-panel" style={{ padding: '1.25rem', position: 'relative' }}>

              {/* Cluster Header */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <div>
                  <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>
                    {cluster.clusterName}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', fontFamily: 'JetBrains Mono, monospace' }}>
                    {cluster.clusterId}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', background: 'rgba(217,119,6,0.1)', border: '1px solid rgba(217,119,6,0.3)', padding: '4px 10px', borderRadius: '20px' }}>
                  <Crown size={13} color="#b45309" />
                  <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#78350f' }}>
                    Leader: {cluster.leaderId || 'ELECTING...'}
                  </span>
                </div>
              </div>

              {/* ─── SVG Network Graph ─── */}
              <div style={{
                background: 'var(--bg-subtle)',
                borderRadius: '12px',
                border: '1px solid var(--border-card)',
                padding: '8px 4px 4px',
                marginBottom: '0.85rem',
                position: 'relative',
                overflow: 'hidden'
              }}>
                <div style={{ position: 'absolute', top: 6, left: 10, fontSize: '0.62rem', fontWeight: 700, letterSpacing: '0.07em', textTransform: 'uppercase', color: 'var(--text-dim)' }}>
                  Network Graph
                </div>
                <ClusterGraph cluster={cluster} clusterNodes={clusterNodes} />
              </div>

              {/* Edge Aggregated Stats */}
              {summary ? (
                <div style={{
                  background: 'linear-gradient(135deg, rgba(99,102,241,0.06), rgba(6,182,212,0.06))',
                  border: '1px solid rgba(99,102,241,0.15)',
                  borderRadius: '10px', padding: '8px 12px', marginBottom: '0.85rem',
                  display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '4px', textAlign: 'center'
                }}>
                  <div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Avg Temp</div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--accent-blue)' }}>{summary.avgTemperature}°C</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Avg Humidity</div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--accent-teal)' }}>{summary.avgHumidity}%</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.65rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Samples</div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--accent-purple)' }}>{summary.sampleCount}</div>
                  </div>
                </div>
              ) : (
                <div style={{
                  background: 'var(--bg-subtle)', borderRadius: '10px', padding: '6px 12px',
                  marginBottom: '0.85rem', border: '1px solid var(--border-card)',
                  fontSize: '0.72rem', color: 'var(--text-dim)', textAlign: 'center'
                }}>
                  Awaiting first edge aggregated summary…
                </div>
              )}

              {/* Status Summary + Node Action List */}
              <div style={{ display: 'flex', gap: '6px', marginBottom: '0.75rem', flexWrap: 'wrap' }}>
                <span className="badge badge-healthy"><Wifi size={10} />{healthyCount} Healthy</span>
                {failedCount > 0 && <span className="badge badge-failed"><Skull size={10} />{failedCount} Crashed</span>}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.6rem' }}>
                {clusterNodes.map(node => {
                  const isLeader = node.sensorId === cluster.leaderId;
                  const isFailed = node.status === 'FAILED';
                  const isSuspected = node.status === 'SUSPECTED';

                  return (
                    <div
                      key={node.sensorId}
                      className={`glass-card-interactive ${isLeader ? 'leader-card' : ''}`}
                      style={{ padding: '0.75rem', opacity: isFailed ? 0.65 : 1 }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                          <span style={{ fontSize: '14px' }}>{sensorIcon(node.type)}</span>
                          <span className="font-mono" style={{ fontSize: '0.9rem', fontWeight: 800, color: 'var(--text-main)' }}>
                            {node.sensorId}
                          </span>
                          {isLeader && <Crown size={12} color="#b45309" />}
                        </div>
                        <span className={`badge ${isFailed ? 'badge-failed' : isSuspected ? 'badge-suspected' : 'badge-healthy'}`}
                          style={{ fontSize: '0.6rem', padding: '1px 6px' }}>
                          {node.status}
                        </span>
                      </div>

                      <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginBottom: '6px' }}>
                        {node.type.replace('_', ' ')}
                      </div>

                      {/* Battery Bar */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px', marginBottom: '8px' }}>
                        <Battery size={11} color="var(--text-dim)" />
                        <div style={{ flex: 1, height: '4px', background: '#e2e8f0', borderRadius: '2px', overflow: 'hidden' }}>
                          <div style={{
                            height: '100%',
                            width: `${Math.round(node.batteryScore || 100)}%`,
                            background: (node.batteryScore || 100) > 40 ? '#059669' : '#e11d48',
                            borderRadius: '2px'
                          }} />
                        </div>
                        <span style={{ fontSize: '0.65rem', color: 'var(--text-dim)' }}>{Math.round(node.batteryScore || 100)}%</span>
                      </div>

                      {/* Action Buttons */}
                      <div style={{ display: 'flex', gap: '4px' }}>
                        {!isFailed ? (
                          <button onClick={() => onFailNode(node.sensorId)}
                            className="btn-danger"
                            style={{ flex: 1, padding: '3px 4px', fontSize: '0.68rem', justifyContent: 'center' }}
                            title={`Crash ${node.sensorId} → triggers Bully re-election`}>
                            <Skull size={10} /> Crash
                          </button>
                        ) : (
                          <button onClick={() => onRecoverNode(node.sensorId)}
                            className="btn-outline"
                            style={{ flex: 1, padding: '3px 4px', fontSize: '0.68rem', justifyContent: 'center', borderColor: '#059669', color: '#059669' }}
                            title={`Recover ${node.sensorId}`}>
                            <RotateCcw size={10} /> Recover
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

            </div>
          );
        })}
      </div>
    </div>
  );
};
