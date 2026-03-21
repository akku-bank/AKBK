-- V2603200000__encrypt_cvc.sql
-- Increase CVC column length to accommodate encryption
ALTER TABLE cards ALTER COLUMN cvc TYPE VARCHAR(255);
