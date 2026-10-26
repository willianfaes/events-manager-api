# infra

Terraform for the AWS host, ECR, IAM and Cognito. Run it from WSL, in this directory.

## Remote state

The state lives in S3 (`versions.tf`, `use_lockfile = true`, Terraform >= 1.10). The bucket is created by hand,
once: it cannot live in the state it stores.

    aws s3api create-bucket --bucket events-manager-tfstate-<account-id> --region eu-north-1 \
      --create-bucket-configuration LocationConstraint=eu-north-1
    aws s3api put-bucket-versioning --bucket events-manager-tfstate-<account-id> \
      --versioning-configuration Status=Enabled

New buckets are already encrypted (SSE-S3) and block public access, so nothing else is needed. Versioning keeps
every previous state. Then `terraform init` (or `terraform init -migrate-state` from a local state).

## Cognito

`cognito.tf` creates the user pool, the hosted login page, the Swagger app client and the `USER` and `ADMIN`
groups. Users are created by an admin (no public sign-up).

- Demo user: `demo@example.com` / `Demo1234!` (group `USER`). The password is public on purpose.
- Demo admin: `admin-demo@example.com`, only created when `demo_admin_password` is set in `terraform.tfvars`.
- Load-test users for k6: `load-test-user-1@example.com` to `load-test-user-200@example.com` (group `USER`), only
  created when `load_test_password` is set in `terraform.tfvars`. Put the same value in `LOAD_TEST_PASSWORD` in `.env`.
  The pool policy applies: at least 8 characters with lowercase, uppercase, a number and a symbol. Create only them
  (a plain `apply` also brings up the EC2 stack):

      terraform apply -target=aws_cognito_user.load_test -target=aws_cognito_user_in_group.load_test
- The client only accepts the callback `http://localhost:8080/swagger-ui/oauth2-redirect.html`: Cognito requires
  HTTPS for any other host, so login from the deployed Swagger waits for a domain.

Get a token without a browser:

    aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH --client-id <cognito_client_id> \
      --auth-parameters USERNAME=demo@example.com,PASSWORD='Demo1234!'

## CI credentials

The deploy job in `.github/workflows/ci.yml` signs in to AWS with the access key of the IAM user `events-ci`
(`cicd.tf`), stored as the repository secrets `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`. OIDC would avoid
storing a key, but the organization SCP denies creating the OIDC provider. The user can only push to the ECR
repository and run `ssm:SendCommand` on the host.

The key is created by hand, once, so the secret never reaches the Terraform state or the terminal. From the
repository directory, with `aws` and `gh` logged in:

    read -r ID SECRET < <(aws iam create-access-key --user-name events-ci \
      --query 'AccessKey.[AccessKeyId,SecretAccessKey]' --output text)
    printf %s "$ID" | gh secret set AWS_ACCESS_KEY_ID
    printf %s "$SECRET" | gh secret set AWS_SECRET_ACCESS_KEY
    unset ID SECRET

To rotate it (a user has at most two keys): run the commands above, check that a deploy works, then delete the
old key with `aws iam list-access-keys --user-name events-ci` and
`aws iam delete-access-key --user-name events-ci --access-key-id <old-key-id>`.

## Teardown

The pool and the CI user are kept between sessions. A new pool changes the issuer and the `sub` of every user, which
orphans the profiles in the database; destroying the user would kill the key stored in GitHub. A plain
`terraform destroy` fails on purpose (`prevent_destroy`). Use `infra/teardown.sh`, which destroys everything except
Cognito and the CI user.
