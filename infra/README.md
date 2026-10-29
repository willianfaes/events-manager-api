# infra

Terraform for the AWS host, ECR, IAM, Cognito and the DNS zone. Run it from WSL, in this directory.

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
groups. Anyone can sign up on the hosted page: Cognito e-mails a code (check the spam folder; the default sender is
limited to 50 e-mails a day) and the new user has no group, so they can call the API but not `/admin`. The users below
are created by an admin and marked as verified.

- Demo user: `demo@example.com` / `Demo1234!` (group `USER`). The password is public on purpose.
- Demo admin: `admin-demo@example.com`, only created when `demo_admin_password` is set in `terraform.tfvars`.
- Load-test users for k6: `load-test-user-1@example.com` to `load-test-user-200@example.com` (group `USER`), only
  created when `load_test_password` is set in `terraform.tfvars`. Put the same value in `LOAD_TEST_PASSWORD` in `.env`.
  The pool policy applies: at least 8 characters with lowercase, uppercase, a number and a symbol. Create only them
  (a plain `apply` also brings up the EC2 stack):

      terraform apply -target=aws_cognito_user.load_test -target=aws_cognito_user_in_group.load_test
- The client accepts two callbacks: `http://localhost:8080/swagger-ui/oauth2-redirect.html` and
  `https://events.<domain_name>/swagger-ui/oauth2-redirect.html` (Cognito requires HTTPS for any host but localhost).
  The pool is kept between sessions, so a change to the list needs
  `terraform apply -target=aws_cognito_user_pool_client.swagger`.

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

## Domain

The domain (`domain_name` in `variables.tf`) is bought outside AWS; `domain.tf` creates the Route 53 hosted zone that
answers for it. The zone is created once and the registrar is pointed to it once:

1. Create only the zone (a plain `apply` also brings up the EC2 stack):

       terraform apply -target=aws_route53_zone.main

2. Read the four name servers it was given:

       terraform output name_servers

3. At the registrar, switch the domain to custom name servers and enter those four. A `.dev` change can take from a
   few hours up to 48 hours to reach everyone.
4. Once the name servers answer (`dig NS <domain_name>` lists the four), create the certificate for
   `events.<domain_name>`:

       terraform apply -target=aws_acm_certificate_validation.api

   ACM checks a CNAME in the zone, so this step only finishes after step 3 has propagated. Run before that, it waits
   and then fails with a timeout; running it again is safe.

## Deploy

One EC2 host runs the API, Postgres and Redis with `docker-compose.prod.yml`. The pipeline publishes the image to ECR
and runs `deploy/deploy.sh` on the host through SSM: there is no SSH and the host never clones the repository.

First time, in this order (from this directory, in WSL):

1. **Parameters.** `deploy.sh` reads everything under `/events-manager/prod/` from Parameter Store and writes one
   `NAME=value` line per parameter into the host's `.env`. They are created by hand, so no secret reaches the
   Terraform state, and they survive the teardown. The names are the variables `docker-compose.prod.yml` expects:

       aws ssm put-parameter --name /events-manager/prod/DB_NAME --type String --value events_manager_db
       aws ssm put-parameter --name /events-manager/prod/DB_USER --type String --value events_manager_app
       aws ssm put-parameter --name /events-manager/prod/DB_PASSWORD --type SecureString --value "$(openssl rand -hex 16)"
       aws ssm put-parameter --name /events-manager/prod/COGNITO_ISSUER_URI --type String --value "$(terraform output -raw cognito_issuer_uri)"
       aws ssm put-parameter --name /events-manager/prod/COGNITO_HOSTED_UI_URL --type String --value "$(terraform output -raw cognito_hosted_ui_url)"
       aws ssm put-parameter --name /events-manager/prod/COGNITO_CLIENT_ID --type String --value "$(terraform output -raw cognito_client_id)"

   The password is hexadecimal on purpose: the `.env` has no quotes, so a `$` or a quote in it would break the
   compose file. Add `--overwrite` to change a value.
2. **Stack.** `terraform apply` creates the host, the load balancer, the listeners and the `events` record. From here
   on it costs money by the hour. The host needs a few minutes to run its first-boot script and register in SSM; it is
   ready when this lists it as `Online`:

       aws ssm describe-instance-information --query 'InstanceInformationList[].[InstanceId,PingStatus]' --output text
3. **Pipeline.** The deploy job only runs when the repository variable is `true`, and the workflow only runs on a push
   or a pull request, so turn the variable on and re-run the last run on `main`:

       gh variable set DEPLOY_ENABLED --body true
       gh run list --branch main --limit 1
       gh run rerun <run-id>
4. **Check.** `terraform output api_url` prints the address. On the host (the command cannot contain double quotes):

       ../deploy/ssm-run.sh "docker compose -f /opt/events-manager/docker-compose.prod.yml ps"
       ../deploy/ssm-run.sh "docker compose -f /opt/events-manager/docker-compose.prod.yml logs --tail 100 events-manager"

   In the browser: `http://` redirects to `https://`, the certificate is valid, Swagger UI opens, and the sign-up on
   the hosted page comes back to the Swagger UI of the domain.

To switch it off, run `./teardown.sh` and `gh variable set DEPLOY_ENABLED --body false`; otherwise every merge to
`main` fails its deploy job. The database lives on the host, so it is created again (and the example events seeded
again) the next time; the Cognito users stay, but their profiles are gone until they call `POST /users/me`.

## Teardown

The pool, the CI user and the domain identity (the hosted zone, the ACM certificate and its validation record) are
kept between sessions. A new pool changes the issuer and the `sub` of every user, which orphans the profiles in the
database; destroying the user would kill the key stored in GitHub; a new zone gets new name servers, which would have
to be set again at the registrar. A plain `terraform destroy` fails on purpose (`prevent_destroy`). Use
`infra/teardown.sh`, which destroys everything except those.
