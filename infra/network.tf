# No ingress rules on purpose: Session Manager needs no inbound port (the agent connects out) and there is no SSH.
# The load balancer adds the only ingress later (8080, from its own security group).
resource "aws_security_group" "ec2" {
  name        = "${var.name}-ec2"
  description = "Security group for the ${var.name} EC2 host"

  tags = {
    Name = "${var.name}-ec2"
  }
}

# Terraform does not create the allow-all egress rule the console adds; without it the SSM agent and the image pulls cannot get out.
resource "aws_vpc_security_group_egress_rule" "all_out" {
  security_group_id = aws_security_group.ec2.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}
