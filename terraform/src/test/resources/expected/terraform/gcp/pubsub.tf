resource "google_pubsub_topic" "user_events" {
  name = "user_events"
}

resource "google_pubsub_subscription" "user_events" {
  name  = "user_events-subscription"
  topic = google_pubsub_topic.user_events.name

  ack_deadline_seconds = 20
}
