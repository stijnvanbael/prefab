package be.appify.prefab.processor.terraform.gcp;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.ClassManifest;
import be.appify.prefab.processor.PrefabContext;
import java.io.IOException;
import java.util.List;
import javax.annotation.processing.FilerException;
import javax.lang.model.element.Element;
import javax.tools.StandardLocation;

class GcpTerraformWriter {

    private final PrefabContext context;

    GcpTerraformWriter(PrefabContext context) {
        this.context = context;
    }

    void write(List<ClassManifest> manifests) {
        boolean hasPostgres = isOnClasspath("org.springframework.data.relational.core.mapping.Table");
        boolean hasMongodb = isOnClasspath("org.springframework.data.mongodb.core.MongoTemplate");
        boolean hasPubSub = isOnClasspath("com.google.cloud.spring.pubsub.core.PubSubTemplate");
        boolean hasCloudSql = hasPostgres;
        boolean hasRestEndpoints = hasRestEndpoints(manifests);

        var elements = manifests.stream()
                .map(m -> (Element) m.type().asElement())
                .toArray(Element[]::new);

        writeFile("main.tf", mainTf(), elements);
        writeFile("variables.tf", variablesTf(hasRestEndpoints), elements);
        writeFile("outputs.tf", outputsTf(), elements);
        writeFile("cloud_run.tf", cloudRunTf(), elements);
        writeFile("artifact_registry.tf", artifactRegistryTf(), elements);
        writeFile("iam.tf", iamTf(hasCloudSql, hasPubSub), elements);

        if (hasCloudSql) {
            writeFile("cloud_sql.tf", cloudSqlTf(), elements);
            writeFile("vpc.tf", vpcTf(), elements);
        }
        if (hasMongodb) {
            writeFile("firestore.tf", firestoreTf(), elements);
        }
        if (hasPubSub) {
            writeFile("pubsub.tf", pubsubTf(), elements);
        }
        if (hasRestEndpoints) {
            writeFile("load_balancer.tf", loadBalancerTf(), elements);
        }
    }

    private boolean isOnClasspath(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean hasRestEndpoints(List<ClassManifest> manifests) {
        return manifests.stream().anyMatch(m ->
                !m.constructorsWith(Create.class).isEmpty()
                        || !m.methodsWith(GetById.class).isEmpty()
                        || !m.methodsWith(GetList.class).isEmpty()
                        || !m.methodsWith(Delete.class).isEmpty()
                        || !m.methodsWith(Update.class).isEmpty()
        );
    }

    private List<String> pubsubTopics() {
        return context.roundEnvironment().getElementsAnnotatedWith(Event.class)
                .stream()
                .map(e -> e.getAnnotation(Event.class).topic())
                .distinct()
                .sorted()
                .toList();
    }

    private void writeFile(String filename, String content, Element... elements) {
        try {
            var file = context.processingEnvironment().getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "terraform/gcp/" + filename, elements);
            try (var writer = file.openWriter()) {
                writer.write(content);
            }
        } catch (FilerException e) {
            // File already written in a previous processing round; skip.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String mainTf() {
        return """
                terraform {
                  required_version = ">= 1.5"
                  required_providers {
                    google = {
                      source  = "hashicorp/google"
                      version = "~> 5.0"
                    }
                  }
                }

                provider "google" {
                  project = var.project_id
                  region  = var.region
                }
                """;
    }

    private String variablesTf(boolean hasRestEndpoints) {
        var sb = new StringBuilder();
        sb.append("""
                variable "project_id" {
                  description = "The GCP project ID"
                  type        = string
                }

                variable "region" {
                  description = "The GCP region"
                  type        = string
                  default     = "europe-west1"
                }

                variable "app_name" {
                  description = "Application name used for resource naming"
                  type        = string
                }

                variable "image_tag" {
                  description = "Docker image tag to deploy"
                  type        = string
                  default     = "latest"
                }
                """);
        if (hasRestEndpoints) {
            sb.append("""

                    variable "domain" {
                      description = "The domain name for the HTTPS load balancer"
                      type        = string
                    }
                    """);
        }
        return sb.toString();
    }

    private String outputsTf() {
        return """
                output "cloud_run_url" {
                  description = "The URL of the deployed Cloud Run service"
                  value       = google_cloud_run_v2_service.app.uri
                }

                output "artifact_registry_repository" {
                  description = "The Artifact Registry repository URL"
                  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.app.repository_id}"
                }
                """;
    }

    private String cloudRunTf() {
        return """
                resource "google_cloud_run_v2_service" "app" {
                  name     = var.app_name
                  location = var.region

                  template {
                    service_account = google_service_account.app.email

                    containers {
                      image = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.app.repository_id}/${var.app_name}:${var.image_tag}"

                      resources {
                        limits = {
                          cpu    = "1"
                          memory = "512Mi"
                        }
                      }

                      env {
                        name  = "SPRING_PROFILES_ACTIVE"
                        value = "production"
                      }
                    }
                  }
                }

                resource "google_cloud_run_v2_service_iam_member" "public_access" {
                  name     = google_cloud_run_v2_service.app.name
                  location = google_cloud_run_v2_service.app.location
                  role     = "roles/run.invoker"
                  member   = "allUsers"
                }
                """;
    }

    private String artifactRegistryTf() {
        return """
                resource "google_artifact_registry_repository" "app" {
                  location      = var.region
                  repository_id = var.app_name
                  format        = "DOCKER"
                }
                """;
    }

    private String iamTf(boolean hasCloudSql, boolean hasPubSub) {
        var sb = new StringBuilder();
        sb.append("""
                resource "google_service_account" "app" {
                  account_id   = var.app_name
                  display_name = "${var.app_name} service account"
                }
                """);
        if (hasCloudSql) {
            sb.append("""

                    resource "google_project_iam_member" "app_cloudsql" {
                      project = var.project_id
                      role    = "roles/cloudsql.client"
                      member  = "serviceAccount:${google_service_account.app.email}"
                    }
                    """);
        }
        if (hasPubSub) {
            sb.append("""

                    resource "google_project_iam_member" "app_pubsub_publisher" {
                      project = var.project_id
                      role    = "roles/pubsub.publisher"
                      member  = "serviceAccount:${google_service_account.app.email}"
                    }

                    resource "google_project_iam_member" "app_pubsub_subscriber" {
                      project = var.project_id
                      role    = "roles/pubsub.subscriber"
                      member  = "serviceAccount:${google_service_account.app.email}"
                    }
                    """);
        }
        sb.append("""

                resource "google_project_iam_member" "app_secret_accessor" {
                  project = var.project_id
                  role    = "roles/secretmanager.secretAccessor"
                  member  = "serviceAccount:${google_service_account.app.email}"
                }
                """);
        return sb.toString();
    }

    private String cloudSqlTf() {
        return """
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
                """;
    }

    private String firestoreTf() {
        return """
                resource "google_firestore_database" "app" {
                  name        = var.app_name
                  location_id = var.region
                  type        = "FIRESTORE_NATIVE"
                }
                """;
    }

    private String pubsubTf() {
        var sb = new StringBuilder();
        for (var topic : pubsubTopics()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("""
                    resource "google_pubsub_topic" "%s" {
                      name = "%s"
                    }

                    resource "google_pubsub_subscription" "%s" {
                      name  = "%s-subscription"
                      topic = google_pubsub_topic.%s.name

                      ack_deadline_seconds = 20
                    }
                    """.formatted(topic, topic, topic, topic, topic));
        }
        return sb.toString();
    }

    private String vpcTf() {
        return """
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
                """;
    }

    private String loadBalancerTf() {
        return """
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
                """;
    }
}
