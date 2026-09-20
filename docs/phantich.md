# I. PHÂN TÍCH BÀI TOÁN
## Giả định
### Phần 1. GIẢ ĐỊNH CHUNG CHO TOÀN HỆ THỐNG
- **Toàn vẹn giao dịch**: Hệ thống bọc toàn bộ logic thay đổi số dư trong @Transactional với cơ chế thiết lập DB chống lỗi đồng thời. Khi có bất kỳ lỗi Runtime Exception nào xảy ra giữa chừng (ví dụ: mất kết nối, lỗi logic tính toán ở bước sau), toàn bộ tiến trình phải được tự động Rollback hoàn toàn về trạng thái ban đầu, tuyệt đối không để xảy ra kịch bản "ví nguồn đã trừ nhưng ví đích chưa cộng".
- **Đơn vị tiền tệ**: Hệ thống chỉ sử dụng đơn vị Việt Nam Đồng (VND). Kiểu dữ liệu trong Java là BigDecimal và trong Database là DECIMAL(18, 2) nhằm bảo toàn tính chính xác tuyệt đối, ngăn chặn triệt để lỗi sai số nhị phân của kiểu dữ liệu Double/Float.
- **Audit log bất biến**: Mọi giao dịch khi đã được ghi nhận vào bảng Transaction sẽ ở trạng thái Bất biến. Hệ thống không cung cấp bất kỳ API nào cho phép chỉnh sửa hoặc xóa các bản ghi lịch sử này. Mọi thay đổi trạng thái của giao dịch phải tuân theo State Machine và được lưu vết rõ ràng.
- **Cơ chế xác thực JWT**: Sau khi đăng nhập đúng thông tin, hệ thống trả về một chuỗi Stateless JWT Token chứa các thông tin (Claims) cốt lõi như userId, username, và role. Token này phải được gửi kèm trong Header Authorization: Bearer <token> của tất cả các API private sau đó. Nếu thiếu hoặc sai token, hệ thống lập tức từ chối xử lý và trả về mã lỗi 401 Unauthorized.
### Phần 2. BÓC TÁCH CHI TIẾT TỪNG API
#### 1. Chức năng Đăng ký & Đăng nhập
- **Giả định**:
    - **Mã hóa mật khẩu**: Hệ thống mặc định sử dụng thuật toán mã hóa một chiều để hash mật khẩu người dùng trước khi lưu xuống Database.
    - **Ranh giới mật khẩu**: Hệ thống quy định mật khẩu mạnh phải có độ dài tối thiểu là 8 ký tự. Nếu ngắn hơn, hệ thống sẽ từ chối ngay từ vòng nhận dữ liệu với mã 400 Bad Request.
    - **Bảo mật thông tin đăng nhập**: Khi đăng nhập thất bại (sai username hoặc sai password), hệ thống chỉ trả về một thông báo chung 401 Unauthorized để tránh việc Hacker dò tìm tài khoản.

#### 2. Chức năng nạp tiền & xem số dư
- **Giả định**:
    - **An toàn xem số dư**: Người dùng chỉ được xem số dư của ví thuộc quyền sở hữu của mình dựa vào thông tin userId trích xuất từ JWT Token.
    - **Luật nạp tiền và phí**: Giao dịch nạp tiền hoàn toàn miễn phí (0đ phụ phí) và chỉ tương tác với duy nhất chiếc ví của người thực hiện request (Không có quan hệ gửi - nhận). Số tiền nạp phải là một số nguyên dương, tối thiểu là 10.000 VNĐ và độ dài không vượt quá 18 chữ số. Nạp thành công hệ thống sinh 1 bản ghi giao dịch TOPUP với trạng thái COMPLETED và trả về mã 201 Created.
