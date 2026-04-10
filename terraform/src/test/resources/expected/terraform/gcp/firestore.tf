resource "google_firestore_database" "app" {
  name        = var.app_name
  location_id = var.region
  type        = "FIRESTORE_NATIVE"
}
