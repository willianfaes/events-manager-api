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

variable "domain_name" {
  description = "Domain bought outside AWS; its hosted zone and certificate survive the teardown. The API is served at events.<domain_name>"
  type        = string
  default     = "williansoares.dev"
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

variable "demo_user_password" {
  description = "Password of the demo user; public on purpose (it is in the README), so it has a default"
  type        = string
  default     = "Demo1234!"
}

variable "demo_admin_password" {
  description = "Password of the demo admin; set it in terraform.tfvars (git-ignored). Without it the admin is not created"
  type        = string
  default     = null
  sensitive   = true
}

variable "load_test_password" {
  description = "Password of the k6 load-test users; set it in terraform.tfvars (git-ignored) and in .env. Without it they are not created"
  type        = string
  default     = null
  sensitive   = true
}
