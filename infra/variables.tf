variable "region" {
  description = "AWS region for every resource"
  type        = string
  default     = "eu-north-1"
}

variable "name" {
  description = "Prefix for resource names and the Name tag"
  type        = string
  default     = "events-manager"
}

variable "instance_type" {
  description = "EC2 instance type; the app, Postgres and Redis share one host, so a micro (1 GB) is only good for short tests"
  type        = string
  default     = "t3.medium"
}

variable "ecr_force_delete" {
  description = "Let `terraform destroy` delete the ECR repository even if it still holds images"
  type        = bool
  default     = false
}
