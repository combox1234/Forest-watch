output "public_ip" {
  description = "Public IP address of the ForestWatch EC2 instance"
  value       = aws_instance.forestwatch_node.public_ip
}

output "ssh_command" {
  description = "SSH Command to connect to the ForestWatch instance"
  value       = "ssh -i ${var.key_name}.pem ubuntu@${aws_instance.forestwatch_node.public_ip}"
}

output "central_dashboard_url" {
  description = "URL for ForestWatch Central Operations Dashboard"
  value       = "http://${aws_instance.forestwatch_node.public_ip}:8099/"
}

output "grafana_dashboard_url" {
  description = "URL for Grafana Observability Dashboard (admin/admin)"
  value       = "http://${aws_instance.forestwatch_node.public_ip}:3000/"
}

output "prometheus_url" {
  description = "URL for Prometheus Metrics Server"
  value       = "http://${aws_instance.forestwatch_node.public_ip}:9090/"
}
