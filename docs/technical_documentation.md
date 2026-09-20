# Tài Liệu Kỹ Thuật - Kiến Trúc Và Các Chức Năng Dự Án MiniWallet

Tài liệu này trình bày chi tiết về cách thiết lập, luồng xử lý và các cơ chế kỹ thuật được áp dụng trong dự án MiniWallet, tập trung vào Xác thực (Auth), tính Luỹ đẳng (Idempotency) kết hợp với Khóa Bi quan (Pessimistic Locking), và luồng nghiệp vụ của các chức năng chính.

---

## 1. Cơ Chế Xác Thực và Phân Quyền (Authentication & Authorization)

Hệ thống sử dụng **Spring Security** kết hợp với **JSON Web Token (JWT)** để đảm bảo an toàn cho các API. Cơ chế này được thiết kế theo hướng **Stateless** (không lưu phiên đăng nhập trên server), phù hợp với kiến trúc RESTful.

### 1.1. Cấu hình Security (`SecurityConfig.java`)
- **Vô hiệu hóa CSRF**: Vì hệ thống dùng JWT và stateless session, CSRF (Cross-Site Request Forgery) được vô hiệu hóa.
- **Session Management**: Thiết lập `SessionCreationPolicy.STATELESS`, server không tạo ra JSESSIONID, mỗi request đều phải tự chứng minh danh tính thông qua token.
- **Phân quyền Route**:
  - Các endpoint `/api/v1/auth/**` (đăng ký, đăng nhập) được cấu hình `permitAll()` cho phép truy cập tự do.
  - Các endpoint `/api/v1/admin/**` yêu cầu role `ADMIN`.
  - Toàn bộ các request còn lại yêu cầu phải được xác thực (`authenticated()`).

### 1.2. Bộ lọc JWT (`JwtAuthenticationFilter.java`)
- Filter này can thiệp vào mọi request (trừ các request public) để kiểm tra header `Authorization`.
- Nếu phát hiện token hợp lệ, nó sẽ trích xuất thông tin người dùng (`userId`, `username`, `role`), sau đó tạo một đối tượng `UsernamePasswordAuthenticationToken` và đưa vào `SecurityContextHolder`.

### 1.3. Mã hóa mật khẩu
- Sử dụng thuật toán hash một chiều **BCrypt** (`BCryptPasswordEncoder`) để mã hóa mật khẩu trước khi lưu vào cơ sở dữ liệu. Ngay cả khi lộ Database, mật khẩu người dùng vẫn được bảo vệ.

---

## 2. Cơ Chế Chống Trùng Lặp (Idempotency)

Trong hệ thống thanh toán, việc một user nhấn đúp nút (double-click) hoặc gửi lại request do lag mạng có thể dẫn đến việc bị trừ tiền hai lần. Để giải quyết, dự án áp dụng cơ chế **Idempotency** dựa trên **Redis**.

### 2.1. IdempotencyService
- Mỗi request quan trọng (Nạp tiền, Chuyển tiền) yêu cầu client đính kèm một `Idempotency-Key` (thường là UUID) trên header.
- **Khóa Key trên Redis (`lockKey`)**: Hệ thống cố gắng lưu key này vào Redis với trạng thái `PROCESSING` sử dụng lệnh `setIfAbsent` (tương đương `SETNX` trong Redis).
  - Nếu thành công (trả về `true`), đây là request đầu tiên, hệ thống tiếp tục xử lý.
  - Nếu thất bại (trả về `false` hoặc key đã tồn tại), hệ thống chặn lại và ném lỗi hoặc trả về kết quả cũ.
- **Lưu lại kết quả (`saveResponse`)**: Sau khi giao dịch (ví dụ: chuyển tiền) xử lý xong dưới Database, hệ thống serialize kết quả (DTO) và đè lên trạng thái `PROCESSING` của key đó trong Redis với thời gian sống (TTL) là 10 phút.
- **Đọc kết quả cũ (`getPreviousResponse`)**: Nếu một request đến và key đã có kết quả (khác `PROCESSING`), hệ thống trực tiếp trả về kết quả lấy từ Redis mà không chạy lại logic trừ/cộng tiền.

---

## 3. Quản Lý Đồng Thời với Khóa Bi Quan (Pessimistic Locking)

Để ngăn chặn Race Condition (Hai luồng cùng lúc trừ tiền của một ví), hệ thống áp dụng cơ chế khóa ở mức cơ sở dữ liệu.

