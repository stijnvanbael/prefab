resource "google_compute_network" "app" {
  name                    = var.app_name
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "app" {
  name          = var.app_name
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.app.id
}

resource "google_vpc_access_connector" "app" {
  name          = var.app_name
  region        = var.region
  subnet {
    name = google_compute_subnetwork.app.name
  }
}

resource "google_compute_global_address" "private_ip" {
  name          = "${var.app_name}-private-ip"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.app.id
}

resource "google_service_networking_connection" "private_vpc" {
  network                 = google_compute_network.app.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip.name]
}
