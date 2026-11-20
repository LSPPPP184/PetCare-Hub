
'use strict';
const products = [{"id": "harness", "name": "Đai dắt dạo phố Lá Xanh", "group": "ACCESSORY", "category": "Phụ kiện", "price": 245000, "image": "harness", "note": "Êm vai, dễ điều chỉnh", "description": "Một chiếc đai nhẹ cho những buổi dạo phố cùng người bạn nhỏ. Đệm mềm ở ngực, dây có thể điều chỉnh và khóa cài gọn gàng.", "spec": "Kích thước M · Màu xanh lá · Vải dệt mềm"}, {"id": "bowl", "name": "Bát gốm Mây ấm", "group": "ACCESSORY", "category": "Phụ kiện", "price": 185000, "image": "bowl", "note": "Góc ăn xinh xắn mỗi ngày", "description": "Bát gốm màu đất nung dành cho góc ăn của thú cưng. Thiết kế lòng sâu và chân đế vững, dễ vệ sinh sau mỗi bữa.", "spec": "Dung tích 450 ml · Gốm · Màu đất nung"}, {"id": "toy", "name": "Dây thừng vui nhộn", "group": "ACCESSORY", "category": "Phụ kiện", "price": 95000, "image": "toy", "note": "Thêm một lý do để cùng chơi", "description": "Đồ chơi dây thừng với hai đầu bóng, dành cho những giờ chơi tương tác giữa bạn và thú cưng.", "spec": "Dài 35 cm · Sợi dệt · Hai đầu bóng"}, {"id": "grooming", "name": "Tắm và chăm sóc lông", "group": "SERVICE", "category": "Dịch vụ", "price": 220000, "image": "care", "note": "60 phút chăm sóc dịu dàng", "description": "Buổi tắm và chăm sóc lông theo lịch đặt tại cửa hàng. Nhân viên trao đổi tình trạng của thú cưng trước khi bắt đầu.", "spec": "Thời lượng 60 phút · Giá mẫu cho thú cưng nhỏ"}, {"id": "pet", "name": "Bông — Corgi thân thiện", "group": "PET", "category": "Thú cưng", "price": 8500000, "image": "friends", "note": "6 tháng tuổi · Thích vận động", "description": "Bông là hồ sơ thú cưng minh họa: thích những buổi đi dạo và chơi cùng gia đình. Tìm hiểu nhu cầu chăm sóc trước khi lựa chọn.", "spec": "Chó Corgi · 6 tháng tuổi · Đực"}, {"id": "package", "name": "Gói chăm sóc An Tâm", "group": "SERVICE", "category": "Gói chăm sóc", "price": 790000, "image": "care", "note": "1 tháng · 4 lượt dịch vụ", "description": "Gói mẫu gồm bốn lượt tắm và chăm sóc lông trong một tháng. Trang này chỉ minh họa cách trình bày quyền lợi.", "spec": "Thời hạn 1 tháng · 4 lượt · Tắm và chăm sóc lông"}];
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
const params = new URLSearchParams(location.search);
const filters = document.getElementById('filters');
if (filters) {
  const cards = [...document.querySelectorAll('.product-card')];
  const grid = document.getElementById('product-grid');
  const query = document.getElementById('q');
  const group = document.getElementById('group');
  const sort = document.getElementById('sort');
  if (['PET', 'ACCESSORY', 'SERVICE'].includes(params.get('group'))) group.value = params.get('group');
  function applyFilters() {
    let count = 0;
    cards.sort((a, b) => (Number(a.dataset.price) - Number(b.dataset.price)) * (sort.value === 'desc' ? -1 : 1));
    for (const card of cards) {
      card.hidden = !(normalize(card.dataset.name).includes(normalize(query.value)) && (!group.value || card.dataset.group === group.value));
      if (!card.hidden) count++;
      grid.appendChild(card);
    }
    document.getElementById('result-count').textContent = count + ' sản phẩm mẫu';
    document.getElementById('empty').hidden = count !== 0;
  }
  filters.addEventListener('submit', event => { event.preventDefault(); applyFilters(); });
  filters.addEventListener('reset', event => { event.preventDefault(); query.value = ''; group.value = ''; sort.value = 'asc'; applyFilters(); });
  applyFilters();
}
const purchase = document.getElementById('demo-purchase');
if (purchase) {
  const product = products.find(item => item.id === params.get('item')) || products[0];
  for (const field of ['name','category','description','spec']) document.getElementById('detail-' + field).textContent = product[field];
  document.getElementById('detail-price').textContent = money(product.price);
  document.getElementById('detail-image').src = '../assets/' + product.image + '.svg';
  document.getElementById('detail-image').alt = 'Minh họa ' + product.name;
  const service = product.id === 'grooming';
  document.getElementById('quantity-field').hidden = service || product.group === 'PET';
  document.getElementById('purchase-button').textContent = service ? 'Thử chọn dịch vụ' : 'Thử thêm vào giỏ';
  purchase.addEventListener('submit', event => {
    event.preventDefault();
    const feedback = document.getElementById('purchase-feedback');
    feedback.hidden = false;
    feedback.textContent = service ? 'Đã chọn dịch vụ mẫu “' + product.name + '”. Đây là phản hồi xem trước, chưa tạo lịch hẹn.' : 'Đã thử thêm ' + (product.group === 'PET' ? '1' : document.getElementById('quantity').value) + ' × ' + product.name + '. Bản xem trước không lưu giỏ hàng hoặc tạo đơn.';
  });
}
