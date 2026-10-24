#!/bin/bash
# Destroys everything in the state except the Cognito pool and what hangs from it (client, domain, groups, users).
#   infra/teardown.sh [terraform destroy options, e.g. -auto-approve]
# Terraform has no "everything but this" option, so the targets are built from the state: a resource added later is
# included without touching the script. A plain `terraform destroy` fails on purpose (prevent_destroy on the pool),
# because a new pool changes the issuer and the `sub` of every user.
set -euo pipefail

cd "$(dirname "$0")"

mapfile -t RESOURCES < <(terraform state list | grep -v -e '^data\.' -e '^aws_cognito_')

if [ "${#RESOURCES[@]}" -eq 0 ]; then
  echo "Nothing to destroy outside Cognito."
  exit 0
fi

TARGETS=()
for RESOURCE in "${RESOURCES[@]}"; do
  TARGETS+=("-target=${RESOURCE}")
done

terraform destroy "${TARGETS[@]}" "$@"
