-- 편의점을 별도의 카테고리로 설정 편의점 카테고리 id -> 10
UPDATE merchant
SET sub_category_id = 10,
    sub_category_name = '편의점'
WHERE merchant_name LIKE '%GS25%'
   OR merchant_name LIKE '%CU%'
   OR merchant_name LIKE '%세븐일레븐%'
   OR merchant_name LIKE '%이마트24%';