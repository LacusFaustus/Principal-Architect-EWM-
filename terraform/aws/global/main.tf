provider "aws" {
  region = "us-east-1"
  alias  = "us-east-1"
}

provider "aws" {
  region = "eu-west-1"
  alias  = "eu-west-1"
}

provider "aws" {
  region = "ap-southeast-1"
  alias  = "ap-southeast-1"
}

# Global Accelerator
resource "aws_globalaccelerator_accelerator" "ewm" {
  name            = "ewm-accelerator"
  ip_address_type = "IPV4"
  enabled         = true

  attributes {
    flow_logs_enabled = true
    flow_logs_s3_bucket = "ewm-logs"
    flow_logs_s3_prefix = "global-accelerator"
  }
}

resource "aws_globalaccelerator_listener" "ewm" {
  accelerator_arn = aws_globalaccelerator_accelerator.ewm.id
  client_affinity = "SOURCE_IP"
  protocol        = "TCP"

  port_range {
    from_port = 80
    to_port   = 80
  }

  port_range {
    from_port = 443
    to_port   = 443
  }
}

# Endpoint Groups
resource "aws_globalaccelerator_endpoint_group" "us" {
  listener_arn = aws_globalaccelerator_listener.ewm.id
  endpoint_group_region = "us-east-1"

  endpoint_configuration {
    endpoint_id = aws_lb.ewm_us.arn
    weight      = 100
    client_ip_preservation_enabled = true
  }

  health_check_port = 8080
  health_check_protocol = "HTTP"
  health_check_path = "/actuator/health"
  health_check_interval_seconds = 30
  threshold_count = 3
}

resource "aws_globalaccelerator_endpoint_group" "eu" {
  listener_arn = aws_globalaccelerator_listener.ewm.id
  endpoint_group_region = "eu-west-1"

  endpoint_configuration {
    endpoint_id = aws_lb.ewm_eu.arn
    weight      = 100
    client_ip_preservation_enabled = true
  }

  health_check_port = 8080
  health_check_protocol = "HTTP"
  health_check_path = "/actuator/health"
  health_check_interval_seconds = 30
  threshold_count = 3
}

# Route 53 Geolocation Routing
resource "aws_route53_record" "api_na" {
  zone_id = var.route53_zone_id
  name    = "api.ewm.example.com"
  type    = "A"

  geo_location_routing_policy {
    continent = "NA"
  }

  set_identifier = "us-east-1"
  alias {
    name                   = aws_globalaccelerator_accelerator.ewm.dns_name
    zone_id               = aws_globalaccelerator_accelerator.ewm.zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "api_eu" {
  zone_id = var.route53_zone_id
  name    = "api.ewm.example.com"
  type    = "A"

  geo_location_routing_policy {
    continent = "EU"
  }

  set_identifier = "eu-west-1"
  alias {
    name                   = aws_globalaccelerator_accelerator.ewm.dns_name
    zone_id               = aws_globalaccelerator_accelerator.ewm.zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "api_asia" {
  zone_id = var.route53_zone_id
  name    = "api.ewm.example.com"
  type    = "A"

  geo_location_routing_policy {
    continent = "AS"
  }

  set_identifier = "ap-southeast-1"
  alias {
    name                   = aws_globalaccelerator_accelerator.ewm.dns_name
    zone_id               = aws_globalaccelerator_accelerator.ewm.zone_id
    evaluate_target_health = false
  }
}