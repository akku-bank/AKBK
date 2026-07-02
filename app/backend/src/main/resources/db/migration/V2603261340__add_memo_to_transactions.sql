/*
 * Flyway Migration: V2603261340__add_memo_to_transactions.sql
 * Purpose: Add memo column to transactions table for personalized transaction notes.
 */
ALTER TABLE transactions ADD COLUMN memo VARCHAR(255);
