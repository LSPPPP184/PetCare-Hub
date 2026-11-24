# Checklist bốn tính chất OOP

Đối chiếu mã nguồn ngày 22/11/2026, commit `1598cc754b3e95252fe61bcca8134cf6d0808fcc`. Tên định danh giữ nguyên theo Java; phần giải thích bằng tiếng Việt.

Mỗi liên kết chỉ dòng trong nguồn tại thời điểm đối chiếu; phương thức ghi kèm vẫn là mốc tra cứu nếu số dòng thay đổi. Không dùng DTO setter làm ví dụ đóng gói nghiệp vụ.

## Đóng gói

| Class + dòng | Phương thức/quan hệ | Bằng chứng |
| --- | --- | --- |
| [Cart:25](../src/main/java/com/petcare/domain/Cart.java#L25) | `getItems(), add(), setQuantity()` | items là private; trả unmodifiableList và bắt thay đổi qua kiểm tra chính sách mua. |
| [LoyaltyAccount:51](../src/main/java/com/petcare/domain/LoyaltyAccount.java#L51) | `redeem(), credit(), adjust()` | Không có setter số dư/hạng; redeem kiểm tra voucher và đủ 100 điểm trước khi trừ. |
| [OrderItem:52](../src/main/java/com/petcare/domain/OrderItem.java#L52) | `subscriptionBenefits(), getSnapshotServices()` | Giữ quyền lợi đã chốt; tập dịch vụ chỉ đọc; cột snapshot updatable=false. Đây là bảo vệ API ứng dụng, không phải bất biến vật lý tuyệt đối của DB. |

## Kế thừa

| Class + dòng | Phương thức/quan hệ | Bằng chứng |
| --- | --- | --- |
| [Product:8](../src/main/java/com/petcare/domain/Product.java#L8) | `Product → Pet, Accessory, ServiceItem` | Các lớp con dùng chung tên, giá gốc, version, toForm; JPA JOINED lưu đúng cây lớp. |
| [ServiceItem:5](../src/main/java/com/petcare/domain/ServiceItem.java#L5) | `ServiceItem → GroomingService, VetService, MonthlyCarePackage` | Tầng trung gian chia sẻ durationMinutes; lớp con thêm phụ phí, bác sĩ hoặc số tháng/số lượt. |
| [MutableClock:8](../src/main/java/com/petcare/config/MutableClock.java#L8) | `MutableClock extends Clock; instant(), withZone()` | Thay cách cung cấp thời gian trong profile demo, vẫn dùng hợp đồng Clock tại các service sức khỏe. |

## Đa hình

| Class + dòng | Phương thức/quan hệ | Bằng chứng |
| --- | --- | --- |
| [OrderItem:37](../src/main/java/com/petcare/domain/OrderItem.java#L37) | `OrderItem constructor → Product.calculatePrice()` | Biến kiểu Product gọi cách tính giá của Pet/Accessory/GroomingService/... ở runtime; GroomingService cộng phụ phí. |
| [Order:70](../src/main/java/com/petcare/domain/Order.java#L70) | `confirm() → getState().confirm(this)` | Cùng lời gọi confirm nhưng PendingState cho xác nhận, các State khác từ chối qua AbstractOrderState. |
| [MatchingService:48](../src/main/java/com/petcare/service/MatchingService.java#L48) | `submit() → MatchingStrategy.match()` | Cùng câu trả lời có thể chạy WeightedScoreStrategy hoặc BudgetFirstStrategy theo registry key. |

## Trừu tượng

| Class + dòng | Phương thức/quan hệ | Bằng chứng |
| --- | --- | --- |
| [Product:53](../src/main/java/com/petcare/domain/Product.java#L53) | `Product.calculatePrice(), getType(), getCategory()` | Hợp đồng sản phẩm không quy định một công thức giá chung cho mọi lớp cụ thể. |
| [MatchingStrategy:9](../src/main/java/com/petcare/service/matching/MatchingStrategy.java#L9) | `match(), key(), label()` | Service chỉ cần hợp đồng thuật toán, không biết công thức hay chi tiết lọc. |
| [DiscountStrategy:6](../src/main/java/com/petcare/domain/discount/DiscountStrategy.java#L6) | `apply(), discountRate()` | LoyaltyPricingService sử dụng hợp đồng giảm giá; cách lấy tỷ lệ/làm tròn được giấu sau triển khai. |

## Cách trình bày trước giảng viên

Phân biệt kế thừa (quan hệ is-a trong khai báo lớp) với đa hình (lời gọi qua Product/State/Strategy chọn hành vi runtime). Trừu tượng nêu hợp đồng cần làm gì; đóng gói bảo vệ dữ liệu và quy tắc thay đổi. Một ví dụ có thể thể hiện nhiều tính chất nhưng phần giải thích phải chỉ rõ góc nhìn.
Đối chứng test: [ProductFactoryTest#calculatesExactPrice](../src/test/java/com/petcare/ProductFactoryTest.java), [OrderStateTest#verifiesEveryTransitionAndRejectsInvalidActions](../src/test/java/com/petcare/OrderStateTest.java), [MatchingStrategyTest#strategiesReverseRankingForSameAnswers](../src/test/java/com/petcare/MatchingStrategyTest.java), [LoyaltyDomainTest#redeemAndNegativeAdjustmentNeverLowerLifetimePointsOrTier](../src/test/java/com/petcare/LoyaltyDomainTest.java), [SubscriptionIntegrationTest#purchaseSnapshotsPackageUsesMonthsAndServicesBeforeCatalogChanges](../src/test/java/com/petcare/SubscriptionIntegrationTest.java).
