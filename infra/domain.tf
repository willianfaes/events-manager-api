# The domain is bought at the registrar; this zone only answers for it. It is kept between sessions (see teardown.sh):
# a new zone gets new name servers, which would have to be changed at the registrar again.
resource "aws_route53_zone" "main" {
  name = var.domain_name

  lifecycle {
    prevent_destroy = true
  }
}

locals {
  api_domain = "events.${var.domain_name}"
}

# Free certificate for the API subdomain, kept between sessions with the zone (see teardown.sh).
resource "aws_acm_certificate" "api" {
  domain_name       = local.api_domain
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

# ACM proves we own the name by looking for this CNAME in the zone.
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for option in aws_acm_certificate.api.domain_validation_options : option.domain_name => {
      name   = option.resource_record_name
      type   = option.resource_record_type
      record = option.resource_record_value
    }
  }

  zone_id         = aws_route53_zone.main.zone_id
  name            = each.value.name
  type            = each.value.type
  records         = [each.value.record]
  ttl             = 60
  allow_overwrite = true
}

# Waits until ACM sees the CNAME, so it only finishes once the registrar points the domain to the zone's name servers.
resource "aws_acm_certificate_validation" "api" {
  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}