#### 3. Chức năng chuyển tiền
- **Giả định**:
    - **Mối quan hệ và khóa dòng Concurrency: Giao dịch bắt buộc phải có mối quan hệ giữa 2 tài khoản (Ví nguồn và Ví đích). Hệ thống áp dụng cơ chế Pessimistic Locking khi truy vấn ví: Khi ví đang bị trừ tiền, mọi giao dịch đồng thời khác tác động lên ví đó phải xếp hàng chờ để đảm bảo tổng rút không vượt quá số dư.
    - **Chống trùng lặp**: Client bắt buộc phải truyền kèm một chuỗi định danh duy nhất trong Header mang tên Idempotency-Key (Lưu trên Redis với TTL 10 phút). Nếu gửi liên tiếp 2 request trùng Key do lag mạng, hệ thống chỉ xử lý giao dịch ở request 1, request 2 lập tức trả về kết quả của request 1 mà không thực hiện trừ tiền lần 2.
    - **Quy tắc tính phí**: Người gửi chịu phí giao dịch. Phí được tính bằng 0.5% dựa trên số tiền chuyển gốc nhập từ ô input, nhưng áp dụng thêm điều kiện ràng buộc: mức phí tối thiểu phải là 1.000 VNĐ. Phí phát sinh được làm tròn lên đến hàng đơn vị (đồng).
        - **Công thức trừ tiền ví nguồn**: Tổng trừ = Số tiền chuyển (Người nhận hưởng) + Phí.
    - **Điều kiện số dư và hạng mức ngày**: Giao dịch chỉ thành công khi người gửi có số dư hiện tại ≥ Tổng trừ, đồng thời số dư khả dụng ban đầu phải đạt tối thiểu 10.000 VNĐ. Số tiền chuyển tối thiểu cho một lần là 10.000 VNĐ, độ dài số tiền không vượt quá 18 chữ số. Hệ thống áp dụng hạn mức ngày tối đa là 50.000.000 VNĐ / ngày (Tính trong chu kỳ từ 00:00:00 đến 23:59:59 theo GMT+7).
    - **Trạng thái giao dịch**: Khi bắt đầu kích hoạt lệnh chuyển tiền, giao dịch mang trạng thái PENDING. Sau khi hệ thống trừ/cộng tiền hoàn tất, giao dịch chuyển sang trạng thái cuối cùng là COMPLETED. Nếu có bất kỳ bước kiểm tra nào bị thất bại (thiếu tiền, vượt hạn mức, sai thông tin), giao dịch chuyển sang trạng thái FAILED.

#### 4. Chức năng xem lịch sử, sao kê và lọc nâng cao
- **Giả định**:
    - **Cơ chế phân trang**: Khi danh sách lịch sử có từ 10 giao dịch trở lên, hệ thống bắt buộc phân trang theo kích thước cố định là 10 giao dịch / trang. Thứ tự sắp xếp mặc định luôn là giao dịch mới nhất hiển thị lên đầu.
    - **Phân quyền bảo mật và ranh giới admin**: User chỉ được xem lịch sử của chính mình qua cơ chế check ID chặt chẽ, không thể xem giao dịch của Admin vì Admin không sở hữu ví tiền và không phát sinh giao dịch tài chính. ADMIN có quyền tối cao tra cứu lịch sử của bất kỳ người dùng nào bằng cách truyền userId cụ thể.
    - **Logic tính toán sao kê**: Khi người dùng yêu cầu kết xuất sao kê theo khoảng thời gian, hệ thống tính toán động số liệu dựa trên các công thức kế toán sau:
        - Số dư đầu kỳ: Tổng số tiền biến động khả dụng của ví tại thời điểm trước giây đầu tiên của ngày fromDate.
        - Tổng tiền vào: Tổng số tiền của các giao dịch thành công mang chiều nạp (TOPUP) hoặc nhận tiền (TRANSFER_IN).
        - Tổng tiền ra: Tổng số tiền gốc của các giao dịch thành công mang chiều gửi tiền đi (TRANSFER_OUT).
        - Tổng phí: Tổng tất cả các khoản phí giao dịch phát sinh được ghi nhận riêng.
        - Số dư cuối kỳ: Tính theo công thức bắt buộc: Số dư cuối kỳ = Số dư đầu kỳ + Tổng tiền vào - Tổng tiền ra - Tổng phí.
    - **Logic lọc**: Hệ thống cho phép lọc động kết hợp: theo loại giao dịch (TOPUP, TRANSFER, REVERSAL), theo chiều tiền (IN, OUT), hoặc theo khoảng ngày. Các bộ lọc nếu để trống sẽ được hệ thống ngầm định là chọn tất cả.

