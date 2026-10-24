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

output "cognito_issuer_uri" {
  description = "Value of the API's issuer-uri: it validates the tokens against this pool"
  value       = "https://${aws_cognito_user_pool.main.endpoint}"
}

output "cognito_client_id" {
  description = "Id of the Swagger app client (not a secret)"
  value       = aws_cognito_user_pool_client.swagger.id
}

output "cognito_hosted_ui_url" {
  description = "Base address of the hosted login page"
  value       = "https://${aws_cognito_user_pool_domain.main.domain}.auth.${var.region}.amazoncognito.com"
}
