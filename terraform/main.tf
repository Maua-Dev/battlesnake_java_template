# ---- configurando backend remoto para guardar o tfstate ------
terraform {

  backend "remote" {
    organization = "DevCoisas"

    workspaces {
      prefix = "battlesnakews-"
    }
    
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}


# ----- configurando o provedor -----
provider "aws" {
  region = "us-west-2"
}

# ------ configurando a funcao lambda ------

# politica para a funcao lambda
data "aws_iam_policy_document" "assume_role" {
  statement {
    effect = "Allow"
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
    actions = ["sts:AssumeRole"]
  }
}

# permissao
resource "aws_iam_role" "lambda_role_name" {
  name               = "lambda_role_battlesnake-${var.project_name}_${var.environment}"
  assume_role_policy = data.aws_iam_policy_document.assume_role.json
}

# anexando a permissao a uma politica eu acho
resource "aws_iam_role_policy_attachment" "lambda_exec_policy" {
  role       = aws_iam_role.lambda_role_name.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# criando a funcao lambda
resource "aws_lambda_function" "lambda_battle_snake_java" {
  function_name = "${var.project_name}-lambda-${var.environment}"

  filename = "../target/battlesnake-lambda-1.0.jar"

  role    = aws_iam_role.lambda_role_name.arn
  handler = "com.mauadev.code.Handler::handleRequest"
  runtime = "java17"

  source_code_hash = filebase64sha256("../target/battlesnake-lambda-1.0.jar")
}


# ---- configurando o api gateway ----

# instanciando o api gateway
resource "aws_api_gateway_rest_api" "api" {
  name        = "api-battlesnake-${var.project_name}-${var.environment}"
  description = "API para a funcao lambda do battle snake"
}

# uma rota comum "/endpoint"
resource "aws_api_gateway_resource" "resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "{proxy+}" #camptura qualquer caminho
}

# metodo para o endpoint
resource "aws_api_gateway_method" "method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.resource.id
  http_method   = "ANY" #qualquer metodo
  authorization = "NONE"
}

resource "aws_api_gateway_method" "root_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_rest_api.api.root_resource_id # aponta para a raiz
  http_method   = "GET"
  authorization = "NONE"
}

# configurando a interacao do api gateway com os recursos / metodos eu acho
resource "aws_api_gateway_integration" "integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.resource.id
  http_method             = aws_api_gateway_method.method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.lambda_battle_snake_java.invoke_arn
}

resource "aws_api_gateway_integration" "root_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_method.root_method.resource_id
  http_method             = aws_api_gateway_method.root_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.lambda_battle_snake_java.invoke_arn
}

# adicionando permiossoes na lambda para o apigateway poder executala
resource "aws_lambda_permission" "api_gateway_permission" {
  statement_id  = "AllowAPIGatewayToInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.lambda_battle_snake_java.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}

# nao sei
resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.resource.id,
      aws_api_gateway_method.method.id,
      aws_api_gateway_integration.integration.id,
    ]))
  }
  lifecycle {
    create_before_destroy = true
  }
}

# configurando o stage
resource "aws_api_gateway_stage" "stage" {
  deployment_id = aws_api_gateway_deployment.deployment.id
  rest_api_id   = aws_api_gateway_rest_api.api.id
  stage_name    = "dev"
}

# output da url
output "api_url_base" {
  value       = aws_api_gateway_stage.stage.invoke_url
  description = "URL base da API. Use esta URL no site do Battlesnake."
}