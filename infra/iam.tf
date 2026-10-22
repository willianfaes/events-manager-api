data "aws_caller_identity" "current" {}

resource "aws_iam_role" "ec2" {
  name = "${var.name}-ec2"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = "sts:AssumeRole"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# The managed SSM policy does not allow reading parameters, so the app secrets need their own, read-only.
resource "aws_iam_role_policy" "read_params" {
  name = "read-${var.name}-params"
  role = aws_iam_role.ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"]
      # GetParametersByPath is authorized on the path itself, not only on its children.
      Resource = [
        "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/${var.name}/prod",
        "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/${var.name}/prod/*",
      ]
    }]
  })
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.name}-ec2"
  role = aws_iam_role.ec2.name
}
