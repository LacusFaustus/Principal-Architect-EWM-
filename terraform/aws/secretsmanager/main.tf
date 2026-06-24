# providers.tf
provider "aws" {
  region = var.aws_region
}

# secrets.tf
resource "aws_secretsmanager_secret" "database" {
  name = "ewm/database"
  description = "Database credentials for EWM platform"

  rotation_rules {
    automatically_after_days = 30
  }

  tags = {
    Environment = "production"
    Project     = "ewm"
  }
}

resource "aws_secretsmanager_secret_version" "database_v1" {
  secret_id = aws_secretsmanager_secret.database.id
  secret_string = jsonencode({
    username = var.db_username
    password = var.db_password
    host     = var.db_host
    port     = var.db_port
    database = var.db_name
  })
}

resource "aws_secretsmanager_secret" "redis" {
  name = "ewm/redis"
  description = "Redis credentials for EWM platform"

  rotation_rules {
    automatically_after_days = 30
  }

  tags = {
    Environment = "production"
    Project     = "ewm"
  }
}

resource "aws_secretsmanager_secret_version" "redis_v1" {
  secret_id = aws_secretsmanager_secret.redis.id
  secret_string = jsonencode({
    host     = var.redis_host
    port     = var.redis_port
    password = var.redis_password
  })
}

resource "aws_secretsmanager_secret" "jwt" {
  name = "ewm/jwt"
  description = "JWT secret for EWM platform"

  rotation_rules {
    automatically_after_days = 90
  }

  tags = {
    Environment = "production"
    Project     = "ewm"
  }
}

resource "aws_secretsmanager_secret_version" "jwt_v1" {
  secret_id = aws_secretsmanager_secret.jwt.id
  secret_string = jsonencode({
    secret = random_password.jwt_secret.result
  })
}

# Generate random password for JWT
resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

# IAM policies for EKS to access Secrets Manager
resource "aws_iam_policy" "secrets_manager_access" {
  name = "ewm-secrets-manager-access"
  description = "Allow EKS to read secrets from AWS Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = [
          aws_secretsmanager_secret.database.arn,
          aws_secretsmanager_secret.redis.arn,
          aws_secretsmanager_secret.jwt.arn
        ]
      }
    ]
  })
}

# Attach policy to IAM role used by EKS
resource "aws_iam_role_policy_attachment" "eks_secrets_manager" {
  role       = var.eks_worker_role_name
  policy_arn = aws_iam_policy.secrets_manager_access.arn
}

# variables.tf
variable "aws_region" {
  description = "AWS region"
  default     = "us-east-1"
}

variable "db_username" {
  description = "Database username"
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  sensitive   = true
}

variable "db_host" {
  description = "Database host"
}

variable "db_port" {
  description = "Database port"
  default     = 5432
}

variable "db_name" {
  description = "Database name"
  default     = "ewm"
}

variable "redis_host" {
  description = "Redis host"
}

variable "redis_port" {
  description = "Redis port"
  default     = 6379
}

variable "redis_password" {
  description = "Redis password"
  sensitive   = true
}

variable "eks_worker_role_name" {
  description = "EKS worker node IAM role name"
}

# outputs.tf
output "secret_arns" {
  description = "ARNs of created secrets"
  value = {
    database = aws_secretsmanager_secret.database.arn
    redis    = aws_secretsmanager_secret.redis.arn
    jwt      = aws_secretsmanager_secret.jwt.arn
  }
}