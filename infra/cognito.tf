# Identity lives here, not in the API: users sign up and log in against the pool, and the API only validates its tokens.
resource "aws_cognito_user_pool" "main" {
  name = var.name

  # Log in with the e-mail address; the e-mail is trusted without a confirmation message (users are created by an admin).
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  # No public sign-up: only an admin creates users, so the pool never sends mail on its own.
  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = true
  }

  # A new pool has a new id, issuer and `sub` for every user, which orphans the profiles in the database.
  # A plain `terraform destroy` fails here on purpose; see README.md for the teardown that keeps the pool.
  lifecycle {
    prevent_destroy = true
  }
}

# The hosted login page (classic version): users type their credentials here, so there is no front end to build.
resource "aws_cognito_user_pool_domain" "main" {
  domain       = "${var.name}-${data.aws_caller_identity.current.account_id}"
  user_pool_id = aws_cognito_user_pool.main.id
}

# Swagger UI is a public client: it cannot keep a secret, so the code flow is protected by PKCE instead.
resource "aws_cognito_user_pool_client" "swagger" {
  name         = "${var.name}-swagger"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "profile"]
  supported_identity_providers         = ["COGNITO"]

  # Cognito only accepts HTTPS callbacks, except localhost: the deployed Swagger waits for a domain (HTTPS).
  callback_urls = ["http://localhost:8080/swagger-ui/oauth2-redirect.html"]

  # USER_PASSWORD_AUTH lets the AWS CLI get a token without a browser (to check the groups claim).
  explicit_auth_flows           = ["ALLOW_USER_SRP_AUTH", "ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH"]
  prevent_user_existence_errors = "ENABLED"
}

# The API reads these from the `cognito:groups` claim and turns them into ROLE_USER and ROLE_ADMIN.
resource "aws_cognito_user_group" "user" {
  name         = "USER"
  user_pool_id = aws_cognito_user_pool.main.id
  description  = "Regular users"
}

resource "aws_cognito_user_group" "admin" {
  name         = "ADMIN"
  user_pool_id = aws_cognito_user_pool.main.id
  description  = "Can manage events"
}