#### 5. Chức năng Admin hoàn giao dịch
- **Giả định**:
    - **Kiểm soát trạng thái**: ADMIN chỉ có quyền thực hiện hoàn tiền đối với các giao dịch chuyển tiền đã ở trạng thái COMPLETED. Ngay khi lệnh hoàn tiền thành công, trạng thái của giao dịch gốc phải được cập nhật lập tức từ COMPLETED sang REVERSED. Mọi nỗ lực thực hiện hoàn tiền lần thứ hai trên một giao dịch đã REVERSED phải bị hệ thống từ chối và trả về mã lỗi 409 Conflict.
    - **Xử lý dòng tiền hoàn**: Hệ thống sinh một bản ghi giao dịch mới mang loại REVERSAL. Số tiền hoàn trả về cho ví nguồn (người gửi ban đầu) bằng đúng số tiền gốc ghi nhận trên ô input (Người nhận ban đầu bị thu hồi đúng số tiền đó). Khoản phí giao dịch 0.5% phát sinh ở giao dịch gốc sẽ không được hoàn lại cho người gửi vì đây là chi phí xử lý hệ thống đã hoàn tất.
    - **Giới hạn số dư**: Hệ thống chấp nhận kịch bản số dư của ví bị thu hồi tiền rơi vào trạng thái Số dư âm nếu tại thời điểm ADMIN bấm nút Hoàn tiền, tài khoản đó đã thực hiện tẩu tán hoặc rút sạch số dư về bằng 0đ (Nhằm đảm bảo tính công bằng, trả lại tiền vô điều kiện cho nạn nhân bị mất tiền).

## Edge cases

### 1. Chức năng Đăng ký (Register)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Username đã tồn tại trong hệ thống | `username` = "existingUser" | Từ chối đăng ký, trả thông báo lỗi chung | `409 Conflict` |
| Mật khẩu đúng 8 ký tự (biên dưới hợp lệ) | `password` = "Abcd1234" (8 chars) | Đăng ký thành công | `201 Created` |
| Mật khẩu 7 ký tự (dưới biên) | `password` = "Abc1234" (7 chars) | Từ chối ngay tại validation | `400 Bad Request` |
| Mật khẩu rỗng (empty string) | `password` = "" | Từ chối ngay tại validation | `400 Bad Request` |
| Username rỗng hoặc null | `username` = "" hoặc `null` | Từ chối ngay tại validation | `400 Bad Request` |
| Mật khẩu cực dài (vượt giới hạn) | `password` = 1000 ký tự | Từ chối hoặc cắt, tùy policy hệ thống | `400 Bad Request` |
| Username chứa ký tự đặc biệt / SQL injection | `username` = "'; DROP TABLE users;--" | Từ chối hoặc xử lý an toàn, không thực thi SQL | `400 Bad Request` |
| Gửi request thiếu trường bắt buộc | Body thiếu `username` hoặc `password` | Từ chối ngay tại validation | `400 Bad Request` |
| Mật khẩu chỉ gồm khoảng trắng | `password` = "        " (8 spaces) | Tùy policy: từ chối vì không đảm bảo mật khẩu mạnh | `400 Bad Request` |
| Đăng ký thành công → kiểm tra mật khẩu đã được hash | Đăng ký bình thường | Mật khẩu lưu trong DB phải khác plaintext, đã qua hash một chiều | `201 Created` |

### 2. Chức năng Đăng nhập (Login)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Đăng nhập đúng username, đúng password | Thông tin hợp lệ | Trả về JWT token chứa `userId`, `username`, `role` | `200 OK` |
| Đúng username, sai password | `password` sai | Trả thông báo lỗi **chung chung** (không tiết lộ username tồn tại) | `401 Unauthorized` |
| Sai username (không tồn tại) | `username` không tồn tại | Trả thông báo lỗi **chung chung** giống hệt case trên | `401 Unauthorized` |
| Cả username và password đều sai | Cả 2 trường sai | Trả thông báo lỗi chung chung | `401 Unauthorized` |
| Username rỗng | `username` = "" | Từ chối tại validation | `400 Bad Request` |
| Password rỗng | `password` = "" | Từ chối tại validation | `400 Bad Request` |
| Gửi request không có body | Body = null | Từ chối tại validation | `400 Bad Request` |
| JWT token trả về phải có đầy đủ claims | Đăng nhập thành công | Token decode ra phải chứa: `userId`, `username`, `role` | `200 OK` |
| Gọi API private không có token | Header thiếu `Authorization` | Từ chối xử lý | `401 Unauthorized` |
| Gọi API private với token hết hạn | Token đã expired | Từ chối xử lý | `401 Unauthorized` |
| Gọi API private với token bị chỉnh sửa/giả mạo | Token bị tampered | Từ chối xử lý (chữ ký không hợp lệ) | `401 Unauthorized` |
| Gọi API private với token sai format | `Authorization: Bearer xxxinvalid` | Từ chối xử lý | `401 Unauthorized` |

