variable "project_name" {
    type = string
    description = "O nome único para esta instância do projeto. Será usado para nomear recursos e o workspace."
}

variable "environment" {
  type        = string
  description = "O ambiente de deploy (ex: 'dev', 'staging', 'prod')."
  default     = "dev"
}
