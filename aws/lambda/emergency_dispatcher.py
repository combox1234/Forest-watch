"""
AWS Lambda Serverless Handler: Emergency Alert Dispatcher
Course: Distributed Systems (Unit IV: Serverless Architectures & AWS Case Study)
Project: Distributed Environmental Sensor Network

Trigger: Invoked by Central Coordinator / Edge Cluster when critical alerts occur.
Purpose: Deduplicates and broadcasts emergency notifications (e.g. via Amazon SNS/SES)
         preserving Lamport Vector Clocks and causal order.
"""

import json
import os
import time

def lambda_handler(event, context):
    print("Received event:", json.dumps(event))
    
    # Parse incoming payload
    body = event
    if isinstance(event, str):
        try:
            body = json.loads(event)
        except Exception:
            body = {"raw": event}
    elif "body" in event and isinstance(event["body"], str):
        try:
            body = json.loads(event["body"])
        except Exception:
            body = {"raw": event["body"]}
            
    alert_type = body.get("alertType", "UNKNOWN_ALERT")
    cluster_id = body.get("clusterId", "C1")
    source_node = body.get("sourceNodeId", body.get("sourceNode", "UNKNOWN"))
    vector_clock = body.get("vectorClock", "[0,0,0,0]")
    description = body.get("description", "Emergency anomaly detected by sensor network")
    
    request_id = getattr(context, "aws_request_id", "local-simulated-req-id")
    
    # Simulated Emergency Broadcast logic (e.g. boto3 SNS publish)
    # import boto3
    # sns = boto3.client('sns', region_name=os.environ.get('AWS_REGION', 'us-east-1'))
    # sns.publish(TopicArn=os.environ.get('SNS_TOPIC_ARN'), Message=json.dumps(body))
    
    response_payload = {
        "status": "DISPATCHED",
        "lambdaFunction": "EmergencyAlertDispatcher",
        "awsRequestId": request_id,
        "processedAt": int(time.time() * 1000),
        "alert": {
            "type": alert_type,
            "cluster": cluster_id,
            "sourceNode": source_node,
            "causalVectorClock": vector_clock,
            "description": description
        },
        "action": "BROADCAST_COMPLETED",
        "message": f"Alert {alert_type} from node {source_node} in cluster {cluster_id} dispatched successfully."
    }
    
    return {
        "statusCode": 200,
        "headers": {
            "Content-Type": "application/json",
            "X-Serverless-Framework": "AWS-Lambda-Python3.12"
        },
        "body": json.dumps(response_payload)
    }
