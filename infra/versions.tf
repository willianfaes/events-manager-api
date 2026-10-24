terraform {
  required_version = ">= 1.10"

  # The bucket is created by hand (see README.md): it cannot live in the state it stores.
  backend "s3" {
    bucket       = "events-manager-tfstate-164106395660"
    key          = "events-manager/terraform.tfstate"
    region       = "eu-north-1"
    use_lockfile = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region
}
