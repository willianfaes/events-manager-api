#!/bin/bash
# Destroys everything in the state except the Cognito pool and what hangs from it (client, domain, groups, users), the
# CI user with its policy (its access key is stored in GitHub, so destroying the user would kill the key) and the
# domain identity: the Route 53 zone, the ACM certificate and its validation record.
#   infra/teardown.sh [terraform destroy options, e.g. -auto-approve]
# Terraform has no "everything but this" option, so the targets are built from the state: a resource added later is
# included without touching the script. A plain `terraform destroy` fails on purpose (prevent_destroy on the pool, on
# the CI user and on the zone): a new pool changes the issuer and the `sub` of every user, and a new zone gets new name
# servers that would have to be set again at the registrar.
set -euo pipefail

cd "$(dirname "$0")"

mapfile -t RESOURCES < <(terraform state list | grep -v -e '^data\.' -e '^aws_cognito_' -e '^aws_iam_user' \
  -e '^aws_route53_zone\.' -e '^aws_acm_certificate' -e '^aws_route53_record\.cert_validation')

if [ "${#RESOURCES[@]}" -eq 0 ]; then
  echo "Nothing to destroy outside Cognito and the CI user."
  exit 0
fi

TARGETS=()
for RESOURCE in "${RESOURCES[@]}"; do
  TARGETS+=("-target=${RESOURCE}")
done

terraform destroy "${TARGETS[@]}" "$@"
