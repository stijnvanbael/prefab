resource "google_compute_global_address" "app" {
  name = var.app_name
}

resource "google_compute_managed_ssl_certificate" "app" {
  name = var.app_name

  managed {
    domains = [var.domain]
  }
}

resource "google_compute_region_network_endpoint_group" "app" {
  name                  = var.app_name
  network_endpoint_type = "SERVERLESS"
  region                = var.region

  cloud_run {
    service = google_cloud_run_v2_service.app.name
  }
}

resource "google_compute_backend_service" "app" {
  name = var.app_name

  backend {
    group = google_compute_region_network_endpoint_group.app.id
  }
}

resource "google_compute_url_map" "app" {
  name            = var.app_name
  default_service = google_compute_backend_service.app.id
}

resource "google_compute_target_https_proxy" "app" {
  name             = var.app_name
  url_map          = google_compute_url_map.app.id
  ssl_certificates = [google_compute_managed_ssl_certificate.app.id]
}

resource "google_compute_global_forwarding_rule" "app" {
  name       = var.app_name
  target     = google_compute_target_https_proxy.app.id
  port_range = "443"
  ip_address = google_compute_global_address.app.address
}
