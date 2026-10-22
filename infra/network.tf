# The only ingress is 8080 from the load balancer (below): Session Manager needs no inbound port
# (the agent connects out) and there is no SSH.
resource "aws_security_group" "ec2" {
  name        = "${var.name}-ec2"
  description = "Security group for the ${var.name} EC2 host"

  tags = {
    Name = "${var.name}-ec2"
  }
}

resource "aws_vpc_security_group_ingress_rule" "app_from_alb" {
  security_group_id            = aws_security_group.ec2.id
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
  referenced_security_group_id = aws_security_group.alb.id
}

# Terraform does not create the allow-all egress rule the console adds; without it the SSM agent and the image pulls cannot get out.
resource "aws_vpc_security_group_egress_rule" "all_out" {
  security_group_id = aws_security_group.ec2.id
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}
