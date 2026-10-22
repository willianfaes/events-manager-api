data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  compose_version = "v2.35.1"
}

# The default VPC gives the host a public IP, used only for outbound traffic (SSM agent, ECR, image pulls)
# because there is no NAT gateway. The security group has no ingress, so nothing can reach it from the Internet.
resource "aws_instance" "app" { # NOSONAR: public IP needed for egress; no ingress rules (see network.tf)
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.instance_type
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  # Runs once, on the first boot: a changed script replaces the instance instead of leaving a hand-configured host.
  user_data_replace_on_change = true
  user_data                   = <<-EOF
    #!/bin/bash
    set -euo pipefail

    dnf install -y docker
    systemctl enable --now docker

    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -fSL https://github.com/docker/compose/releases/download/${local.compose_version}/docker-compose-linux-x86_64 \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

    mkdir -p /opt/events-manager
  EOF

  # A newer AMI would otherwise replace a running host (and its database) on the next plan.
  lifecycle {
    ignore_changes = [ami]
  }

  tags = {
    Name = var.name
  }
}
