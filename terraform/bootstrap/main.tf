
terraform {
  backend "s3" {
    bucket         = "battle-snake-bootstrap-state-new-account"
    dynamodb_table = "battle-snake-bootstrap-state-table-new-account"
    region         = "us-east-1"
    encrypt        = true
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1" 
}

variable "project_name" {
  description = "Nome do projeto para o backend (ex: battlesnake)."
  type        = string
  default     = "battlesnake"
}

variable "environment" {
  description = "Ambiente para o backend (ex: tfstate)."
  type        = string
  default     = "tfstate"
}

resource "aws_s3_bucket" "terraform_state" {
  bucket = "battlesnake-java-template-terraform-state-tfstate-new-account"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "versioning_example" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "block_public" {
  bucket                  = aws_s3_bucket.terraform_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "terraform_locks" {
  name         = "battlesnake-java-template-terraform-locks-tfstate-new-account"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }
}

output "s3_bucket_name" {
  description = "O nome do bucket S3 criado para o estado do Terraform."
  value       = aws_s3_bucket.terraform_state.id
}

output "dynamodb_table_name" {
  description = "O nome da tabela DynamoDB para travamento de estado."
  value       = aws_dynamodb_table.terraform_locks.name
}