### 3. Chức năng Nạp tiền & Xem số dư

#### 3a. Nạp tiền (Top-up)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Nạp đúng 10.000 VNĐ (biên dưới hợp lệ) | `amount` = 10000 | Nạp thành công, tạo bản ghi TOPUP → COMPLETED | `201 Created` |
| Nạp 9.999 VNĐ (dưới biên) | `amount` = 9999 | Từ chối giao dịch | `400 Bad Request` |
| Nạp 0 VNĐ | `amount` = 0 | Từ chối: không phải số nguyên dương | `400 Bad Request` |
| Nạp số âm | `amount` = -50000 | Từ chối: không phải số nguyên dương | `400 Bad Request` |
| Nạp số thập phân | `amount` = 10000.50 | Từ chối: phải là số nguyên dương | `400 Bad Request` |
| Nạp số có đúng 18 chữ số (biên trên) | `amount` = 999999999999999999 (18 digits) | Nạp thành công | `201 Created` |
| Nạp số có 19 chữ số (vượt biên trên) | `amount` = 1000000000000000000 (19 digits) | Từ chối: vượt giới hạn độ dài | `400 Bad Request` |
| Gửi amount dạng string/text | `amount` = "abc" | Từ chối: sai kiểu dữ liệu | `400 Bad Request` |
| Gửi request thiếu trường amount | Body thiếu `amount` | Từ chối tại validation | `400 Bad Request` |
| Nạp tiền không có JWT token | Header thiếu Authorization | Từ chối xử lý | `401 Unauthorized` |
| Nạp tiền thành công → xác nhận phí = 0đ | Nạp 100.000 VNĐ | Số dư tăng đúng 100.000 VNĐ, không phát sinh phí | `201 Created` |
| Nạp tiền thành công → kiểm tra bản ghi Transaction | Nạp hợp lệ | Có 1 bản ghi TOPUP, trạng thái COMPLETED, audit log bất biến | `201 Created` |

#### 3b. Xem số dư (Balance)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Xem số dư ví của chính mình | JWT chứa userId hợp lệ | Trả về số dư chính xác của ví thuộc userId đó | `200 OK` |
| Cố xem số dư ví của người khác | userId trong JWT ≠ walletId truyền vào | Từ chối truy cập | `403 Forbidden` |
| Xem số dư khi ví mới tạo (chưa nạp) | Ví vừa đăng ký xong | Số dư = 0 VNĐ | `200 OK` |
| Xem số dư không có token | Không có JWT | Từ chối | `401 Unauthorized` |
| Xem số dư sau khi nạp tiền liên tục nhiều lần | Nạp 3 lần: 10k, 20k, 30k | Số dư = 60.000 VNĐ (tính chính xác BigDecimal) | `200 OK` |

### 4. Chức năng Chuyển tiền (Transfer)

