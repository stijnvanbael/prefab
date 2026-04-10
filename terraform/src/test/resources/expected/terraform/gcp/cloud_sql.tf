resource "google_sql_database_instance" "app" {
  name             = var.app_name
  database_version = "POSTGRES_15"
  region           = var.region

  settings {
    tier = "db-f1-micro"

    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = google_compute_network.app.id
      enable_private_path_for_google_cloud_services = true
    }
  }

  deletion_protection = true
}

resource "google_sql_database" "app" {
  name     = var.app_name
  instance = google_sql_database_instance.app.name
}

resource "google_sql_user" "app" {
  name     = var.app_name
  instance = google_sql_database_instance.app.name
  password = random_password.db_password.result
}

resource "random_password" "db_password" {
  length  = 32
  special = true
}

resource "google_secret_manager_secret" "db_password" {
  secret_id = "${var.app_name}-db-password"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_password.result
}
