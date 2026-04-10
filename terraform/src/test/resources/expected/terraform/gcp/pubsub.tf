resource "google_pubsub_topic" "TOPIC_NAME" {
  name = "TOPIC_NAME"
}

resource "google_pubsub_subscription" "TOPIC_NAME" {
  name  = "TOPIC_NAME-subscription"
  topic = google_pubsub_topic.TOPIC_NAME.name

  ack_deadline_seconds = 20
}