#### 4a. Validation cơ bản

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Chuyển đúng 10.000 VNĐ (biên dưới) | `amount` = 10000 | Thành công: trừ ví nguồn (10.000 + phí), cộng ví đích 10.000 | `201 Created` |
| Chuyển 9.999 VNĐ (dưới biên) | `amount` = 9999 | Từ chối: dưới mức tối thiểu | `400 Bad Request` |
| Chuyển số âm | `amount` = -10000 | Từ chối | `400 Bad Request` |
| Chuyển 0 VNĐ | `amount` = 0 | Từ chối | `400 Bad Request` |
| Chuyển số thập phân | `amount` = 15000.75 | Từ chối: phải là số nguyên | `400 Bad Request` |
| Số tiền có 18 chữ số (biên trên) | `amount` = 999999999999999999 | Xử lý bình thường (nếu đủ số dư và hạn mức) | `201 Created` |
| Số tiền có 19 chữ số (vượt biên) | `amount` = 1000000000000000000 | Từ chối: vượt giới hạn độ dài | `400 Bad Request` |
| Chuyển tiền cho chính mình | `toUserId` = userId (JWT) | Từ chối: ví nguồn và ví đích trùng nhau | `400 Bad Request` |
| Chuyển tiền cho userId không tồn tại | `toUserId` không có trong DB | Từ chối: người nhận không tồn tại | `404 Not Found` |

#### 4b. Tính phí

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Phí chuyển 10.000 VNĐ → 0.5% = 50đ < sàn 1.000đ | `amount` = 10000 | Phí = 1.000 VNĐ (áp sàn). Tổng trừ = 11.000 VNĐ | `201 Created` |
| Phí chuyển 200.000 VNĐ → 0.5% = 1.000đ = sàn | `amount` = 200000 | Phí = 1.000 VNĐ (đúng bằng sàn). Tổng trừ = 201.000 VNĐ | `201 Created` |
| Phí chuyển 1.000.000 VNĐ → 0.5% = 5.000đ > sàn | `amount` = 1000000 | Phí = 5.000 VNĐ. Tổng trừ = 1.005.000 VNĐ | `201 Created` |
| Phí cần làm tròn lên: 0.5% × 333.333 = 1666.665 | `amount` = 333333 | Phí = 1.667 VNĐ (ceil). Tổng trừ = 335.000 VNĐ | `201 Created` |
| Phí chuyển 100.000 → 0.5% = 500đ < sàn 1.000đ | `amount` = 100000 | Phí = 1.000 VNĐ (áp sàn). Tổng trừ = 101.000 VNĐ | `201 Created` |

#### 4c. Kiểm tra số dư

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Số dư ví nguồn < Tổng trừ | Số dư = 50.000, chuyển 50.000 (tổng trừ = 51.000) | Từ chối: không đủ số dư | `400 Bad Request` |
| Số dư ví nguồn = Tổng trừ (vừa đủ) | Số dư = 11.000, chuyển 10.000 (tổng trừ = 11.000) | Thành công, số dư còn lại = 0 VNĐ | `201 Created` |
| Số dư ban đầu < 10.000 VNĐ | Số dư = 9.999 VNĐ | Từ chối: số dư khả dụng ban đầu chưa đạt tối thiểu 10.000 | `400 Bad Request` |
| Số dư ban đầu = 10.000 VNĐ (biên dưới) | Số dư = 10.000, chuyển 10.000 (tổng trừ = 11.000) | Từ chối: đủ điều kiện số dư tối thiểu nhưng không đủ tổng trừ | `400 Bad Request` |

#### 4d. Hạn mức ngày

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Tổng chuyển trong ngày = 49.000.000, chuyển thêm 1.000.000 | Tổng = 50.000.000 ≤ hạn mức | Thành công (vừa đúng hạn mức) | `201 Created` |
| Tổng chuyển trong ngày = 50.000.000, chuyển thêm 10.000 | Tổng = 50.010.000 > hạn mức | Từ chối: vượt hạn mức ngày 50 triệu | `400 Bad Request` |
| Chuyển tiền ở 23:59:59 GMT+7 và 00:00:00 GMT+7 ngày mới | Giao dịch tại ranh giới ngày | Hạn mức phải reset ở 00:00:00 GMT+7. Giao dịch 23:59:59 tính ngày cũ, 00:00:00 tính ngày mới | Tùy case |
| Ngày mới → hạn mức reset về 0 | Ngày hôm qua đã chuyển 50 triệu, hôm nay chuyển 10.000 | Thành công: hạn mức ngày mới = 0 → chưa vượt | `201 Created` |

