CREATE TABLE account_details (
   id BIGSERIAL PRIMARY KEY,
   created_at TIMESTAMP NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMP DEFAULT NOW(),

   registered_phone VARCHAR(20),
   registered_email VARCHAR(255),
   account_number VARCHAR(20) NOT NULL,

   disabled BOOLEAN NOT NULL DEFAULT FALSE,

   account_type VARCHAR(50) NOT NULL,

   issuing_institution_id BIGINT NOT NULL REFERENCES financial_institutions(id) ON DELETE RESTRICT,
   customer_user_id BIGINT REFERENCES customer_users(id) ON DELETE CASCADE,

   card JSONB
);
