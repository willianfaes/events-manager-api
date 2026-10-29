# Identity lives here, not in the API: users sign up and log in against the pool, and the API only validates its tokens.
resource "aws_cognito_user_pool" "main" {
  name = var.name

  # Log in with the e-mail address. A visitor who signs up gets a code by e-mail and must type it to confirm the address;
  # users created by an admin are marked as verified in their own resource below.
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  # Anyone can sign up on the hosted page. A new user has no group, so the API lets them in but not into /admin.
  # The default Cognito sender is limited to 50 e-mails a day (SES would lift it).
  admin_create_user_config {
    allow_admin_create_user_only = false
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

  # Cognito only accepts HTTPS callbacks, except localhost.
  callback_urls = [
    "http://localhost:8080/swagger-ui/oauth2-redirect.html",
    "https://${local.api_domain}/swagger-ui/oauth2-redirect.html",
  ]

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

# Demo users so a visitor can log in without signing up. The password is permanent, so there is no forced change
# at first login, and SUPPRESS keeps the pool from sending an invitation e-mail.
resource "aws_cognito_user" "demo" {
  user_pool_id   = aws_cognito_user_pool.main.id
  username       = "demo@example.com"
  password       = var.demo_user_password
  message_action = "SUPPRESS"

  attributes = {
    email          = "demo@example.com"
    email_verified = "true"
  }
}

resource "aws_cognito_user_in_group" "demo" {
  user_pool_id = aws_cognito_user_pool.main.id
  group_name   = aws_cognito_user_group.user.name
  username     = aws_cognito_user.demo.username
}

# The admin can edit and delete events, so its password is not in the repo: without it the user is not created.
resource "aws_cognito_user" "demo_admin" {
  count = var.demo_admin_password == null ? 0 : 1

  user_pool_id   = aws_cognito_user_pool.main.id
  username       = "admin-demo@example.com"
  password       = var.demo_admin_password
  message_action = "SUPPRESS"

  attributes = {
    email          = "admin-demo@example.com"
    email_verified = "true"
  }
}

resource "aws_cognito_user_in_group" "demo_admin" {
  count = var.demo_admin_password == null ? 0 : 1

  user_pool_id = aws_cognito_user_pool.main.id
  group_name   = aws_cognito_user_group.admin.name
  username     = aws_cognito_user.demo_admin[0].username
}

# Users for the k6 registration race (load-test/k6/script.js logs each one in). Without the password none is created.
locals {
  load_test_users = var.load_test_password == null ? 0 : 200
}

resource "aws_cognito_user" "load_test" {
  count = local.load_test_users

  user_pool_id   = aws_cognito_user_pool.main.id
  username       = "load-test-user-${count.index + 1}@example.com"
  password       = var.load_test_password
  message_action = "SUPPRESS"

  attributes = {
    email          = "load-test-user-${count.index + 1}@example.com"
    email_verified = "true"
  }
}

resource "aws_cognito_user_in_group" "load_test" {
  count = local.load_test_users

  user_pool_id = aws_cognito_user_pool.main.id
  group_name   = aws_cognito_user_group.user.name
  username     = aws_cognito_user.load_test[count.index].username
}
