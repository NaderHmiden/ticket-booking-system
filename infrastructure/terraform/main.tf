terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "key_name" {
  type        = string
  description = "Name of an existing EC2 key pair"
  default     = "vockey" # Learner Lab default; change to your own key pair name
}

provider "aws" {
  region = var.aws_region
}

# Latest Ubuntu 22.04 LTS AMI from Canonical, valid in any region
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

resource "aws_security_group" "ticket_sg" {
  name        = "ticket-platform-sg"
  description = "Allow SSH and app traffic"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "app_server" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.medium" # Enough for Postgres/Redis/Kafka/Java
  key_name      = var.key_name

  vpc_security_group_ids = [aws_security_group.ticket_sg.id]

  tags = {
    Name        = "TicketPlatformServer"
    Environment = "Production"
  }
}

output "instance_public_ip" {
  value = aws_instance.app_server.public_ip
}