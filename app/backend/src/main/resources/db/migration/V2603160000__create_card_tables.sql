-- 8. 카드 상품 (Card Products)
CREATE TABLE card_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_unique_no VARCHAR(20) UNIQUE NOT NULL,  
    card_issuer_code VARCHAR(10),
    card_issuer_name VARCHAR(100),
    card_name VARCHAR(255) NOT NULL,
    card_description VARCHAR(1000),
    card_type_code VARCHAR(10),
    card_type_name VARCHAR(50),
    base_limit_performance BIGINT,
    max_benefit_limit BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. 보유 카드 (Cards)
CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    card_product_id UUID NOT NULL,
    card_no VARCHAR(20) UNIQUE NOT NULL,
    cvc VARCHAR(3) NOT NULL,
    card_expiry_date VARCHAR(8) NOT NULL, 
    withdrawal_account_no VARCHAR(20),
    withdrawal_date VARCHAR(2), 
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
