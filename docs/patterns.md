# Pattern thực sự có trong code

Đối chiếu mã nguồn ngày 22/11/2026, commit `1598cc754b3e95252fe61bcca8134cf6d0808fcc`. Tên định danh giữ nguyên theo Java; phần giải thích bằng tiếng Việt.

Các test dưới đây là bằng chứng trong source test; kết quả thực thi được ghi riêng ở hồ sơ kiểm chứng. Simple Factory và Repository được gọi đúng phạm vi, không đồng nhất mọi lớp có tên Factory với GoF Factory Method.

| Pattern | File/class cụ thể | Vấn đề giải quyết | Vì sao chọn | Nếu không dùng |
| --- | --- | --- | --- | --- |
| State — Order | [Order:70](../src/main/java/com/petcare/domain/Order.java#L70); [OrderState:1](../src/main/java/com/petcare/domain/order/OrderState.java#L1) | Ràng buộc vòng đời đơn và hoàn kho khi hủy | Mỗi State mở đúng hành động; enum lưu chữ, hành vi đa hình | Chuỗi if/switch lặp ở controller/service dễ cho phép ship hoặc hoàn kho sai |
| State — Appointment | [Appointment:86](../src/main/java/com/petcare/domain/Appointment.java#L86); [AppointmentState:1](../src/main/java/com/petcare/domain/appointment/AppointmentState.java#L1) | Không hoàn tất lịch khi chưa bắt đầu | State tách vòng đời khỏi chính sách giờ và quyền truy cập | Các cờ booked/done độc lập dễ tạo trạng thái mâu thuẫn |
| State — VaccinationRecord | [ScheduledState:14](../src/main/java/com/petcare/domain/vaccination/ScheduledState.java#L14); [VaccinationRecord:1](../src/main/java/com/petcare/domain/VaccinationRecord.java#L1) | Phân loại theo ngày và ngăn sửa mũi đã tiêm | Tách State đang hoạt động và kết thúc; kiểm thử ca biên riêng | So sánh ngày rải rác dễ coi đúng ngày hẹn là quá hạn |
| State — Subscription | [ActiveState:8](../src/main/java/com/petcare/domain/subscription/ActiveState.java#L8); [Subscription:1](../src/main/java/com/petcare/domain/Subscription.java#L1) | Số lượt, hết hạn, gia hạn và hủy | Quy tắc theo State kết hợp thời hạn rõ ràng | Setter trạng thái/lượt dễ làm gói hủy hồi sinh hoặc lượt âm |
| Strategy — Matching | [MatchingService:44](../src/main/java/com/petcare/service/MatchingService.java#L44); [MatchingStrategy:1](../src/main/java/com/petcare/service/matching/MatchingStrategy.java#L1) | Hoán đổi thuật toán cho cùng câu trả lời | Registry nhận bean Strategy, service không biết cách tính điểm | if theo chế độ trong controller làm trộn lọc và tính điểm |
| Strategy — Discount | [LoyaltyPricingService:23](../src/main/java/com/petcare/service/LoyaltyPricingService.java#L23); [DiscountStrategy:1](../src/main/java/com/petcare/domain/discount/DiscountStrategy.java#L1) | Giảm giá theo hạng và cấu hình | Bốn lớp BronzeDiscount/SilverDiscount/GoldDiscount/PlatinumDiscount dùng chung làm tròn | if theo hạng ở nhiều luồng dễ lệch giá đơn và lịch |
| Simple Factory — Product | [ProductFactory:35](../src/main/java/com/petcare/domain/factory/ProductFactory.java#L35); [ProductService:1](../src/main/java/com/petcare/service/ProductService.java#L1) | Tạo đúng loại từ ProductForm | Registry creator tập trung năm loại sản phẩm | new từng loại trong controller làm phụ thuộc lớp cụ thể và lặp khởi tạo |
| Observer — điểm và gói | [LoyaltyEventListener:15](../src/main/java/com/petcare/event/LoyaltyEventListener.java#L15); [SubscriptionEventListener:1](../src/main/java/com/petcare/event/SubscriptionEventListener.java#L1) | Hoàn tất đơn/lịch kích hoạt điểm; đơn cấp quyền lợi | Listener đồng bộ MANDATORY giữ tính nguyên tử; sourceKey chống phát lại | Service đơn gọi trực tiếp mọi module làm tăng liên kết; tách sau commit có thể mất đồng bộ |
| Observer — tiêm phòng | [VaccinationStatusChangedListener:12](../src/main/java/com/petcare/event/VaccinationStatusChangedListener.java#L12); [VaccinationRecord:1](../src/main/java/com/petcare/domain/VaccinationRecord.java#L1) | Thông báo chuyển sắp đến hạn/quá hạn sau commit | AbstractAggregateRoot đăng ký sự kiện; listener ghi log sau giao dịch | Ghi log trước commit có thể báo thay đổi đã rollback |
| Template Method — quy tắc mua | [TypedPurchasePolicy:11](../src/main/java/com/petcare/domain/purchase/TypedPurchasePolicy.java#L11); [AccessoryPurchasePolicy:1](../src/main/java/com/petcare/domain/purchase/AccessoryPurchasePolicy.java#L1) | Kiểm tra chung trước kiểm tra số lượng từng loại | validate() final gọi hook validateQuantity(); trình tự không bị lớp con bỏ qua | Lặp validate cho từng loại dễ quên kiểm tra active/số lượng dương |
| Template Method — chấm điểm | [AbstractScoreStrategy:14](../src/main/java/com/petcare/service/matching/AbstractScoreStrategy.java#L14); [BudgetFirstStrategy:1](../src/main/java/com/petcare/service/matching/BudgetFirstStrategy.java#L1) | Giữ quy trình lọc/chấm/xếp hạng, thay trọng số | match()/score() gọi weight(); match không final nên khung chưa bị khóa bằng ngôn ngữ | Nhân bản hai thuật toán dễ lệch lọc cứng và thứ tự hòa điểm |
| Adapter — quy tắc mua có kiểu | [AccessoryPurchasePolicy:12](../src/main/java/com/petcare/domain/purchase/AccessoryPurchasePolicy.java#L12); [TypedPurchasePolicy:1](../src/main/java/com/petcare/domain/purchase/TypedPurchasePolicy.java#L1) | Chuẩn hóa API mua/hoàn cho loại Product và proxy Hibernate | ProductPurchasePolicy bọc thao tác toForm/update, entity() unproxy và cast có kiểu | Service phải cast và cập nhật riêng từng loại ở nhiều nơi |
| Repository — Spring Data JPA | [OrderRepository:11](../src/main/java/com/petcare/repository/OrderRepository.java#L11); [ProductRepository:1](../src/main/java/com/petcare/repository/ProductRepository.java#L1) | Tách truy vấn/persistence khỏi nghiệp vụ | Interface JpaRepository và JPQL/projection tái dùng, mock được | SQL/EntityManager rải trong controller làm khó kiểm thử và sửa truy vấn |

## State — Order

Minh họa trích từ [Order:70](../src/main/java/com/petcare/domain/Order.java#L70):

```java
    public void confirm() { stateName = getState().confirm(this); }
    public void ship() { stateName = getState().ship(this); }
    public void complete() { stateName = getState().complete(this); }
```

Test chứng minh: [OrderStateTest#verifiesEveryTransitionAndRejectsInvalidActions](../src/test/java/com/petcare/OrderStateTest.java).

## State — Appointment

Minh họa trích từ [Appointment:86](../src/main/java/com/petcare/domain/Appointment.java#L86):

```java
    public void start() { stateName = getState().start(this); }
    public void finish() { stateName = getState().finish(this); }
    public void cancel() { stateName = getState().cancel(this); }
```

Test chứng minh: [AppointmentStateTest#testsWholeTransitionTable](../src/test/java/com/petcare/AppointmentStateTest.java).

## State — VaccinationRecord

Minh họa trích từ [ScheduledState:14](../src/main/java/com/petcare/domain/vaccination/ScheduledState.java#L14):

```java
    public VaccinationStatus checkTime(VaccinationRecord record, LocalDate today) {
        long remaining = ChronoUnit.DAYS.between(today, record.getScheduledDate());
        if (remaining < 0) return VaccinationStatus.OVERDUE;
        if (remaining <= dueSoonDays) return VaccinationStatus.DUE_SOON;
        return VaccinationStatus.SCHEDULED;
    }
```

Test chứng minh: [VaccinationStateTest#scheduledChecksExactDateBoundaries](../src/test/java/com/petcare/VaccinationStateTest.java).

## State — Subscription

Minh họa trích từ [ActiveState:8](../src/main/java/com/petcare/domain/subscription/ActiveState.java#L8):

```java
    public SubscriptionStatus useOnce(Subscription s, LocalDate today) {
        if (!s.isUsableOn(today)) throw invalid();
        return s.getRemainingUses() == 1 ? SubscriptionStatus.EXPIRED : SubscriptionStatus.ACTIVE;
    }
```

Test chứng minh: [SubscriptionStateTest#inclusiveExpiryAndLastUseAndRefund](../src/test/java/com/petcare/SubscriptionStateTest.java).

## Strategy — Matching

Minh họa trích từ [MatchingService:44](../src/main/java/com/petcare/service/MatchingService.java#L44):

```java
        var strategy = strategies.get(key);
        if (strategy == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chiến lược ghép đôi không hợp lệ.");
        var owner = email == null ? null : users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        var matches = strategy.match(answers, pets.findCandidates()).stream().limit(3).toList();
        var result = new QuizResult(owner, answers, key, strategy.label(), LocalDateTime.now(clock), matches);
        if (owner != null) history.save(result);
        return QuizReport.from(result);
```

Test chứng minh: [MatchingStrategyTest#strategiesReverseRankingForSameAnswers](../src/test/java/com/petcare/MatchingStrategyTest.java).

## Strategy — Discount

Minh họa trích từ [LoyaltyPricingService:23](../src/main/java/com/petcare/service/LoyaltyPricingService.java#L23):

```java
        var tier = account == null ? MembershipTier.BRONZE : account.currentTier(properties);
        var afterTier = strategies.forTier(tier).apply(amount);
        var voucher = useVoucher && account != null && account.isVoucherAvailable()
            ? afterTier.min(new BigDecimal("10000.00")) : BigDecimal.ZERO;
        return new LoyaltyQuote(properties.tier(tier).getName(), amount.subtract(afterTier), voucher, afterTier.subtract(voucher));
```

Test chứng minh: [LoyaltyDomainTest#strategyUsesTierRate](../src/test/java/com/petcare/LoyaltyDomainTest.java).

## Simple Factory — Product

Minh họa trích từ [ProductFactory:35](../src/main/java/com/petcare/domain/factory/ProductFactory.java#L35):

```java
    public Product create(ProductForm f) {
        return creators.get(Objects.requireNonNull(f.getType())).apply(f);
    }
```

Test chứng minh: [ProductFactoryTest#createsCorrectSubtype](../src/test/java/com/petcare/ProductFactoryTest.java).

## Observer — điểm và gói

Minh họa trích từ [LoyaltyEventListener:15](../src/main/java/com/petcare/event/LoyaltyEventListener.java#L15):

```java
    @EventListener @Transactional(propagation = Propagation.MANDATORY)
    public void onOrder(OrderCompletedEvent event) { loyalty.earnOrder(event.orderId()); }
    @EventListener @Transactional(propagation = Propagation.MANDATORY)
    public void onAppointment(AppointmentCompletedEvent event) { loyalty.earnAppointment(event.appointmentId()); }
```

Test chứng minh: [LoyaltyIntegrationTest#rollbackCompletionAlsoRollsBackPointsAndLedger](../src/test/java/com/petcare/LoyaltyIntegrationTest.java).

## Observer — tiêm phòng

Minh họa trích từ [VaccinationStatusChangedListener:12](../src/main/java/com/petcare/event/VaccinationStatusChangedListener.java#L12):

```java
    @TransactionalEventListener
    public void onStatusChanged(VaccinationStatusChangedEvent event) {
        String label = new VaccinationStateFactory(VaccinationStateFactory.DEFAULT_DUE_SOON_DAYS)
            .from(event.getCurrentState()).getLabel();
        log.info("Lịch tiêm #{}: {} — {}, ngày hẹn {}, ngày kiểm tra {}.", event.getRecordId(),
            event.getVaccineName(), label, event.getScheduledDate(), event.getObservedOn());
    }
```

Test chứng minh: [PetHealthPersistenceTest#rollbackDoesNotLogUncommittedStatus](../src/test/java/com/petcare/PetHealthPersistenceTest.java).

## Template Method — quy tắc mua

Minh họa trích từ [TypedPurchasePolicy:11](../src/main/java/com/petcare/domain/purchase/TypedPurchasePolicy.java#L11):

```java
    @Override public final void validate(Product product, int quantity) {
        if (quantity <= 0) throw new PurchaseException("Số lượng phải lớn hơn 0.");
        if (!product.isActive()) throw new PurchaseException("Sản phẩm đã ẩn, không thể mua.");
        validateQuantity(entity(product), quantity);
    }
    protected abstract void validateQuantity(T product, int quantity);
```

Test chứng minh: [CartPurchaseTest#hiddenProductAndNonPositiveQuantityAreRejected](../src/test/java/com/petcare/CartPurchaseTest.java).

## Template Method — chấm điểm

Minh họa trích từ [AbstractScoreStrategy:14](../src/main/java/com/petcare/service/matching/AbstractScoreStrategy.java#L14):

```java
    @Override public List<MatchResult> match(QuizAnswers answers, List<Pet> candidates) {
        properties.validate();
        return candidates.stream().filter(p -> eligible(answers, p)).map(p -> score(answers, p))
            .sorted(Comparator.comparingDouble(MatchResult::scorePercent).reversed()
                .thenComparing(r -> r.pet().getId(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(r -> r.pet().getName()))
            .toList();
    }

```

Test chứng minh: [MatchingStrategyTest#scoreBreakdownAddsUpAndTiesUseId](../src/test/java/com/petcare/MatchingStrategyTest.java).

## Adapter — quy tắc mua có kiểu

Minh họa trích từ [AccessoryPurchasePolicy:12](../src/main/java/com/petcare/domain/purchase/AccessoryPurchasePolicy.java#L12):

```java
    @Override public void consume(Product product, int quantity) {
        validate(product, quantity);
        var accessory = entity(product);
        var form = accessory.toForm();
        form.setStockQuantity(accessory.getStockQuantity() - quantity);
        accessory.update(form);
    }
```

Test chứng minh: [CartPurchaseTest#accessoryAggregatesQuantityAndRestoresStock](../src/test/java/com/petcare/CartPurchaseTest.java).

## Repository — Spring Data JPA

Minh họa trích từ [OrderRepository:11](../src/main/java/com/petcare/repository/OrderRepository.java#L11):

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    long countByStateName(OrderStatus stateName);
```

Test chứng minh: [OrderSummaryRepositoryTest#summaryContainsSnapshotMoneyLineCountAndStateActionsWithoutLoadingEntities](../src/test/java/com/petcare/OrderSummaryRepositoryTest.java).

## Các lớp đi kèm và phạm vi nhận định

- State Order: PendingState, ConfirmedState, ShippingState, CompletedState, CancelledState và AbstractOrderState trong `domain/order/`. Appointment: BookedState, InProgressState, DoneState, CancelledState trong `domain/appointment/`. Vaccination: ScheduledState, DueSoonState, OverdueState, CompletedState, ActiveVaccinationState trong `domain/vaccination/`. Subscription: ActiveState, ExpiredState, CancelledState trong `domain/subscription/`. Xem [UML](uml-class-diagram.md) để tránh nhầm lớp cùng tên.

- Registry còn có OrderStateFactory, AppointmentStateFactory, VaccinationStateFactory, SubscriptionStates, DiscountStrategyFactory, PurchasePolicies. Chúng ánh xạ khóa sang hành vi; không phải cây Factory Method có lớp con ghi đè thao tác tạo.

- Hai Strategy ghép đôi là WeightedScoreStrategy và BudgetFirstStrategy. Các Adapter mua gồm PetPurchasePolicy, AccessoryPurchasePolicy, AppointmentPurchasePolicy, PackagePurchasePolicy; chúng cũng đóng vai trò chính sách có thể thay thế qua ProductPurchasePolicy.

- SubscriptionEventListener ưu tiên @Order(-10). [SubscriptionIntegrationTest#purchaseDiscountCompletionPointsReplayAndSubscriptionAreAtomic](../src/test/java/com/petcare/SubscriptionIntegrationTest.java) và [SubscriptionIntegrationTest#rollbackCompletionRollsBackGrantAndPoints](../src/test/java/com/petcare/SubscriptionIntegrationTest.java) chứng minh cấp gói/điểm/hoàn tất đơn cùng giao dịch. [SubscriptionIntegrationTest#purchaseSnapshotsPackageUsesMonthsAndServicesBeforeCatalogChanges](../src/test/java/com/petcare/SubscriptionIntegrationTest.java) kiểm tra bản chụp quyền lợi không đổi khi sửa danh mục.

- Không tìm thấy Builder hay Decorator nghiệp vụ trong `src/main/java`. Constructor, static create(), DTO record và Lombok @RequiredArgsConstructor không tự trở thành Builder. Voucher cộng vào phép tính trong LoyaltyPricingService, không bọc DiscountStrategy nên không gọi là Decorator.

- VaccinationScheduleTemplate là lớp final sinh lịch từ registry dữ liệu; tên “Template” không đủ để gọi GoF Template Method. Template Method thật nằm ở TypedPurchasePolicy và AbstractScoreStrategy như trên.

- MutableClock là Clock tùy biến kế thừa java.time.Clock và quản lý ngày demo, không tự gán nhãn Decorator. DTO/bản chụp và Spring MVC là lựa chọn kiến trúc; không cần bịa thêm GoF pattern để đủ số lượng.
