CREATE TABLE media_request (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL REFERENCES app_user(id),
  details TEXT NOT NULL,
  total DECIMAL(15,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  emailed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_media_request_created ON media_request(created_at);
