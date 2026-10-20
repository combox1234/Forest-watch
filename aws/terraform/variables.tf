variable "aws_region" {
  description = "AWS deployment region (Mumbai default for ForestWatch)"
  type        = string
  default     = "ap-south-1"
}

variable "instance_type" {
  description = "EC2 instance type (t3.small recommended for 2GB RAM; t2.micro for strict Free Tier)"
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "Name of existing AWS EC2 Key Pair for SSH access"
  type        = string
  default     = "forestwatch-key"
}

variable "environment" {
  description = "Deployment environment name"
  type        = string
  default     = "production"
}

variable "admin_cidr" {
  description = "Trusted administrative IPv4 CIDR block permitted for SSH terminal access. Must be the operator's public IP as a /32 (e.g. '203.0.113.25/32') or a corporate bastion CIDR. 0.0.0.0/0 and internal VPC ranges are prohibited."
  type        = string

  validation {
    condition     = can(cidrhost(var.admin_cidr, 0)) && var.admin_cidr != "0.0.0.0/0" && var.admin_cidr != "10.0.0.0/16"
    error_message = "admin_cidr must be a valid CIDR block (e.g. '203.0.113.25/32'). It cannot be 0.0.0.0/0 or the internal VPC CIDR 10.0.0.0/16."
  }
}

variable "enable_monitoring_ingress" {
  description = "Whether to permit direct ingress to Prometheus (9090) and Grafana (3000) from the admin_cidr. Defaults to false (access via SSH tunnel recommended for security)."
  type        = bool
  default     = false
}
