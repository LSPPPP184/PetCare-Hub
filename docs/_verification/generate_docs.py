"""Sinh tài liệu từ các nguồn đã đối chiếu; chỉ ghi bên trong docs/."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / 'docs'
MAIN = ROOT / 'src/main/java/com/petcare'
TEST = ROOT / 'src/test/java/com/petcare'

def source(path):
    return (MAIN / path).read_text(encoding='utf-8')

def link(path, needle=None):
    n = next((i for i, s in enumerate(source(path).splitlines(), 1) if needle and needle in s), 1)
    return f'[{Path(path).stem}:{n}](../src/main/java/com/petcare/{path}#L{n})'

def test(name):
    cls, method = name.split('#')
    assert method in (TEST / (cls+'.java')).read_text(encoding='utf-8'), name
    return f'[{name}](../src/test/java/com/petcare/{cls}.java)'

def excerpt(path, needle, count):
    rows = source(path).splitlines()
    start = next(i for i, row in enumerate(rows) if needle in row)
    assert count < 20
    return '```java\n'+'\n'.join(rows[start:start+count])+'\n```'

def write(name, body):
    body = re.sub(r'(?m)(?<!\n)\n(## [^\n]+)\n', r'\n\n\1\n\n', body)
    body = re.sub(r'(?m)^(## [^\n]+)\n(?!\n)', r'\1\n\n', body)
    (DOCS/name).write_text(body.strip()+'\n', encoding='utf-8')

intro = 'Đối chiếu mã nguồn ngày 22/11/2026, commit `1598cc754b3e95252fe61bcca8134cf6d0808fcc`. Tên định danh giữ nguyên theo Java; phần giải thích bằng tiếng Việt.'

uml = ['# Sơ đồ lớp PetCare Hub', intro,
       'Các khối độc lập: những lớp trùng tên như `CancelledState` thuộc package ghi trong từng mục. Mũi tên nét đứt là phụ thuộc, không phải khóa ngoại; đối tượng State được dựng từ enum, không lưu thành entity. Chỉ trình bày thành viên liên quan đến chủ đề, không liệt kê mọi getter do Lombok sinh. Ký hiệu phương thức được rút gọn; dấu () không khẳng định phương thức Java không có tham số. Chữ ký đầy đủ nằm trong file nguồn dẫn kèm.']

def diagram(title, description, code, refs):
    uml.extend([f'## {title}', description, '```mermaid\nclassDiagram\n'+code.strip()+'\n```', 'Nguồn: '+', '.join(link(p) for p in refs)+'.'])

diagram('1. Cây kế thừa Product',
        '`Product` và `ServiceItem` là lớp trừu tượng. Giá gọi qua `calculatePrice()`; `GroomingService` cộng phụ phí, các loại còn lại trả giá gốc. JPA dùng JOINED, kể cả bảng trung gian `service_items`. `MonthlyCarePackage` vừa là sản phẩm bán được vừa cung cấp quyền lợi cho Subscription.', '''
Product <|-- Pet
Product <|-- Accessory
Product <|-- ServiceItem
ServiceItem <|-- GroomingService
ServiceItem <|-- VetService
ServiceItem <|-- MonthlyCarePackage
Product : <<abstract>>
Product : Long id
Product : long version
Product : BigDecimal basePrice
Product : calculatePrice() BigDecimal
Product : updateCatalog(ProductForm f)
Product : subscriptionPackage()
ServiceItem : <<abstract>>
ServiceItem : Integer durationMinutes
Pet : PetStatus status
Accessory : Integer stockQuantity
GroomingService : BigDecimal petSizeSurcharge
VetService : String doctorName
MonthlyCarePackage : Integer durationMonths
MonthlyCarePackage : Integer includedUses
MonthlyCarePackage : configureCoveredServices(services)
MonthlyCarePackage "0..*" --> "0..*" ServiceItem : coveredServices
Pet "1" --> "0..1" PetProfile : profile
''', ['domain/Product.java','domain/ServiceItem.java','domain/MonthlyCarePackage.java'])

state_specs = [
 ('Order','order','OrderState','AbstractOrderState','OrderStateFactory', ['PendingState','ConfirmedState','ShippingState','CompletedState','CancelledState'],
  'Luồng chính PENDING → CONFIRMED → SHIPPING → COMPLETED; PENDING/CONFIRMED có thể hủy. Chỉ hủy từ CONFIRMED hoàn kho. State quyết định chuyển, OrderService xử lý kho và sự kiện trong giao dịch.', ['confirm','ship','complete','cancel']),
 ('Appointment','appointment','AppointmentState','AbstractAppointmentState','AppointmentStateFactory', ['BookedState','InProgressState','DoneState','CancelledState'],
  'BOOKED → IN_PROGRESS → DONE; chỉ BOOKED cho hủy. Quy tắc giờ làm việc, trùng lịch và khách hủy trước ít nhất hai giờ nằm ở AppointmentPolicy/AppointmentService, không nằm hết trong State.', ['book','start','finish','cancel']),
 ('VaccinationRecord','vaccination','VaccinationState','AbstractVaccinationState','VaccinationStateFactory', ['ScheduledState','DueSoonState','OverdueState','CompletedState'],
  'SCHEDULED → DUE_SOON → OVERDUE theo ngày; có thể đi thẳng từ SCHEDULED tới OVERDUE. Ba State còn hoạt động cho hoàn tất hoặc đổi lịch; COMPLETED là kết thúc. Đổi lịch phân loại lại ngay theo ngày mới. Ngưỡng mặc định bảy ngày, đúng ngày hẹn vẫn DUE_SOON.', ['checkTime','complete','reschedule']),
 ('Subscription','subscription','SubscriptionState','AbstractSubscriptionState','SubscriptionStates', ['ActiveState','ExpiredState','CancelledState'],
  'ACTIVE dùng lượt; lượt cuối hoặc quá endDate chuyển EXPIRED. ACTIVE gần hết hạn và EXPIRED có thể gia hạn qua đơn hàng; CANCELLED không hồi sinh. Subscription giữ số lượt/dịch vụ đã chốt. Hoàn lượt do hủy lịch là hành vi riêng restoreUse(), chỉ có thể hồi phục gói hết lượt nhưng chưa quá ngày.', ['useOnce','expire','cancel','renew'])]

for index, (entity,pkg,interface,abstract,factory,states,desc,methods) in enumerate(state_specs,2):
    lines = [f'{interface} : <<interface>>', f'{abstract} : <<abstract>>', f'{interface} <|.. {abstract}',
             f'{entity} ..> {interface} : getState()', f'{entity} ..> {factory} : dựng State',
             f'{factory} ..> {interface} : from(status)', f'{entity} : {entity.replace("Record", "")}Status stateName' if entity != 'VaccinationRecord' else f'{entity} : VaccinationStatus stateName']
    for method in methods:
        lines.append(f'{interface} : {method}()')
    for state in states:
        parent = 'ActiveVaccinationState' if pkg == 'vaccination' and state != 'CompletedState' else abstract
        lines.append(f'{parent} <|-- {state}')
    if pkg == 'vaccination':
        lines += [f'{abstract} <|-- ActiveVaccinationState','ActiveVaccinationState : <<abstract>>','ScheduledState : int dueSoonDays']
    if pkg == 'subscription':
        lines += ['Subscription : int remainingUses','Subscription : LocalDate endDate','Subscription : restoreUse(LocalDate today)',
                  'OrderItem ..> SubscriptionBenefits : subscriptionBenefits()', 'Subscription ..> SubscriptionBenefits : createFromBenefits()',
                  'Subscription "0..*" --> "1" MonthlyCarePackage : carePackage','Subscription "0..*" --> "0..*" ServiceItem : coveredServices']
    diagram(f'{index}. State {entity} — domain.{pkg}', desc, '\n'.join(lines),[f'domain/{entity}.java',f'domain/{pkg}/{interface}.java',f'domain/{pkg}/{factory}.java'])

diagram('6. Strategy MatchingStrategy',
        '`MatchingService` nhận danh sách Strategy qua constructor và chọn theo key. Hai thuật toán chia sẻ lọc cứng, chuẩn hóa, giải thích, xếp hạng; khác hook weight(). Kết quả tối đa ba thú cưng, có bản chụp khi người dùng đăng nhập.', '''
MatchingStrategy : <<interface>>
MatchingStrategy : match(answers, candidates)
MatchingStrategy : key() String
MatchingStrategy : label() String
AbstractScoreStrategy : <<abstract>>
AbstractScoreStrategy : weight(MatchCriterion criterion) double
AbstractScoreStrategy : match(answers, candidates)
MatchingStrategy <|.. AbstractScoreStrategy
AbstractScoreStrategy <|-- WeightedScoreStrategy
AbstractScoreStrategy <|-- BudgetFirstStrategy
MatchingService --> MatchingStrategy : strategies
AbstractScoreStrategy --> MatchingProperties : properties
AbstractScoreStrategy ..> MatchCriterion : tính điểm
MatchingService ..> QuizResult : lưu bản chụp
''',['service/MatchingService.java','service/matching/AbstractScoreStrategy.java'])

diagram('7. Strategy DiscountStrategy',
        '`apply(amount)` trả số tiền phải trả sau giảm, không phải số tiền giảm. Bốn Strategy nhận tỷ lệ từ LoyaltyProperties; LoyaltyPricingService chốt giá và áp dụng voucher riêng. Voucher không được triển khai bằng Decorator.', '''
DiscountStrategy : <<interface>>
DiscountStrategy : discountRate() BigDecimal
DiscountStrategy : apply(BigDecimal amount) BigDecimal
AbstractTierDiscount : <<abstract>>
DiscountStrategy <|.. AbstractTierDiscount
AbstractTierDiscount <|-- BronzeDiscount
AbstractTierDiscount <|-- SilverDiscount
AbstractTierDiscount <|-- GoldDiscount
AbstractTierDiscount <|-- PlatinumDiscount
DiscountStrategyFactory --> DiscountStrategy : strategies
LoyaltyPricingService --> DiscountStrategyFactory : strategies
AbstractTierDiscount --> LoyaltyProperties : properties
''',['domain/discount/DiscountStrategy.java','domain/discount/DiscountStrategyFactory.java','service/LoyaltyPricingService.java'])

diagram('8. Factory ProductFactory',
        'Đây là Simple Factory dùng registry Map<ProductType, Function<ProductForm, Product>>, không phải họ Abstract Factory. Mỗi creator tạo lớp cụ thể rồi gọi update(form); ProductService gọi create khi thêm sản phẩm.', '''
ProductService --> ProductFactory : factory
ProductFactory : create(ProductForm f) Product
ProductFactory ..> ProductForm : đầu vào
ProductFactory ..> ProductType : khóa creators
ProductFactory ..> Product : kiểu trả về
ProductFactory ..> Pet : tạo
ProductFactory ..> Accessory : tạo
ProductFactory ..> GroomingService : tạo
ProductFactory ..> VetService : tạo
ProductFactory ..> MonthlyCarePackage : tạo
''',['domain/factory/ProductFactory.java','service/ProductService.java'])
uml += ['## Đồng hồ, sự kiện và giao diện',
        '`AppointmentClockConfig.appointmentClock()` dùng giờ Asia/Ho_Chi_Minh. Bean `healthClock` có `defaultCandidate=false`: ngoài demo dùng đồng hồ thực, trong demo dùng `MutableClock`. `DemoDateService.simulate()` chỉ chạy VaccinationScheduler; không giả lập đồng hồ của lịch hẹn, gói tháng hay doanh thu. MutableClock kế thừa java.time.Clock, chia sẻ trạng thái giữa các múi giờ và chỉ công bố ngày sau khi tác vụ thành công.',
        '`LoyaltyEventListener` và `SubscriptionEventListener` nhận OrderCompletedEvent trong giao dịch; lỗi có thể rollback cả đơn, điểm và cấp gói. VaccinationStatusChangedListener chỉ ghi log sau commit. Chi tiết và test ở [patterns.md](patterns.md).',
        'Giao diện hướng A đang dùng `src/main/resources/static/css/petcare.css`, `templates/fragments/layout.html` và các template Thymeleaf. Các bản trong `docs/ui/preview/` chỉ là bản xem trước, không dùng làm bằng chứng nghiệp vụ.']
write('uml-class-diagram.md','\n\n'.join(uml))

# ERD logic: tên entity/field thật; ánh xạ bảng được ghi riêng để không giả định DDL đã triển khai.
entities = {}
for file in sorted((MAIN/'domain').glob('*.java')):
    text = file.read_text(encoding='utf-8')
    if re.search(r'@Entity\b',text): entities[file.stem] = text
assert len(entities) == 22
field_re = re.compile(r'\b(?:private|protected)\s+(?!static\b)(?:final\s+)?([\w.]+(?:<[^;=]+?>)?)\s+(\w+)\s*(?=[;=])')
erd = ['# Mô hình dữ liệu',intro,
       'ERD logic đầy đủ **22 entity JPA**. Tên khối chính là class; thuộc tính giữ tên field Java, không tự đổi sang tên cột SQL. Bảng ánh xạ bên dưới nêu tên bảng vật lý. Các field tham chiếu có kiểu entity được đánh FK; JPA lưu ID của đối tượng, không lưu cả object. Đây là ánh xạ từ code, chưa phải kết quả truy vấn schema SQL Server đang chạy.',
       '`Product` dùng `@Inheritance(strategy=InheritanceType.JOINED)`: ID của các bảng con đồng thời là PK/FK tới bảng cha. `ServiceItem` là entity trừu tượng có bảng riêng. Quan hệ cha → con là 1 → 0..1 cho từng loại; một Product cụ thể thuộc đúng một nhánh thực tế, không đồng thời mọi loại.',
       '```mermaid\nerDiagram']
relations = []
parents = {'Pet':'Product','Accessory':'Product','ServiceItem':'Product','GroomingService':'ServiceItem','VetService':'ServiceItem','MonthlyCarePackage':'ServiceItem'}
for cls, text in entities.items():
    erd.append(f'    {cls} {{')
    if cls in parents: erd.append('        Long id PK,FK "kế thừa ID bảng cha"')
    for typ, field in field_re.findall(text):
        if '<' in typ: continue
        if typ == 'QuizAnswers':
            for t,f in field_re.findall(source('dto/QuizAnswers.java')):
                erd.append(f'        {t} {f} "nhúng từ answers"')
            continue
        if cls == 'Pet' and field == 'profile':
            continue  # mappedBy: FK nằm ở PetProfile.pet, không có cột profile trên Pet.
        key = ' PK' if field == 'id' else (' FK' if typ in entities else '')
        if cls == 'SubscriptionGrant' and field == 'id': key = ' PK,FK'
        note = ' "EnumType.STRING"' if typ.endswith('Status') or typ in ['Role','Species','MembershipTier','PointTransactionType'] else ''
        erd.append(f'        {typ} {field}{key}{note}')
        if typ in entities and not (cls == 'Pet' and field == 'profile'):
            at = text.index('private '+typ+' '+field) if 'private '+typ+' '+field in text else 0
            previous = text[:at]
            annotation_pos = max(previous.rfind('@ManyToOne'),previous.rfind('@OneToOne'))
            annotation = previous[annotation_pos:]
            required = 'optional = false' in annotation or 'optional=false' in annotation
            left = '||' if required else 'o|'
            right = 'o|' if '@OneToOne' in annotation else 'o{'
            if cls == 'Cart' and field == 'customer': right='o|'
            relations.append(f'    {typ} {left}--{right} {cls} : {field}')
    erd.append('    }')
for child,parent in parents.items(): relations.append(f'    {parent} ||--o| {child} : JOINED')
for name,owner,owner_col in [('monthly_package_services','MonthlyCarePackage','package_id'),('subscription_services','Subscription','subscription_id'),('order_item_snapshot_services','OrderItem','order_item_id')]:
    erd += [f'    {name} {{', f'        Long {owner_col} PK,FK', '        Long service_id PK,FK','    }']
    relations += [f'    {owner} ||--o{{ {name} : chứa',f'    ServiceItem ||--o{{ {name} : dịch_vụ']
erd += ['    quiz_matches {','        Long quiz_id PK,FK','        int rank_index PK']
for t,f in field_re.findall(source('domain/QuizMatchSnapshot.java')): erd.append(f'        {t} {f}')
erd += ['    }','    QuizResult ||--o{ quiz_matches : matches'] + relations + ['```']
erd += ['## Ánh xạ entity → bảng', '| Entity | Bảng khai báo bằng @Table |','| --- | --- |']
for cls,text in entities.items():
    table = re.search(r'@Table\(name\s*=\s*"([^"]+)"',text).group(1)
    erd.append(f'| {link("domain/"+cls+".java")} | `{table}` |')
erd += ['## Cột trạng thái và cấu trúc bổ sung',
        '- `Order.stateName`, `Appointment.stateName`, `VaccinationRecord.stateName`, `Subscription.stateName` và `Pet.status` dùng `@Enumerated(EnumType.STRING)`: lưu chữ như PENDING, BOOKED, SCHEDULED, ACTIVE, AVAILABLE; không lưu ordinal và không lưu object State. Tên cột mặc định theo naming strategy là `state_name` hoặc `status`.',
        '- `role`, `species`, `petSpecies`, `tier`, `PointTransaction.type` và `QuizAnswers.homeSize` cũng dùng enum dạng chữ. `hibernate.use_nationalized_character_data=true` yêu cầu ánh xạ chuỗi sang nvarchar trên SQL Server; cần kiểm tra schema cũ thực tế, vì cấu hình không chứng minh mọi cột đã được chuyển đổi.',
        '- `QuizAnswers` là @Embeddable, bảy field nằm trong `quiz_results`, không có bảng/entity độc lập. `QuizMatchSnapshot` là @Embeddable trong @ElementCollection: bảng `quiz_matches`, có `quiz_id` và `rank_index`. `petId` chỉ là giá trị chụp lịch sử, **không có quan hệ JPA/FK tới Pet**.',
        '- Ba bảng nối Set là `monthly_package_services`, `subscription_services`, `order_item_snapshot_services`; cặp ID biểu diễn khóa ghép của tập quan hệ. Không nhầm các bảng nối với entity Java.',
        '- `OrderItem` chụp `unitPrice`, `snapshotPackage`, `snapshotIncludedUses`, `snapshotDurationMonths`, `snapshotServices`; `Subscription` giữ `coveredServices`, số lượt và thời hạn đã cấp. Snapshot lưu tập tham chiếu dịch vụ, không sao chép toàn bộ tên/thuộc tính của ServiceItem.',
        '- `SubscriptionGrant.id` dùng @MapsId từ `order_id`; một đơn có tối đa một biên nhận, `subscription` trỏ gói đầu tiên của lô. Một đơn nhiều đơn vị có thể cấp nhiều Subscription qua sourceKey; không vẽ quan hệ trực tiếp Order → Subscription cho mua mới vì không có field đó.',
        '- `Appointment.subscription` và `subscriptionUseRefunded` ghi nguồn lượt dùng/hoàn. `Appointment` chỉ có `petName`/`petSpecies`, **không FK tới OwnedPet**. `OwnedPet` độc lập với Pet bán trong danh mục; tạo từ đơn hoàn tất không thiết lập quan hệ bền vững tới Order/Pet.',
        '## Ràng buộc đáng chú ý',
        '`Cart.customer` duy nhất; cặp cart_id/product_id duy nhất. User.email, PetProfile.pet/seedKey, LoyaltyAccount.customer, PointTransaction.sourceKey, Subscription.sourceKey có ràng buộc duy nhất. Không suy diễn mọi seedKey đều UNIQUE: nhiều seed được bảo vệ bằng khóa và kiểm tra trong service. `@Version` có ở Product, Cart, Order, Appointment, OwnedPet, VaccinationRecord, PetProfile, LoyaltyAccount, Subscription. Ràng buộc CHECK giữ điểm không âm và 0 ≤ remainingUses ≤ totalUses.',
        'Để xác minh SQL Server: xem bước kiểm tra thủ công trong [demo-script.md](demo-script.md).']
write('erd.md','\n\n'.join(erd[:4])+'\n\n'+'\n'.join(erd[4:]))

patterns = ['# Pattern thực sự có trong code',intro,
            'Các test dưới đây là bằng chứng trong source test; kết quả thực thi được ghi riêng ở hồ sơ kiểm chứng. Simple Factory và Repository được gọi đúng phạm vi, không đồng nhất mọi lớp có tên Factory với GoF Factory Method.']
rows = [
 ('State — Order','domain/Order.java','confirm()',3,'domain/order/OrderState.java','Ràng buộc vòng đời đơn và hoàn kho khi hủy','Mỗi State mở đúng hành động; enum lưu chữ, hành vi đa hình','Chuỗi if/switch lặp ở controller/service dễ cho phép ship hoặc hoàn kho sai','OrderStateTest#verifiesEveryTransitionAndRejectsInvalidActions'),
 ('State — Appointment','domain/Appointment.java','public void start()',3,'domain/appointment/AppointmentState.java','Không hoàn tất lịch khi chưa bắt đầu','State tách vòng đời khỏi chính sách giờ và quyền truy cập','Các cờ booked/done độc lập dễ tạo trạng thái mâu thuẫn','AppointmentStateTest#testsWholeTransitionTable'),
 ('State — VaccinationRecord','domain/vaccination/ScheduledState.java','public VaccinationStatus checkTime',6,'domain/VaccinationRecord.java','Phân loại theo ngày và ngăn sửa mũi đã tiêm','Tách State đang hoạt động và kết thúc; kiểm thử ca biên riêng','So sánh ngày rải rác dễ coi đúng ngày hẹn là quá hạn','VaccinationStateTest#scheduledChecksExactDateBoundaries'),
 ('State — Subscription','domain/subscription/ActiveState.java','public SubscriptionStatus useOnce',4,'domain/Subscription.java','Số lượt, hết hạn, gia hạn và hủy','Quy tắc theo State kết hợp thời hạn rõ ràng','Setter trạng thái/lượt dễ làm gói hủy hồi sinh hoặc lượt âm','SubscriptionStateTest#inclusiveExpiryAndLastUseAndRefund'),
 ('Strategy — Matching','service/MatchingService.java','var strategy = strategies.get(key)',8,'service/matching/MatchingStrategy.java','Hoán đổi thuật toán cho cùng câu trả lời','Registry nhận bean Strategy, service không biết cách tính điểm','if theo chế độ trong controller làm trộn lọc và tính điểm','MatchingStrategyTest#strategiesReverseRankingForSameAnswers'),
 ('Strategy — Discount','service/LoyaltyPricingService.java','var tier = account == null',5,'domain/discount/DiscountStrategy.java','Giảm giá theo hạng và cấu hình','Bốn lớp BronzeDiscount/SilverDiscount/GoldDiscount/PlatinumDiscount dùng chung làm tròn','if theo hạng ở nhiều luồng dễ lệch giá đơn và lịch','LoyaltyDomainTest#strategyUsesTierRate'),
 ('Simple Factory — Product','domain/factory/ProductFactory.java','public Product create',3,'service/ProductService.java','Tạo đúng loại từ ProductForm','Registry creator tập trung năm loại sản phẩm','new từng loại trong controller làm phụ thuộc lớp cụ thể và lặp khởi tạo','ProductFactoryTest#createsCorrectSubtype'),
 ('Observer — điểm và gói','event/LoyaltyEventListener.java','@EventListener @Transactional',4,'event/SubscriptionEventListener.java','Hoàn tất đơn/lịch kích hoạt điểm; đơn cấp quyền lợi','Listener đồng bộ MANDATORY giữ tính nguyên tử; sourceKey chống phát lại','Service đơn gọi trực tiếp mọi module làm tăng liên kết; tách sau commit có thể mất đồng bộ','LoyaltyIntegrationTest#rollbackCompletionAlsoRollsBackPointsAndLedger'),
 ('Observer — tiêm phòng','event/VaccinationStatusChangedListener.java','@TransactionalEventListener',7,'domain/VaccinationRecord.java','Thông báo chuyển sắp đến hạn/quá hạn sau commit','AbstractAggregateRoot đăng ký sự kiện; listener ghi log sau giao dịch','Ghi log trước commit có thể báo thay đổi đã rollback','PetHealthPersistenceTest#rollbackDoesNotLogUncommittedStatus'),
 ('Template Method — quy tắc mua','domain/purchase/TypedPurchasePolicy.java','@Override public final void validate',6,'domain/purchase/AccessoryPurchasePolicy.java','Kiểm tra chung trước kiểm tra số lượng từng loại','validate() final gọi hook validateQuantity(); trình tự không bị lớp con bỏ qua','Lặp validate cho từng loại dễ quên kiểm tra active/số lượng dương','CartPurchaseTest#hiddenProductAndNonPositiveQuantityAreRejected'),
 ('Template Method — chấm điểm','service/matching/AbstractScoreStrategy.java','@Override public List<MatchResult> match',9,'service/matching/BudgetFirstStrategy.java','Giữ quy trình lọc/chấm/xếp hạng, thay trọng số','match()/score() gọi weight(); match không final nên khung chưa bị khóa bằng ngôn ngữ','Nhân bản hai thuật toán dễ lệch lọc cứng và thứ tự hòa điểm','MatchingStrategyTest#scoreBreakdownAddsUpAndTiesUseId'),
 ('Adapter — quy tắc mua có kiểu','domain/purchase/AccessoryPurchasePolicy.java','@Override public void consume',7,'domain/purchase/TypedPurchasePolicy.java','Chuẩn hóa API mua/hoàn cho loại Product và proxy Hibernate','ProductPurchasePolicy bọc thao tác toForm/update, entity() unproxy và cast có kiểu','Service phải cast và cập nhật riêng từng loại ở nhiều nơi','CartPurchaseTest#accessoryAggregatesQuantityAndRestoresStock'),
 ('Repository — Spring Data JPA','repository/OrderRepository.java','public interface OrderRepository',2,'repository/ProductRepository.java','Tách truy vấn/persistence khỏi nghiệp vụ','Interface JpaRepository và JPQL/projection tái dùng, mock được','SQL/EntityManager rải trong controller làm khó kiểm thử và sửa truy vấn','OrderSummaryRepositoryTest#summaryContainsSnapshotMoneyLineCountAndStateActionsWithoutLoadingEntities')]
patterns += ['| Pattern | File/class cụ thể | Vấn đề giải quyết | Vì sao chọn | Nếu không dùng |', '| --- | --- | --- | --- | --- |']
for title,p,needle,count,other,problem,why,without,proof in rows:
    patterns.append(f'| {title} | {link(p,needle)}; {link(other)} | {problem} | {why} | {without} |')
for title,p,needle,count,other,problem,why,without,proof in rows:
    patterns += [f'## {title}',f'Minh họa trích từ {link(p,needle)}:',excerpt(p,needle,count), 'Test chứng minh: '+test(proof)+'.']
patterns += ['## Các lớp đi kèm và phạm vi nhận định',
             '- State Order: PendingState, ConfirmedState, ShippingState, CompletedState, CancelledState và AbstractOrderState trong `domain/order/`. Appointment: BookedState, InProgressState, DoneState, CancelledState trong `domain/appointment/`. Vaccination: ScheduledState, DueSoonState, OverdueState, CompletedState, ActiveVaccinationState trong `domain/vaccination/`. Subscription: ActiveState, ExpiredState, CancelledState trong `domain/subscription/`. Xem [UML](uml-class-diagram.md) để tránh nhầm lớp cùng tên.',
             '- Registry còn có OrderStateFactory, AppointmentStateFactory, VaccinationStateFactory, SubscriptionStates, DiscountStrategyFactory, PurchasePolicies. Chúng ánh xạ khóa sang hành vi; không phải cây Factory Method có lớp con ghi đè thao tác tạo.',
             '- Hai Strategy ghép đôi là WeightedScoreStrategy và BudgetFirstStrategy. Các Adapter mua gồm PetPurchasePolicy, AccessoryPurchasePolicy, AppointmentPurchasePolicy, PackagePurchasePolicy; chúng cũng đóng vai trò chính sách có thể thay thế qua ProductPurchasePolicy.',
             '- SubscriptionEventListener ưu tiên @Order(-10). '+test('SubscriptionIntegrationTest#purchaseDiscountCompletionPointsReplayAndSubscriptionAreAtomic')+' và '+test('SubscriptionIntegrationTest#rollbackCompletionRollsBackGrantAndPoints')+' chứng minh cấp gói/điểm/hoàn tất đơn cùng giao dịch. '+test('SubscriptionIntegrationTest#purchaseSnapshotsPackageUsesMonthsAndServicesBeforeCatalogChanges')+' kiểm tra bản chụp quyền lợi không đổi khi sửa danh mục.',
             '- Không tìm thấy Builder hay Decorator nghiệp vụ trong `src/main/java`. Constructor, static create(), DTO record và Lombok @RequiredArgsConstructor không tự trở thành Builder. Voucher cộng vào phép tính trong LoyaltyPricingService, không bọc DiscountStrategy nên không gọi là Decorator.',
             '- VaccinationScheduleTemplate là lớp final sinh lịch từ registry dữ liệu; tên “Template” không đủ để gọi GoF Template Method. Template Method thật nằm ở TypedPurchasePolicy và AbstractScoreStrategy như trên.',
             '- MutableClock là Clock tùy biến kế thừa java.time.Clock và quản lý ngày demo, không tự gán nhãn Decorator. DTO/bản chụp và Spring MVC là lựa chọn kiến trúc; không cần bịa thêm GoF pattern để đủ số lượng.']
write('patterns.md','\n\n'.join(patterns[:3])+'\n\n'+'\n'.join(patterns[3:5+len(rows)])+'\n\n'+'\n\n'.join(patterns[5+len(rows):]))

oop = ['# Checklist bốn tính chất OOP',intro,
       'Mỗi liên kết chỉ dòng trong nguồn tại thời điểm đối chiếu; phương thức ghi kèm vẫn là mốc tra cứu nếu số dòng thay đổi. Không dùng DTO setter làm ví dụ đóng gói nghiệp vụ.']
examples = [
 ('Đóng gói','domain/Cart.java','public List<CartItem> getItems','getItems(), add(), setQuantity()','items là private; trả unmodifiableList và bắt thay đổi qua kiểm tra chính sách mua.'),
 ('Đóng gói','domain/LoyaltyAccount.java','public void redeem()','redeem(), credit(), adjust()','Không có setter số dư/hạng; redeem kiểm tra voucher và đủ 100 điểm trước khi trừ.'),
 ('Đóng gói','domain/OrderItem.java','public Optional<SubscriptionBenefits> subscriptionBenefits','subscriptionBenefits(), getSnapshotServices()','Giữ quyền lợi đã chốt; tập dịch vụ chỉ đọc; cột snapshot updatable=false. Đây là bảo vệ API ứng dụng, không phải bất biến vật lý tuyệt đối của DB.'),
 ('Kế thừa','domain/Product.java','public abstract class Product','Product → Pet, Accessory, ServiceItem','Các lớp con dùng chung tên, giá gốc, version, toForm; JPA JOINED lưu đúng cây lớp.'),
 ('Kế thừa','domain/ServiceItem.java','public abstract class ServiceItem','ServiceItem → GroomingService, VetService, MonthlyCarePackage','Tầng trung gian chia sẻ durationMinutes; lớp con thêm phụ phí, bác sĩ hoặc số tháng/số lượt.'),
 ('Kế thừa','config/MutableClock.java','public final class MutableClock','MutableClock extends Clock; instant(), withZone()','Thay cách cung cấp thời gian trong profile demo, vẫn dùng hợp đồng Clock tại các service sức khỏe.'),
 ('Đa hình','domain/OrderItem.java','this.unitPrice = money(product.calculatePrice())','OrderItem constructor → Product.calculatePrice()','Biến kiểu Product gọi cách tính giá của Pet/Accessory/GroomingService/... ở runtime; GroomingService cộng phụ phí.'),
 ('Đa hình','domain/Order.java','public void confirm()','confirm() → getState().confirm(this)','Cùng lời gọi confirm nhưng PendingState cho xác nhận, các State khác từ chối qua AbstractOrderState.'),
 ('Đa hình','service/MatchingService.java','var matches = strategy.match','submit() → MatchingStrategy.match()','Cùng câu trả lời có thể chạy WeightedScoreStrategy hoặc BudgetFirstStrategy theo registry key.'),
 ('Trừu tượng','domain/Product.java','public abstract BigDecimal calculatePrice','Product.calculatePrice(), getType(), getCategory()','Hợp đồng sản phẩm không quy định một công thức giá chung cho mọi lớp cụ thể.'),
 ('Trừu tượng','service/matching/MatchingStrategy.java','public interface MatchingStrategy','match(), key(), label()','Service chỉ cần hợp đồng thuật toán, không biết công thức hay chi tiết lọc.'),
 ('Trừu tượng','domain/discount/DiscountStrategy.java','public interface DiscountStrategy','apply(), discountRate()','LoyaltyPricingService sử dụng hợp đồng giảm giá; cách lấy tỷ lệ/làm tròn được giấu sau triển khai.')]
for kind in ['Đóng gói','Kế thừa','Đa hình','Trừu tượng']:
    oop += [f'## {kind}','| Class + dòng | Phương thức/quan hệ | Bằng chứng |','| --- | --- | --- |']
    for k,p,needle,method,why in examples:
        if k==kind: oop.append(f'| {link(p,needle)} | `{method}` | {why} |')
oop += ['## Cách trình bày trước giảng viên',
        'Phân biệt kế thừa (quan hệ is-a trong khai báo lớp) với đa hình (lời gọi qua Product/State/Strategy chọn hành vi runtime). Trừu tượng nêu hợp đồng cần làm gì; đóng gói bảo vệ dữ liệu và quy tắc thay đổi. Một ví dụ có thể thể hiện nhiều tính chất nhưng phần giải thích phải chỉ rõ góc nhìn.',
        'Đối chứng test: '+', '.join(test(t) for t in ['ProductFactoryTest#calculatesExactPrice','OrderStateTest#verifiesEveryTransitionAndRejectsInvalidActions','MatchingStrategyTest#strategiesReverseRankingForSameAnswers','LoyaltyDomainTest#redeemAndNegativeAdjustmentNeverLowerLifetimePointsOrTier','SubscriptionIntegrationTest#purchaseSnapshotsPackageUsesMonthsAndServicesBeforeCatalogChanges'])+'.']
write('oop-checklist.md','\n\n'.join(oop[:3])+'\n\n'+'\n'.join(oop[3:]))

write('demo-script.md', '''# Kịch bản demo 10 phút

Đối chiếu code ngày 22/11/2026. Đây là kịch bản thao tác dự kiến từ controller, template, seed và test; chưa xác nhận trên SQL Server của người trình bày. Không dùng dữ liệu của `docs/ui/preview` làm dữ liệu ứng dụng.

## Chuẩn bị trước giờ demo

1. Dùng Java 21, SQL Server và **database demo riêng còn trống**. DemoDataRunner chỉ seed khi tất cả bảng được kiểm tra đều trống; database đã có dữ liệu sẽ không tự bổ sung tài khoản/gói/lịch. Không xóa database đang dùng để chuẩn bị demo.
2. Khai báo `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong môi trường phiên chạy; không ghi mật khẩu DB vào tài liệu hay application.properties. Chạy từ thư mục gốc:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

3. Mở `http://localhost:8080/`; vào `/login`. Chuẩn bị ba phiên trình duyệt tách cookie cho CUSTOMER, STAFF, ADMIN để chuyển nhanh. Mỗi ID trong `{id}` phải lấy bằng bấm liên kết của dữ liệu thật, không giả định ID cố định.
4. Giao diện đang dùng hướng A “Nhà của những người bạn”: xanh trầm `#285444`, nền ấm `#FAF6EE`, header phân quyền, menu trên màn hình nhỏ, thẻ và bảng theo template Thymeleaf. Bản preview tĩnh có dữ liệu khác nên không dùng trong demo nghiệp vụ.
5. Kiểm tra trước phụ kiện còn hàng, dịch vụ còn hoạt động, ngày làm việc tương lai và gói chưa bị dùng/hủy. Seed chạy lại giữ nguyên dữ liệu đã thao tác. Ngày T là ngày thực lúc seed; ngày H là ngày healthClock hiện trên trang giả lập.

## Tài khoản mẫu trong code

| Tài khoản | Mật khẩu demo | Vai trò và mục đích |
| --- | --- | --- |
| customer@petcare.vn | Customer@123 | CUSTOMER: đơn, hồ sơ Milo/Lucky, gói mẫu |
| staff@petcare.vn | Staff@123 | STAFF: xử lý đơn và lịch được giao |
| admin@petcare.vn | Admin@123 | ADMIN: giả lập ngày, dashboard, quản lý toàn bộ lịch |
| appointment.staff1@petcare.vn; appointment.staff2@petcare.vn | Staff@123 | STAFF phụ được seed cho lịch hẹn |
| loyalty.bronze@petcare.vn | Customer@123 | CUSTOMER: 490 điểm ban đầu, hạng Đồng, chưa có gói mẫu |
| loyalty.silver@petcare.vn | Customer@123 | CUSTOMER: 500 điểm, Bạc giảm 3%, có gói mẫu |
| loyalty.gold@petcare.vn | Customer@123 | CUSTOMER: 2.000 điểm, Vàng giảm 5%, có gói mẫu |
| loyalty.platinum@petcare.vn | Customer@123 | CUSTOMER: 5.000 điểm, nhãn giao diện “Kim cương”, giảm 10% |

Đây là mật khẩu công khai của seed demo, được BCrypt khi lưu; không phải thông tin đăng nhập production. Điểm/tỷ lệ trên theo application.properties mặc định; DB đã dùng có thể khác.

## Thao tác theo đúng thứ tự — tổng 10 phút

| Thời gian | Đường dẫn và tài khoản | Thao tác | Kết quả mong đợi và điểm nhấn |
| --- | --- | --- | --- |
| 00:00–01:40 — Mua hàng, State đơn | CUSTOMER `customer@petcare.vn`: `/products?group=ACCESSORY` → `/products/{id}` → `/cart` → `/checkout` → `/orders/{id}`. STAFF `staff@petcare.vn`: `/staff/orders/{id}` | Chọn **Phụ kiện 8**, số lượng 1, giá seed 800.000đ; nhập địa chỉ `12 Nguyễn Trãi, Thành phố Hồ Chí Minh`. Ghi ID đơn; nhân viên bấm Xác nhận → Giao hàng → Hoàn tất. | PENDING → CONFIRMED → SHIPPING → COMPLETED; kho giảm khi xác nhận; nút thay đổi theo State. Giá được chụp, đơn của customer mặc định chưa có giảm giá/voucher nên 800.000đ; hoàn tất nhận 80 điểm. Nhấn mạnh Factory tạo đúng lớp, đa hình giá, giao dịch và Observer. Nếu dữ liệu đã thay đổi, đọc giá/điểm thật thay vì khẳng định số mẫu. |
| 01:40–03:00 — Đặt lịch và quy tắc | CUSTOMER `loyalty.bronze@petcare.vn`: `/appointments/services` → `/appointments/slots?serviceId={id}&date={ngày}` → `/appointments/confirm?...` → `/appointments/{id}`; STAFF: `/staff/appointments?date={ngày}` | Chọn **Tắm tỉa 1**, ngày làm việc tương lai, nhân viên `staff@petcare.vn`, khung trống. Nhập tên thú cưng và loài; xác nhận. Mở lịch đúng ngày ở phiên STAFF, bấm Bắt đầu → Hoàn tất. | BOOKED → IN_PROGRESS → DONE. Không quá khứ, thứ Hai–thứ Bảy, 08:00–18:00, bước gợi ý 30 phút; trùng theo nhân viên bị từ chối, lịch sát nhau được phép. Khách chỉ hủy BOOKED trước ít nhất 2 giờ. Dùng bronze để tránh gói mẫu tự làm giá 0đ; giá seed Tắm tỉa 1 là 100.000đ, nhận 10 điểm và đạt 500 điểm/Bạc sau hoàn tất. |
| 03:00–05:00 — Hồ sơ tiêm và giả lập ngày | CUSTOMER `customer@petcare.vn`: `/my-pets` → `/my-pets/{id}` chọn **Milo bé (demo)**. ADMIN: `/admin/demo/health`; STAFF/ADMIN: `/staff/health` → `/staff/health/pets/{id}` | Xem lịch 6/8/12 tuần và mũi nhắc. Chọn một mũi còn SCHEDULED, ghi ngày hẹn D. ADMIN giả lập D−7 (nếu không trước H), sau đó D+1. STAFF xác nhận đã tiêm ở D+1. | D−7: DUE_SOON; D+1: OVERDUE; sau xác nhận: COMPLETED, không thể hoàn tất lần hai/đổi lịch mũi đã tiêm. Mũi hoàn tất mẫu giữ nguyên. Nói rõ chỉ healthClock tiến; lịch hẹn/gói không đổi ngày. MutableClock không cho lùi; nếu D−7 đã qua thì chọn mũi tương lai khác. |
| 05:00–06:00 — Quiz ghép đôi | CUSTOMER `customer@petcare.vn`: `/quiz` → `/quiz/results` → `/quiz/history` → `/quiz/history/{id}` | Nhập nhà nhỏ, có trẻ em, vận động 3/5, rảnh 2 giờ/ngày, ngân sách 3/5, không dị ứng, kinh nghiệm 2/5. Chạy “Cân bằng các tiêu chí”, rồi đổi “Ưu tiên ngân sách”. | Lọc trước khi chấm; tối đa ba kết quả kèm lý do/đóng góp điểm. Có seed Bông – chó Poodle, Mây – mèo Anh lông ngắn và các thú khác; không cam kết thứ hạng cố định vì hồ sơ/trạng thái có thể đổi. Cùng dữ liệu đầu vào, Strategy có thể thay thứ hạng; lịch sử giữ bản chụp. |
| 06:00–07:00 — Thẻ thành viên, giảm giá | CUSTOMER `loyalty.bronze@petcare.vn`: `/membership`, `/products?group=ACCESSORY`, `/cart` | Xem thẻ đã lên Bạc nhờ lịch vừa hoàn tất; đổi 100 điểm lấy voucher; thêm Phụ kiện 8 và xem giỏ. | totalPoints vẫn 500, currentPoints còn 400; hạng không giảm khi đổi điểm. Giá 800.000đ giảm hạng 24.000đ, voucher 10.000đ, còn 766.000đ nếu dữ liệu chưa đổi. Voucher dùng cho đơn kế tiếp, không cho lịch hẹn; xem giỏ chưa tiêu voucher, checkout mới chốt. |
| 07:00–09:00 — Gói chăm sóc | CUSTOMER `customer@petcare.vn`: `/subscriptions`; `/appointments/services` → xác nhận đặt lịch; `/subscriptions/{id}/renew` qua nút form. STAFF: `/staff/orders/{id}` | Xem ba gói mẫu ACTIVE/EXPIRED/CANCELLED; ghi lượt gói ACTIVE. Chọn dịch vụ nằm trong gói và ngày nằm trong hạn: trang xác nhận báo gói áp dụng/0đ; đặt lịch rồi kiểm tra giảm một lượt. Quay lại gói EXPIRED, bấm Gia hạn, nhân viên hoàn tất đơn qua ba bước. | Xem trước không trừ lượt; POST kiểm tra lại và trừ một lần. Hủy lịch hợp lệ hoàn lượt đúng một lần. Gia hạn tạo đơn mô phỏng, chỉ khi COMPLETED mới cộng quyền lợi. Gói tháng seed là 1 tháng/4 lượt; gia hạn dùng bản chụp ở OrderItem, không đọc lại tùy tiện danh mục đã sửa. Nêu giới hạn: chọn gói chung theo hạn sớm nhất; gói gắn riêng OwnedPet chưa dùng ở luồng đặt lịch. |
| 09:00–10:00 — Dashboard admin | ADMIN `admin@petcare.vn`: `/admin`; liên kết `/admin/members`, `/admin/subscriptions`, `/staff/health` | Trình bày doanh thu, sản phẩm bán chạy, phân bố hạng, cảnh báo và các liên kết quản trị. | Doanh thu tổng hợp đơn COMPLETED theo finalAmount và createdAt; dữ liệu seed có sáu tháng, không phải biểu đồ tĩnh. Số cảnh báo sức khỏe có thể đổi do bước giả lập. Menu/route theo vai trò; chốt bằng bốn tính chất OOP và ba pattern bắt buộc. |

## Dữ liệu và lưu ý để demo không bị gián đoạn

- `ProductDemoSeeder`: 6 Pet, 8 Accessory, 2 GroomingService, 2 VetService, 1 MonthlyCarePackage; MatchingDemoSeeder thêm 8 Pet và hồ sơ. OrderDemoSeeder tiêu kho Phụ kiện 1–6 trong đơn lịch sử, nên dùng **Phụ kiện 8** cho demo mua. Không giả định Phụ kiện 1 còn tồn kho.
- `PetHealthDemoSeeder`: Milo bé sinh T−6 tuần+3 ngày, Lucky lớn sinh T−5 năm, khóa `health-demo-milo`/`health-demo-lucky`; có bản ghi khám và mũi đã tiêm mẫu. Ngày được tính lúc seed, không tính lại mỗi lần khởi động.
- `SubscriptionDemoSeeder`: sourceKey `DEMO:SUBSCRIPTION:ACTIVE`, `EXPIRED`, `CANCELLED`, thêm MEMBER:silver/gold. Gói seed chọn theo ID nhỏ nhất, phủ các dịch vụ đặt được khi tập áp dụng còn rỗng.
- Phím/nút POST phải thao tác qua form để giữ CSRF. Không mở đường dẫn POST bằng thanh địa chỉ. STAFF chỉ thao tác lịch được giao; ADMIN có thể xử lý tất cả nếu chọn nhầm nhân viên.
- Không có reset demo bằng giao diện; MutableClock nằm trong bộ nhớ nhưng trạng thái tiêm đã cập nhật nằm trong DB. Khởi động lại không phục hồi dữ liệu demo ban đầu. Nếu cần trình diễn lặp, chuẩn bị database demo riêng trước buổi, không sửa dữ liệu trực tiếp lúc trình bày.

## SQL Server: các bước người dùng tự xác minh

1. Chạy bằng lệnh profile demo ở trên với biến môi trường đã cấu hình; xác nhận log kết nối SQL Server, không chạy H2/profile test để thay cho dev/demo.
2. Đăng nhập ba vai trò, kiểm tra tên `Quản trị viên`, `Phụ kiện`, `Milo bé (demo)` và địa chỉ có dấu sau lưu/tải lại. Trong SSMS kiểm tra cột chuỗi bằng truy vấn chỉ đọc:

```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dbo'
  AND DATA_TYPE IN ('varchar', 'char', 'text', 'nvarchar', 'nchar', 'ntext')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

3. Kiểm tra PK/FK JOINED qua `products`, `service_items`, các bảng con; `orders.state_name`, `appointments.state_name`, `vaccination_records.state_name`, `subscriptions.state_name` lưu chữ. Đối chiếu bảng nối và ràng buộc trong [erd.md](erd.md).
4. Khởi động lại trên cùng DB: số seed không tăng và thao tác trước không bị reset. Với DB đã có một phần dữ liệu, xác nhận runner bỏ qua seed toàn bộ.
5. Dùng hai phiên đặt cùng nhân viên/khung giờ; chỉ một lần thành công. Thử hai lần dùng lượt cuối và hủy lặp: không âm lượt/không hoàn hai lần. Xác nhận khóa và giao dịch trên SQL Server thực tế, vì test H2 không thay thế kiểm tra hệ quản trị này.
6. Thử hồ sơ/thẻ/đơn không thuộc khách: bị chặn; thử form sửa sản phẩm đã cũ: báo xung đột, không ghi đè tồn kho/trạng thái bán. Dùng bản dữ liệu demo riêng cho thử đồng thời.

Nguồn chính: `config/*DemoSeeder.java`, `config/DemoDataRunner.java`, `controller/*Controller.java`, `service/AppointmentPolicy.java`, `service/LoyaltyService.java`, `service/SubscriptionService.java`, `templates/`, `static/css/petcare.css`; test: StageNineDemoTest, PetHealthDemoTest, SubscriptionIntegrationTest, LoyaltyIntegrationTest.
''')

write('limitations.md', '''# Hạn chế hiện tại và hướng phát triển

Đối chiếu ngày 22/11/2026. Phân biệt hạn chế xác nhận từ source với điều cần thử trên hệ thống thật. Không sửa code hoặc nâng phiên bản trong công việc tài liệu này.

| Hạn chế có bằng chứng | Nguồn | Hướng phát triển |
| --- | --- | --- |
| Chưa có dời **lịch dịch vụ**: chỉ đặt/xem/hủy/bắt đầu/hoàn tất. Lịch tiêm có reschedule, nên không đánh đồng hai module. | AppointmentController, StaffAppointmentController, AppointmentService; đối chiếu VaccinationService | Thêm nghiệp vụ dời lịch giao dịch nguyên tử, kiểm tra trùng, quyền sở hữu và xử lý lượt gói cũ/mới. |
| Thanh toán và gia hạn chỉ mô phỏng; nhân viên chuyển State để hoàn tất. Không có gateway, Payment entity hay callback xác nhận tiền. | CartController, OrderService, SubscriptionService.renew; templates/commerce | Tách trạng thái thanh toán, thêm xử lý callback chống trùng, đối soát và hoàn tiền khi được phê duyệt mở rộng stack. |
| Appointment dùng tên/loài thú, chưa FK tới OwnedPet. Gói có ownedPet riêng bị loại trong appliesTo(). | Appointment, SubscriptionService.appliesTo | Chọn hồ sơ thú cưng khi đặt lịch; xác thực chủ và áp dụng gói riêng đúng hồ sơ. |
| Quyền lợi cũ thiếu snapshot dùng cấu hình gói hiện tại khi hoàn tất; không thể tái dựng quyền lợi lịch sử không từng lưu. Snapshot mới chỉ chụp tập ID dịch vụ/số lượt/tháng, không đóng băng toàn bộ thông tin dịch vụ. | OrderItem.subscriptionBenefits, SubscriptionBenefits, SubscriptionIntegrationTest.legacyMissingSnapshotFallsBackToCurrentBenefits | Lập chính sách chuyển đổi dữ liệu có kiểm chứng; phiên bản hóa quyền lợi và thông tin cần giữ lịch sử. Không tự suy diễn lịch sử cũ. |
| Gia hạn thay toàn bộ coveredServices bằng bản chụp mới, kể cả cho lượt còn lại; quá ngày hết hạn bỏ lượt cũ. Đơn mới chỉ cho một loại gói, dù nhiều đơn vị được cấp riêng. | Subscription.renew, Order.fromCart, SubscriptionService.grantOrder | Nếu nghiệp vụ cần, quản lý từng lô quyền lợi/chu kỳ và quy tắc cộng dồn minh bạch. |
| Đơn cũ có biên nhận hoặc sourceKey ORDER:id được giả định đã cấp nguyên tử; không hỗ trợ dữ liệu bị sửa tay dở dang. | Javadoc SubscriptionService.grantOrder | Công cụ đối soát/cải chính có nhật ký, tránh cấp trùng khi nâng cấp dữ liệu. |
| Một số luồng còn đọc toàn bộ: OrderRepository.findSummaries trả List, AppointmentService.mine trả List, lịch sử giao dịch điểm không phân trang; ProductService.bookableServices dùng findAll; MatchingService chấm toàn bộ ứng viên. | Các phương thức trên và LoyaltyService.view | Phân trang ở DB, projection/batch fetch thích hợp và đo query plan/load trước tối ưu. Không kết luận N+1 cho mọi danh sách: catalog, thành viên, sức khỏe, quiz history, subscriptions đã có phân trang/projection hoặc fetch riêng cùng test giới hạn truy vấn. |
| Observer điểm/cấp gói chạy đồng bộ trong tiến trình; listener tiêm chỉ ghi log sau commit, chưa gửi email/SMS. Không có hàng đợi bền vững. | LoyaltyEventListener, SubscriptionEventListener, VaccinationStatusChangedListener | Giữ giao dịch nội bộ cho điểm/quyền lợi; nếu thêm gửi thông báo ngoài hệ thống, cân nhắc outbox và retry có khóa chống trùng. |
| MutableClock demo chỉ tiến, nằm trong RAM, giữ instant đã công bố; không phải đồng hồ giả toàn hệ thống. Khởi động lại clock không rollback dữ liệu tiêm đã lưu. | MutableClock, AppointmentClockConfig, DemoDateService | Thiết kế reset/scenario trên DB demo riêng; tách rõ thời gian sức khỏe và thời gian giao dịch, kiểm soát quyền ADMIN. |
| DemoDataRunner chỉ seed khi các bảng kiểm tra đều trống; DB có dữ liệu một phần không được tự điền nốt. | DemoDataRunner.run; StageNineDemoTest.partiallyPopulatedDatabaseIsNotBackfilled | Công cụ chuẩn bị demo riêng, báo thiếu dữ liệu và kiểm chứng idempotency; không ghi đè dữ liệu đang dùng. |
| Schema dùng ddl-auto=update; H2 test ở MODE=MSSQLServer không chứng minh hành vi khóa, Unicode và schema cũ trên SQL Server. | application.properties, application-test.properties | Quy trình migration có phiên bản nếu được phép thêm công cụ; kiểm thử tích hợp SQL Server, sao lưu và kiểm tra nvarchar/constraint trên DB thực tế. |
| Lịch vaccine là dữ liệu mẫu cho chó/mèo ở 6/8/12 tuần và nhắc năm; loài khác không có lịch tự sinh. Quiz là chấm điểm quy tắc và dữ liệu hồ sơ, không mô hình học máy/đảm bảo phù hợp. | VaccinationScheduleTemplate, AbstractScoreStrategy | Cấu hình phác đồ được chuyên môn xác nhận; quản trị chất lượng hồ sơ và đánh giá thuật toán trên dữ liệu có kiểm chứng. |
| Giao diện hướng A đã tích hợp, nhưng Bootstrap tải CDN và hình sản phẩm có thể là URL ngoài; trải nghiệm ngoại tuyến phụ thuộc tài nguyên. Bản preview docs/ui không chứng minh mọi trạng thái trang thật. | templates/fragments/layout.html, ProductView, static/css/petcare.css, docs/ui | Kiểm thử responsive/keyboard/độ tương phản toàn bộ trang thật; chiến lược tài nguyên tĩnh theo quy định dự án. |
| Spring Boot cố định 3.5.16 là bản OSS cuối của dòng 3.5.x; không còn các bản vá OSS tiếp theo trong dòng này. Hỗ trợ thương mại là phạm vi riêng, không thể kết luận mọi hình thức hỗ trợ đều chấm dứt. | pom.xml và thông báo chính thức của Spring dưới đây | Giữ phiên bản theo yêu cầu môn học; trước dùng thực tế, đánh giá phương án hỗ trợ/nâng cấp được người phụ trách chấp thuận. Không tự nâng lên 4.x. |

Thông tin vòng đời phụ thuộc được kiểm tra ngoài repo: [Spring Boot 3.5.16 available now — End of OSS Support, 25/06/2026](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/). Source repo chỉ chứng minh phiên bản đang dùng, không tự chứng minh thời hạn hỗ trợ hay sự tồn tại của lỗ hổng. Không khẳng định dự án có CVE cụ thể nếu chưa quét/đối chiếu dependency thực tế.

## Những điểm cần người dùng kiểm tra

1. **SQL Server thật:** chưa có chứng cứ kết nối/đăng nhập, DDL đang triển khai, khóa đồng thời, collation và toàn bộ cột chuỗi đã là nvarchar. Làm các bước cuối [demo-script.md](demo-script.md).
2. **Dữ liệu demo đang có:** tài khoản, mật khẩu, điểm, tồn kho, trạng thái gói và ngày seed được trích từ code; không xác nhận chúng còn nguyên trong DB của người dùng. Không giả định ID hoặc thứ hạng quiz cố định.
3. **Thời lượng 10 phút và hình thức hiển thị:** là kế hoạch phân bổ, chưa bấm thử toàn kịch bản trên trình duyệt và SQL Server đang chạy; cần diễn tập, nhất là chuyển ba phiên đăng nhập và ngày giả lập không được lùi.
4. **Hiệu năng thực tế:** giới hạn nêu trên dựa vào truy vấn/kiểu trả về và source test, chưa có load test hoặc query plan trên tập dữ liệu lớn của SQL Server.
5. **Diễn giải mô hình:** ERD biểu diễn mapping JPA và các bảng collection, không phải bản reverse-engineer DB. Những khóa/cột phát sinh bởi Hibernate cần đối chiếu schema thật; không gán FK giả cho QuizMatchSnapshot.petId hay Appointment.petName.
6. **Kết quả kiểm chứng của lượt tài liệu:** xem hồ sơ trong `docs/_verification/`; phân biệt kiểm tra Mermaid/source với việc chạy ứng dụng thật. Không coi việc có tên test trong tài liệu là bằng chứng test mới chạy xanh.
''')
print('Đã sinh sáu tài liệu; 22 entity, 8 sơ đồ lớp, 13 mục pattern, 12 ví dụ OOP.')
