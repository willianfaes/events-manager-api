# GitHub Actions signs in with the access key of this user, kept in the repository secrets AWS_ACCESS_KEY_ID and
# AWS_SECRET_ACCESS_KEY. OIDC (no stored key) would be better, but the organization SCP denies creating the OIDC
# provider. The key is created by hand, never here, so the secret does not end up in the state (see README.md).
# The name is generic on purpose: the notification service repository reuses this user.
resource "aws_iam_user" "ci" {
  name = "events-ci"

  # The key lives in GitHub: destroying the user would kill it, so teardown.sh keeps this user and its policy.
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_iam_user_policy" "ci" {
  name = "publish-and-deploy"
  user = aws_iam_user.ci.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # The login token request does not support resource-level permissions.
        Effect   = "Allow"
        Action   = "ecr:GetAuthorizationToken"
        Resource = "*"
      },
      {
        # Built from the name, not from the repository resource, because teardown destroys the repository and
        # keeps this user.
        Effect = "Allow"
        Action = [
          "ecr:DescribeImages",
          "ecr:BatchGetImage", # docker push reads the manifest (HEAD) around the upload
          "ecr:BatchCheckLayerAvailability",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage",
        ]
        Resource = "arn:aws:ecr:${var.region}:${data.aws_caller_identity.current.account_id}:repository/${var.name}-api"
      },
      {
        Effect   = "Allow"
        Action   = "ssm:SendCommand"
        Resource = "arn:aws:ssm:${var.region}::document/AWS-RunShellScript"
      },
      {
        # Only on the host, found by its Name tag (its id changes on every apply). The tag condition cannot sit
        # in the statement above: a document has no tags, so the request would be denied.
        Effect   = "Allow"
        Action   = "ssm:SendCommand"
        Resource = "arn:aws:ec2:${var.region}:${data.aws_caller_identity.current.account_id}:instance/*"
        Condition = {
          StringEquals = { "ssm:resourceTag/Name" = var.name }
        }
      },
      {
        # Reading a command result has no resource-level permissions either.
        Effect   = "Allow"
        Action   = ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"]
        Resource = "*"
      },
    ]
  })
}
