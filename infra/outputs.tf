output "ecr_repository_url" {
  description = "Image repository: tag and push the API image here"
  value       = aws_ecr_repository.api.repository_url
}

output "instance_id" {
  description = "EC2 instance id: aws ssm start-session --target <id>"
  value       = aws_instance.app.id
}

output "alb_dns_name" {
  description = "Public address of the load balancer (HTTP only until there is a domain)"
  value       = aws_lb.main.dns_name
}

output "github_deploy_role_arn" {
  description = "Role the deploy workflow assumes through OIDC"
  value       = aws_iam_role.github_deploy.arn
}
