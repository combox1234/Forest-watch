output "public_ip" {
  description = "Public IP address of the ForestWatch EC2 instance"
  value       = aws_instance.forestwatch_node.public_ip
}

output "ssh_command" {
  description = "SSH command to connect to the ForestWatch instance"
  value       = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.forestwatch_node.public_ip}"
}

output "application_url" {
  description = "Public entry point for ForestWatch (NGINX Load Balancer & Reverse Proxy on Port 80)"
  value       = "http://${aws_instance.forestwatch_node.public_ip}/"
}

output "sensor_consoles_info" {
  description = "Sensor node consoles routing info"
  value       = "Accessible via paths: /sensor1/, /sensor2/, /sensor3/, /sensor4/ on port 80"
}

output "monitoring_tunnel_command" {
  description = "Secure SSH tunnel to view Prometheus (:9090) and Grafana (:3000) locally"
  value       = "ssh -i ${var.key_name}.pem -L 3000:localhost:3000 -L 9090:localhost:9090 ubuntu@${aws_instance.forestwatch_node.public_ip}"
}
