# II. THIẾT KẾ CƠ SỞ DỮ LIỆU
## 1. Sơ đồ ERD

![ERD Picture](/images/erdpicture.png)

### Chi tiết trường dư liệu cho từng bảng

#### 1. Bảng User

|Column|Type|Constraints|Description|
|---|---|---|---|
|id|BIGINT|PK, AUTO_INCREMENT|ID tự tăng|
|username|VARCHAR(50)|NOT NULL, UNIQUE|Tên đăng nhập, duy nhất toàn hệ thống|
|password|VARCHAR(255)|NOT NULL|Mật khẩu đã hash một chiều|
|role|ENUM('USER', 'ADMIN')|NOT NULL, DEFAULT 'USER'|Phân quyền: USER hoặc ADMIN|
|created_at|TIMESTAMP|NOT NULL, DEFAULT NOW()|Thời điểm tạo tài khoản|
|updated_at|TIMESTAMP|NOT NULL, DEFAULT NOW() ON UPDATE NOW()|Thời điểm cập nhật tài khoản|

#### 2. Bảng Wallet

|Column|Type|Constraints|Description|
|---|---|---|---|
|id|BIGINT|PK, AUTO_INCREMENT|ID tự tăng|
|user_id|BIGINT|FK -> USERS.id, NOT NULL, UNIQUE|Mỗi user chỉ có 1 ví (1:1)|
|balance|DECIMAL(18,2)|NOT NULL, DEFAULT 0.00|Số dư ví|
|version|BIGINT|NOT NULL, DEFAULT 0|Version cho Optimistic Lock hoặc tracking|
|created_at|TIMESTAMP|NOT NULL, DEFAULT NOW()|Thời điểm tạo ví|
|updated_at|TIMESTAMP|NOT NULL, DEFAULT NOW() ON UPDATE NOW()|Thời điểm cập nhật ví|

#### 3. Bảng Transactions

|Column|Type|Constraints|Description|
|---|---|---|---|
|id|BIGINT|PK, AUTO_INCREMENT|ID tự tăng|
|reference_id|VARCHAR(36)|UNIQUE, NOT NULL| UUID định danh duy nhất, dùng cho client tracking|
|self_wallet_id|BIGINT|FK -> WALLETS.id, NOT NULL| Ví chủ giao dịch|
|wallet_id|BIGINT|FK -> WALLETS.id, NULLABLE| Ví đối tác giao dịch|
|type|ENUM('TOPUP', 'TRANSFER', 'REVERSAL')|NOT NULL|Loại giao dịch|
|direction|ENUM('IN', 'OUT')|NOT NULL|Hướng giao dịch: IN hoặc OUT|
|status|ENUM('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')|NOT NULL, DEFAULT 'PENDING'|Trạng thái giao dịch|
|amount|DECIMAL(18,2)|NOT NULL|Số tiền gốc (người nhận hưởng)|
|fee|DECIMAL(18,2)|NOT NULL, DEFAULT 0.00|Phí giao dịch|
|total_amount|DECIMAL(18,2)|NOT NULL|Tổng trừ = amount + fee|
|balance_before|DECIMAL(18,2)|NOT NULL|Số dư ví trước giao dịch|
|balance_after|DECIMAL(18,2)|NOT NULL|Số dư ví sau giao dịch|
|original_transaction_id|BIGINT|FK -> TRANSACTIONS.id, NULLABLE|Giao dịch gốc (đối với REVERSAL)|
|description|VARCHAR(255)|NULLABLE|Mô tả giao dịch|
|failure_reason|VARCHAR(255)|NULLABLE|Lý do thất bại (Nếu status = FAILED)|
|created_at|TIMESTAMP|NOT NULL, DEFAULT NOW()|Thời điểm tạo giao dịch (phải __bất biến__)|  
|updated_at|TIMESTAMP|NOT NULL, DEFAULT NOW() ON UPDATE NOW()|Thời điểm cập nhật giao dịch (cập nhập lần cuối)|

#### 4. Bảng IDEMPOTENCY_KEYS - Lưu trên Redis

|Column|Type|Constraints|Description|
|---|---|---|---|
|idempotency_keys|VARCHAR(255)|PK|Key từ Header Idempotency-key|
|user_id|BIGINT|NOT NULL| ID user gửi request|
|transaction_id|BIGINT|FK -> TRANSACTIONS.id|ID giao dịch đã xử lý|
|response_body|TEXT|NOT NULL|Cached response JSON|
|response_status|INT|NOT NULL| HTTP status code đã trả|
|created_at|TIMESTAMP|NOT NULL| Thời điểm tạo|

### Quan hệ giữa các bảng

|Quan hệ|Số lượng|Mô tả|
|---|---|---|
|USERS->WALLETS|1:0..1|Mỗi USER có tối đa 1 ví. ADMIN không có ví|
|WALLETS->TRANSACTIONS|1:N|Mỗi ví phát sinh nhiều giao dịch theo thời gian|
|TRANSACTIONS->TRANSACTIONS|1:0..1|Giao dịch REVERSAL tham chiếu đến giao dịch gốc thông qua original_transaction_id|
|IDEMPOTENCY_KEYS → TRANSACTIONS|N:1|Mỗi key liên kết đến 1 giao dịch đã xử lý|