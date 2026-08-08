resource "aws_secretsmanager_secret" "finnhub_api_key" {
  name                    = "${var.project_name}/FINNHUB_API_KEY"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "finnhub_api_key" {
  secret_id     = aws_secretsmanager_secret.finnhub_api_key.id
  secret_string = var.finnhub_api_key
}

resource "aws_secretsmanager_secret" "polygon_api_key" {
  name                    = "${var.project_name}/POLYGON_API_KEY"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "polygon_api_key" {
  secret_id = aws_secretsmanager_secret.polygon_api_key.id
  # Empty string is fine — AppConfig.java's PolygonClienqt alread
  # handles a blank key gracefully (chart endpoint returns []).
  secret_string = var.polygon_api_key == "" ? "unset" : var.polygon_api_key
}