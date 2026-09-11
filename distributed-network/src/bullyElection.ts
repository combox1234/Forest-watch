import { InterNodeMessage } from './types';
import { VectorClock } from './vectorClock';

export interface ElectionCallbacks {
  sendMessage(targetId: string, type: 'ELECTION' | 'OK' | 'COORDINATOR', payload?: any): void;
  broadcastMessage(type: 'ELECTION' | 'OK' | 'COORDINATOR', payload?: any): void;
  onElectedAsLeader(electionDurationMs: number, reason: string): void;
  onNewLeaderRecognized(newLeaderId: string): void;
}

export class BullyElectionManager {
  private nodeId: string;
  private clusterId: string;
  private clusterNodes: string[];
  private currentLeaderId: string | null = null;
  private inElection = false;
  private electionStartTime = 0;
  private higherNodeResponded = false;
  private electionTimer: NodeJS.Timeout | null = null;
  private coordinatorWaitTimer: NodeJS.Timeout | null = null;
  private callbacks: ElectionCallbacks;

  constructor(
    nodeId: string,
    clusterId: string,
    clusterNodes: string[],
    initialLeaderId: string,
    callbacks: ElectionCallbacks
  ) {
    this.nodeId = nodeId;
    this.clusterId = clusterId;
    this.clusterNodes = [...clusterNodes].sort((a, b) => this.comparePriority(a, b));
    this.currentLeaderId = initialLeaderId;
    this.callbacks = callbacks;
  }

  /**
   * Compare priority between two node IDs.
   * Extracts numerical part if present (e.g. S4 > S3), else string compare.
   */
  public comparePriority(idA: string, idB: string): number {
    const numA = parseInt(idA.replace(/\D/g, ''), 10);
    const numB = parseInt(idB.replace(/\D/g, ''), 10);
    if (!isNaN(numA) && !isNaN(numB)) {
      return numA - numB;
    }
    return idA.localeCompare(idB);
  }

  public getHigherPriorityNodes(): string[] {
    return this.clusterNodes.filter(id => this.comparePriority(id, this.nodeId) > 0);
  }

  /**
   * Start a Bully election
   */
  public startElection(reason: string) {
    if (this.inElection) return;
    this.inElection = true;
    this.electionStartTime = Date.now();
    this.higherNodeResponded = false;

    const higherNodes = this.getHigherPriorityNodes();
    console.log(`[Bully Election] [${this.nodeId}]: Starting election in ${this.clusterId} (${reason}). Higher nodes: [${higherNodes.join(', ')}]`);

    if (higherNodes.length === 0) {
      // We are already the highest priority node!
      this.declareVictory(reason);
      return;
    }

    // Step 2: Send ELECTION message to all higher-priority nodes
    for (const higherNode of higherNodes) {
      this.callbacks.sendMessage(higherNode, 'ELECTION', { reason, initiator: this.nodeId });
    }

    // Set timeout to wait for 'OK'
    if (this.electionTimer) clearTimeout(this.electionTimer);
    this.electionTimer = setTimeout(() => {
      if (!this.higherNodeResponded) {
        console.log(`[Bully Election] [${this.nodeId}]: No higher nodes responded to ELECTION within timeout. Declaring self coordinator.`);
        this.declareVictory(reason);
      }
    }, 1500);
  }

  /**
   * Handle incoming ELECTION message from a lower-priority node
   */
  public handleElectionMessage(senderId: string) {
    if (this.comparePriority(this.nodeId, senderId) > 0) {
      // We are higher: reply OK immediately to stop sender from declaring victory
      this.callbacks.sendMessage(senderId, 'OK', { responder: this.nodeId });

      // And start our own election to challenge other higher nodes (or win)
      if (!this.inElection) {
        this.startElection(`Challenged by lower node ${senderId}`);
      }
    }
  }

  /**
   * Handle OK message received in response to ELECTION
   */
  public handleOkMessage(senderId: string) {
    console.log(`[Bully Election] [${this.nodeId}]: Received OK from higher node ${senderId}. Waiting for coordinator announcement.`);
    this.higherNodeResponded = true;
    if (this.electionTimer) {
      clearTimeout(this.electionTimer);
      this.electionTimer = null;
    }

    // Step 3: Wait for higher node to broadcast COORDINATOR
    if (this.coordinatorWaitTimer) clearTimeout(this.coordinatorWaitTimer);
    this.coordinatorWaitTimer = setTimeout(() => {
      // If higher node failed before sending coordinator, restart election
      console.log(`[Bully Election] [${this.nodeId}]: Higher node ${senderId} sent OK but timed out announcing coordinator. Restarting election!`);
      this.inElection = false;
      this.startElection(`Higher node ${senderId} timed out`);
    }, 3500);
  }

  /**
   * Handle COORDINATOR message announcing a new leader
   */
  public handleCoordinatorMessage(newLeaderId: string) {
    console.log(`[Bully Election] [${this.nodeId}]: New coordinator recognized: ${newLeaderId} for cluster ${this.clusterId}`);
    this.currentLeaderId = newLeaderId;
    this.inElection = false;
    this.higherNodeResponded = false;

    if (this.electionTimer) clearTimeout(this.electionTimer);
    if (this.coordinatorWaitTimer) clearTimeout(this.coordinatorWaitTimer);

    this.callbacks.onNewLeaderRecognized(newLeaderId);
  }

  private declareVictory(reason: string) {
    const duration = Date.now() - this.electionStartTime;
    this.currentLeaderId = this.nodeId;
    this.inElection = false;

    if (this.electionTimer) clearTimeout(this.electionTimer);
    if (this.coordinatorWaitTimer) clearTimeout(this.coordinatorWaitTimer);

    console.log(`🏆 [Bully Election] [${this.nodeId}]: CROWNED COORDINATOR for ${this.clusterId}! Broadcasting announcement.`);
    
    // Step 5: Broadcast COORDINATOR message
    this.callbacks.broadcastMessage('COORDINATOR', {
      leaderId: this.nodeId,
      clusterId: this.clusterId,
      electionDurationMs: duration,
      reason
    });

    // Notify local callbacks
    this.callbacks.onElectedAsLeader(duration, reason);
  }

  public getCurrentLeader(): string | null {
    return this.currentLeaderId;
  }

  public isLeader(): boolean {
    return this.currentLeaderId === this.nodeId;
  }
}
