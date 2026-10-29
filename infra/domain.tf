# The domain is bought at the registrar; this zone only answers for it. It is kept between sessions (see teardown.sh):
# a new zone gets new name servers, which would have to be changed at the registrar again.
resource "aws_route53_zone" "main" {
  name = var.domain_name

  lifecycle {
    prevent_destroy = true
  }
}
