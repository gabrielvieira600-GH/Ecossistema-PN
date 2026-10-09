CREATE TABLE commercial_request (
 id VARCHAR(36) PRIMARY KEY,
 fair_id VARCHAR(40) NOT NULL REFERENCES fair(id),
 user_id VARCHAR(36) NOT NULL REFERENCES app_user(id),
 booth_ids TEXT NOT NULL,
 request_type VARCHAR(20) NOT NULL CHECK (request_type IN ('CONTRACT','WAITLIST')),
 details TEXT NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'PENDING_EMAIL',
 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 emailed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX commercial_request_fair_date_idx ON commercial_request(fair_id,created_at DESC);
