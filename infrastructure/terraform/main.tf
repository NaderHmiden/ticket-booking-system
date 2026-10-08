terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "eu-west-3" # Paris
}

resource "aws_security_group" "ticket_sg" {
  name        = "ticket-platform-sg"
  description = "Allow SSH and HTTP traffic"

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
  ami           = "ami-00ac45f3035ff009e" # Ubuntu 22.04 LTS (eu-west-3)
  instance_type = "t3.medium" # Nécessaire pour faire tourner Postgres/Redis/Kafka/Java
  key_name      = "my-ssh-key" # Remplacez par le nom de votre clé AWS

  vpc_security_group_ids = [aws_security_group.ticket_sg.id]

  tags = {
    Name = "TicketPlatformServer"
    Environment = "Production"
  }
}

output "instance_public_ip" {
  value = aws_instance.app_server.public_ip
}
