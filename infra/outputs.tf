output "ecr_repository_urls" {
  description = "Image repositories, by service: the pipelines tag and push the images here"
  value       = { for service, repository in aws_ecr_repository.service : service => repository.repository_url }
}

output "instance_id" {
  description = "EC2 instance id: aws ssm start-session --target <id>"
  value       = aws_instance.app.id
}

output "alb_dns_name" {
  description = "Address of the load balancer; its certificate is for the domain, so use api_url instead"
  value       = aws_lb.main.dns_name
}

output "api_url" {
  description = "Public address of the API"
  value       = "https://${local.api_domain}"
}

output "name_servers" {
  description = "Name servers of the hosted zone; the registrar must point the domain to them"
  value       = aws_route53_zone.main.name_servers
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

output "demo_user_sub" {
  description = "Sub of the demo user: the API's DEMO_USER_SUB, so DataSeeder gives it a profile"
  value       = aws_cognito_user.demo.sub
}
