variable "aws_region" {
  description = "AWS deployment region"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type (t2.micro for Free Tier, t3.small for 2GB RAM)"
  type        = string
  default     = "t2.micro"
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