#### 4e. Idempotency

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Gửi 2 request trùng Idempotency-Key liên tiếp | Cùng `Idempotency-Key` header | Request 1: xử lý bình thường. Request 2: trả lại kết quả request 1, **không** trừ tiền lần 2 | `200 OK` (req 2) |
| Gửi request không có Idempotency-Key header | Header thiếu `Idempotency-Key` | Từ chối: bắt buộc phải có | `400 Bad Request` |
| Gửi Idempotency-Key rỗng | `Idempotency-Key` = "" | Từ chối | `400 Bad Request` |
| Gửi lại Idempotency-Key sau khi TTL 10 phút hết hạn | Cùng key, sau 10 phút | Key đã hết hạn trên Redis → xử lý như giao dịch mới | `201 Created` |

#### 4f. Concurrency (Pessimistic Locking)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| 2 giao dịch đồng thời trên cùng 1 ví nguồn | Ví có 100k, 2 request cùng lúc chuyển 60k | Chỉ 1 thành công, 1 thất bại do thiếu số dư (tổng rút ≤ số dư) | 1 × `201`, 1 × `400` |
| Nhiều giao dịch đồng thời chuyển vào cùng 1 ví đích | 3 người cùng chuyển cho 1 người nhận | Tất cả thành công, ví đích cộng đúng tổng | `201 Created` |
| Deadlock potential: A chuyển cho B, đồng thời B chuyển cho A | 2 request cùng lúc, khóa ngược chiều | Hệ thống phải xử lý không bị deadlock (lock theo thứ tự ID ví) | Cả 2 thành công hoặc 1 retry |

#### 4g. Trạng thái giao dịch

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Giao dịch thành công → trạng thái cuối cùng | Chuyển tiền hợp lệ | PENDING → COMPLETED. Bản ghi TRANSFER bất biến | `201 Created` |
| Giao dịch thất bại (thiếu tiền) → trạng thái | Số dư không đủ | PENDING → FAILED. Bản ghi giao dịch ghi nhận lý do | `400 Bad Request` |
| Runtime Exception giữa chừng (DB crash giữa trừ/cộng) | Lỗi kết nối DB sau khi trừ ví nguồn | Rollback toàn bộ: ví nguồn khôi phục, ví đích không thay đổi | `500 Internal Server Error` |

#### 4h. Khác

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Chuyển tiền không có JWT token | Thiếu Authorization header | Từ chối | `401 Unauthorized` |
| User role cố gắng chuyển tiền từ ví người khác | JWT userId ≠ fromUserId | Từ chối | `403 Forbidden` |

### 5. Chức năng Xem lịch sử, Sao kê & Lọc nâng cao

#### 5a. Xem lịch sử giao dịch

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| User xem lịch sử của chính mình | JWT userId hợp lệ | Trả về danh sách giao dịch của user đó | `200 OK` |
| User cố xem lịch sử của người khác | Truyền userId khác với JWT | Từ chối | `403 Forbidden` |
| User cố xem lịch sử của ADMIN | Truyền adminId | Từ chối: Admin không sở hữu ví, không có giao dịch | `403 Forbidden` hoặc `404` |
| ADMIN xem lịch sử của bất kỳ user nào | ADMIN JWT + truyền userId cụ thể | Trả về lịch sử giao dịch của user đó | `200 OK` |
| ADMIN xem lịch sử user không tồn tại | ADMIN JWT + userId không tồn tại | Trả về lỗi | `404 Not Found` |
| User mới, chưa có giao dịch nào | Ví trống | Trả về danh sách rỗng, không lỗi | `200 OK` |

#### 5b. Phân trang

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Có đúng 10 giao dịch (biên phân trang) | 10 bản ghi | 1 trang, 10 items, sắp xếp mới nhất lên đầu | `200 OK` |
| Có 11 giao dịch | 11 bản ghi | Trang 1: 10 items, Trang 2: 1 item | `200 OK` |
| Có 9 giao dịch (dưới biên phân trang) | 9 bản ghi | 1 trang, 9 items, không cần phân trang | `200 OK` |
| Yêu cầu trang không tồn tại (page = 999) | `page` = 999, tổng chỉ 2 trang | Trả về danh sách rỗng hoặc thông báo hết trang | `200 OK` |
| Yêu cầu page = 0 hoặc page âm | `page` = 0 hoặc -1 | Từ chối hoặc mặc định về trang 1 | `400 Bad Request` |
| Kiểm tra thứ tự sắp xếp | Nhiều giao dịch tạo lần lượt | Giao dịch mới nhất (createdAt lớn nhất) nằm ở vị trí đầu tiên | `200 OK` |