### 3.1. `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- Tại `WalletRepository`, phương thức `findByUserIdWithPessimisticLock` được gắn annotation này.
- Khi truy vấn ví, Spring Data JPA sẽ sinh ra câu lệnh SQL dạng `SELECT * FROM wallets WHERE user_id = ? FOR UPDATE`.
- Hàng dữ liệu (row) của ví này trong Database sẽ bị khóa lại. Mọi request khác muốn tác động lên ví này đều phải xếp hàng chờ cho đến khi Transaction hiện tại `COMMIT` hoặc `ROLLBACK`.

### 3.2. Tránh Deadlock trong Chuyển Tiền
- Trong chức năng Chuyển tiền, cần khóa cùng lúc 2 ví: Ví nguồn và Ví đích.
- **Vấn đề**: Nếu User A chuyển cho User B (Khóa A trước, khóa B sau), cùng lúc đó User B chuyển cho User A (Khóa B trước, khóa A sau) -> Hệ thống sẽ gặp **Deadlock**.
- **Cách giải quyết**: Khóa theo thứ tự ID định sẵn. Trong `WalletServiceImpl.java`:
  ```java
  Long firstLockId = Math.min(userId, request.getToUserId());
  Long secondLockId = Math.max(userId, request.getToUserId());
  // Khóa ví có ID nhỏ trước
  Wallet firstWallet = walletRepository.findByUserIdWithPessimisticLock(firstLockId)...
  // Khóa ví có ID lớn sau
  Wallet secondWallet = walletRepository.findByUserIdWithPessimisticLock(secondLockId)...
  ```
  Nhờ logic này, luôn luôn một ví có ID nhỏ hơn được khóa trước, triệt tiêu hoàn toàn khả năng xảy ra vòng lặp chờ lẫn nhau (Deadlock).

---

## 4. Xây Dựng Các Chức Năng Cốt Lõi (`WalletServiceImpl.java`)

### 4.1. Chức năng Nạp Tiền (Topup)
1. **Kiểm tra Idempotency**: Dùng Redis để chặn trùng lặp.
2. **Khóa Ví**: Lấy thông tin ví và khóa bằng `FOR UPDATE`.
3. **Cộng tiền**: Cập nhật balance của ví.
4. **Lịch sử giao dịch (Transaction)**: Ghi lại một bản ghi giao dịch với loại `TOPUP`, chiều `IN`, trạng thái `COMPLETED` chứa chi tiết về số dư đầu cuối. Tất cả gói gọn trong một `@Transactional`.
5. **Lưu Idempotency**: Lưu kết quả trả về vào Redis.

### 4.2. Chức năng Chuyển Tiền (Transfer)
Chức năng này xử lý logic nghiệp vụ phức tạp nhất:
1. **Kiểm tra tự chuyển tiền**: Chặn user không được chuyển cho chính mình.
2. **Idempotency & Pessimistic Lock**: Chống lặp request và khóa 2 ví theo thứ tự an toàn (đã mô tả ở trên).
3. **Tính toán chi phí**:
   - Tỷ lệ phí là `0.5%`.
   - Áp dụng **Sàn phí**: Sử dụng `fee = amount.multiply(feeRate)` và hàm `compareTo` với mức tối thiểu là `1000 VNĐ`.
4. **Kiểm soát Hạn mức Ngày**:
   - Lấy tổng tiền đã chuyển (`sumAmountByFilters`) trong khoảng thời gian từ `00:00:00` đến cuối ngày của ngày hiện tại.
   - Nếu `Tổng đã chuyển + Số tiền muốn chuyển > 50.000.000 VNĐ`, hệ thống chặn giao dịch, lưu 1 bản ghi `FAILED` để lại dấu vết (`transactionRepository.save(failedTx)`), và báo lỗi `DailyLimitExceededException` (lỗi này được set `noRollbackFor` để bảo lưu được lịch sử FAILED).
5. **Cập nhật số dư**:
   - Trừ tiền và phí của Ví nguồn.
   - Cộng tiền cho Ví đích.
6. **Lịch sử Giao dịch**: Sinh ra tới 3 bản ghi transaction trong một lần chuyển:
   - 1 bản ghi `TRANSFER OUT` cho người gửi.
   - 1 bản ghi `FEE` (chiều `OUT`) tách biệt khoản phí.
   - 1 bản ghi `TRANSFER IN` cho người nhận.

### 4.3. Chức năng Sao Kê & Lịch Sử (History)
- Truy vấn các transaction phân trang qua `transactionRepository.findHistoryWithFilters`.
- Tính toán sao kê số dư đầu kỳ và cuối kỳ thông qua việc lấy số dư biến động (BalanceAfter) của các giao dịch xảy ra liền kề trước hoặc đúng vào `startDate` và `endDate`.

---
*Tài liệu này phản ánh toàn bộ logic kỹ thuật đang chạy trên hệ thống dựa trên sự kết hợp chặt chẽ giữa Database Transactions, Redis và Spring Security.*
