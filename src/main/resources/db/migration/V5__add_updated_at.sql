-- 1. Thêm cột updated_at
ALTER TABLE idempotency_keys 
ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- 2. Tạo function tự động cập nhật updated_at
CREATE OR REPLACE FUNCTION update_idempotency_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 3. Tạo trigger áp dụng cho bảng
CREATE TRIGGER trg_update_idempotency_time
    BEFORE UPDATE ON idempotency_keys
    FOR EACH ROW EXECUTE PROCEDURE update_idempotency_timestamp();