#### 5c. Sao kê (Statement)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Sao kê khoảng thời gian có đầy đủ các loại giao dịch | TOPUP + TRANSFER_IN + TRANSFER_OUT trong khoảng | Tính đúng: Số dư đầu kỳ, Tổng vào, Tổng ra, Tổng phí, Số dư cuối kỳ = Đầu kỳ + Vào - Ra - Phí | `200 OK` |
| Sao kê khoảng thời gian không có giao dịch nào | fromDate → toDate trống giao dịch | Tổng vào = 0, Tổng ra = 0, Tổng phí = 0, Số dư cuối kỳ = Số dư đầu kỳ | `200 OK` |
| Số dư đầu kỳ = 0 (ví mới) | fromDate = ngày đăng ký, chưa có giao dịch trước đó | Số dư đầu kỳ = 0 | `200 OK` |
| fromDate > toDate (khoảng thời gian đảo ngược) | fromDate = "2026-06-15", toDate = "2026-06-10" | Từ chối: khoảng thời gian không hợp lệ | `400 Bad Request` |
| fromDate = toDate (cùng ngày) | fromDate = toDate = "2026-06-15" | Trả kết quả sao kê trong đúng 1 ngày đó | `200 OK` |
| Giao dịch FAILED có bị tính vào sao kê không? | Có giao dịch FAILED trong khoảng | FAILED không được tính vào Tổng vào/Tổng ra (chỉ tính COMPLETED) | `200 OK` |
| Giao dịch REVERSAL ảnh hưởng sao kê | Có giao dịch hoàn trong khoảng | REVERSAL phải được phản ánh chính xác vào dòng tiền (tiền vào cho người gửi gốc, tiền ra cho người nhận gốc) | `200 OK` |
| Công thức kiểm tra: Cuối kỳ = Đầu kỳ + Vào - Ra - Phí | Bất kỳ khoảng thời gian nào | Công thức luôn đúng, không có sai lệch | `200 OK` |
| Số dư đầu kỳ tính tại thời điểm trước giây đầu tiên của fromDate | fromDate = "2026-06-15" | Số dư đầu kỳ = tổng biến động ví trước 2026-06-15 00:00:00 | `200 OK` |

#### 5d. Lọc nâng cao (Filter)

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Lọc theo loại TOPUP | `type` = "TOPUP" | Chỉ trả về giao dịch nạp tiền | `200 OK` |
| Lọc theo loại TRANSFER | `type` = "TRANSFER" | Chỉ trả về giao dịch chuyển tiền (IN + OUT) | `200 OK` |
| Lọc theo loại REVERSAL | `type` = "REVERSAL" | Chỉ trả về giao dịch hoàn tiền | `200 OK` |
| Lọc theo chiều IN | `direction` = "IN" | Chỉ trả về giao dịch tiền vào (TOPUP + TRANSFER_IN) | `200 OK` |
| Lọc theo chiều OUT | `direction` = "OUT" | Chỉ trả về giao dịch tiền ra (TRANSFER_OUT) | `200 OK` |
| Lọc kết hợp: loại TRANSFER + chiều IN | `type` = "TRANSFER", `direction` = "IN" | Chỉ trả về TRANSFER_IN | `200 OK` |
| Lọc kết hợp: loại + chiều + khoảng ngày | `type` + `direction` + `fromDate`/`toDate` | Kết quả thỏa mãn cả 3 điều kiện | `200 OK` |
| Không truyền bất kỳ bộ lọc nào | Tất cả filter = null/empty | Ngầm định = chọn tất cả, trả về toàn bộ giao dịch | `200 OK` |
| Truyền loại giao dịch không hợp lệ | `type` = "INVALID_TYPE" | Từ chối hoặc bỏ qua | `400 Bad Request` |
| Lọc theo khoảng ngày nhưng fromDate > toDate | fromDate = "2026-06-15", toDate = "2026-06-10" | Từ chối: khoảng thời gian không hợp lệ | `400 Bad Request` |
| Lọc ra kết quả 0 bản ghi | Lọc REVERSAL nhưng chưa có giao dịch hoàn nào | Trả về danh sách rỗng, không lỗi | `200 OK` |

