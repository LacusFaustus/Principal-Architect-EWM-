provider "aws" {
  region = var.region
}

# VPC
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support = true

  tags = {
    Name = "ewm-${var.region}"
  }
}

# EKS Cluster
resource "aws_eks_cluster" "main" {
  name     = "ewm-${var.region}"
  role_arn = aws_iam_role.eks.arn
  version  = "1.28"

  vpc_config {
    subnet_ids = aws_subnet.private[*].id
  }

  tags = {
    Environment = "production"
    Region      = var.region
  }
}

# RDS PostgreSQL
resource "aws_db_instance" "main" {
  identifier = "ewm-${var.region}"
  engine     = "postgres"
  engine_version = "15.4"
  instance_class = var.db_instance_class
  allocated_storage = 100
  storage_encrypted = true
  storage_type = "gp3"

  db_name  = "ewm"
  username = var.db_username
  password = var.db_password

  vpc_security_group_ids = [aws_security_group.db.id]
  db_subnet_group_name = aws_db_subnet_group.main.name
  multi_az = true

  backup_retention_period = 30
  backup_window = "03:00-04:00"
  maintenance_window = "sun:04:00-sun:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql"]

  tags = {
    Environment = "production"
    Region      = var.region
  }
}

# ElastiCache Redis
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "ewm-${var.region}"
  description = "Redis cache for EWM"
  engine = "redis"
  engine_version = "7.1"
  node_type = var.redis_node_type
  num_cache_clusters = 3

  parameter_group_name = "default.redis7"
  port = 6379
  automatic_failover_enabled = true
  multi_az_enabled = true

  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  tags = {
    Environment = "production"
    Region      = var.region
  }
}

# Load Balancer
resource "aws_lb" "main" {
  name               = "ewm-${var.region}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb.id]
  subnets            = aws_subnet.public[*].id

  tags = {
    Environment = "production"
    Region      = var.region
  }
}