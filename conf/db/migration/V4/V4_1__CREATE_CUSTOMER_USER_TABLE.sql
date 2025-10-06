DROP TABLE IF EXISTS customer_users CASCADE;

CREATE TABLE customer_users (
   id BIGSERIAL PRIMARY KEY,
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMP DEFAULT NOW(),
   name VARCHAR(255) NOT NULL,
   disabled BOOLEAN NOT NULL DEFAULT FALSE,
   user_phone varchar(50) not null unique,
   user_email varchar(50),
   accounts JSONB NOT NULL,
   identity_uuid text unique not null
);