### 6. Chức năng Admin hoàn giao dịch (Reversal)

#### 6a. Kiểm soát trạng thái

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Hoàn giao dịch COMPLETED hợp lệ | Giao dịch gốc đang COMPLETED | Thành công: giao dịch gốc → REVERSED, tạo bản ghi REVERSAL mới | `201 Created` |
| Cố hoàn giao dịch đã REVERSED (lần 2) | Giao dịch gốc đã REVERSED | Từ chối: đã hoàn rồi, không hoàn lại được | `409 Conflict` |
| Cố hoàn giao dịch đang PENDING | Giao dịch gốc đang PENDING | Từ chối: chỉ hoàn được COMPLETED | `400 Bad Request` |
| Cố hoàn giao dịch FAILED | Giao dịch gốc đang FAILED | Từ chối: chỉ hoàn được COMPLETED | `400 Bad Request` |
| Cố hoàn giao dịch loại TOPUP | transactionId thuộc giao dịch TOPUP | Từ chối: chỉ hoàn được giao dịch TRANSFER | `400 Bad Request` |
| Cố hoàn giao dịch loại REVERSAL | transactionId thuộc giao dịch REVERSAL | Từ chối: không thể hoàn giao dịch hoàn | `400 Bad Request` |
| Hoàn giao dịch không tồn tại | transactionId không có trong DB | Từ chối | `404 Not Found` |

#### 6b. Xử lý dòng tiền hoàn

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Hoàn tiền đúng số tiền gốc (không hoàn phí) | Giao dịch gốc: chuyển 100.000, phí 1.000 | Ví nguồn (người gửi gốc) +100.000. Ví đích (người nhận gốc) -100.000. Phí 1.000 **không** được hoàn | `201 Created` |
| Kiểm tra bản ghi REVERSAL được tạo | Hoàn thành công | Có bản ghi mới type = REVERSAL, liên kết đến giao dịch gốc | `201 Created` |
| Giao dịch gốc chuyển 10.000 (phí sàn 1.000đ) → hoàn | Phí gốc = 1.000 (áp sàn) | Hoàn 10.000 cho người gửi, thu hồi 10.000 từ người nhận. Phí 1.000 không hoàn | `201 Created` |

#### 6c. Số dư âm

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Người nhận đã rút hết tiền, số dư = 0 → bị thu hồi | Ví người nhận = 0 VNĐ, bị trừ 100.000 | Ví người nhận = -100.000 VNĐ (chấp nhận âm). Ví người gửi gốc +100.000 | `201 Created` |
| Người nhận đã chuyển hết cho bên thứ 3 → bị thu hồi | Ví người nhận = 5.000 VNĐ, bị trừ 100.000 | Ví người nhận = -95.000 VNĐ (chấp nhận âm) | `201 Created` |
| Kiểm tra ví có số dư âm có thể nạp tiền để bù không | Ví = -100.000, nạp thêm 200.000 | Số dư = 100.000 VNĐ. Nạp tiền vẫn hoạt động bình thường | `201 Created` |

#### 6d. Phân quyền

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| User (không phải ADMIN) cố hoàn giao dịch | JWT role = USER | Từ chối: chỉ ADMIN mới có quyền hoàn | `403 Forbidden` |
| ADMIN không có JWT token | Thiếu Authorization header | Từ chối | `401 Unauthorized` |

#### 6e. Concurrency

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| 2 ADMIN đồng thời hoàn cùng 1 giao dịch | 2 request hoàn cùng transactionId | Chỉ 1 thành công (COMPLETED → REVERSED), request còn lại nhận 409 Conflict | 1 × `201`, 1 × `409` |

#### 6f. Audit log

| Mô tả Edge Case | Input / Điều kiện | Kết quả mong đợi | HTTP Status |
|---|---|---|---|
| Sau hoàn tiền, kiểm tra tính bất biến | Hoàn thành công | Giao dịch gốc: trạng thái = REVERSED (không xóa). Giao dịch REVERSAL: bản ghi mới, bất biến | `201 Created` |