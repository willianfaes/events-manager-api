# infra

Terraform for the AWS host, ECR and IAM. Run it from WSL, in this directory.

## Remote state

The state lives in S3 (`versions.tf`, `use_lockfile = true`, Terraform >= 1.10). The bucket is created by hand,
once: it cannot live in the state it stores.

    aws s3api create-bucket --bucket events-manager-tfstate-<account-id> --region eu-north-1 \
      --create-bucket-configuration LocationConstraint=eu-north-1
    aws s3api put-bucket-versioning --bucket events-manager-tfstate-<account-id> \
      --versioning-configuration Status=Enabled

New buckets are already encrypted (SSE-S3) and block public access, so nothing else is needed. Versioning keeps
every previous state. Then `terraform init` (or `terraform init -migrate-state` from a local state).
