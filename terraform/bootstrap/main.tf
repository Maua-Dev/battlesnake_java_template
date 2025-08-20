terraform {
  required_providers {
    tfe = {
      source  = "hashicorp/tfe"
      version = "~> 0.50.0"
    }
  }
}

provider "tfe" {
}

variable "workspace_name" {
  type        = string
  description = "O nome do workspace a ser criado no TFC."
}

variable "tfe_organization" {
  type    = string
  default = "DevCoisas"
}

variable "repo_identifier" {
  type        = string
  description = "O identificador completo do repositório no GitHub (ex: Owner/Repo)."
}

# Encontra o ID da conexão com o VCS (GitHub) automaticamente pelo nome
data "tfe_oauth_client" "github" {
  organization = var.tfe_organization
  name         = "GitHub" # Ou o nome que você deu para a sua conexão com o VCS
}

resource "tfe_workspace" "this" {
  name              = var.workspace_name
  organization      = var.tfe_organization
  auto_apply        = true
  working_directory = "terraform/app" # Aponta para onde o código da app está
  terraform_version = "1.6.6"

  vcs_repo {
    identifier     = var.repo_identifier # Ex: "SuaOrg/battlesnake-cliente-A"
    oauth_token_id = data.tfe_oauth_client.github.oauth_token_id
    branch         = "main"
  }
}